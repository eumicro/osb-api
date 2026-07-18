package io.osb.api.resource.internal;

import io.osb.api.dto.internal.WorkflowClientInvokeRequest;
import io.osb.api.dto.internal.WorkflowClientInvokeResponse;
import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.domain.templates.JsonTemplateValues;
import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateKind;
import io.osb.domain.templates.TemplateRepository;
import io.osb.domain.templates.TemplateRenderer;
import io.osb.domain.workflows.WorkflowClientType;
import io.osb.port.client.ClientCommandResult;
import io.osb.port.git.GitClientPort;
import io.osb.port.http.HttpClientNetworkPort;
import io.osb.port.kubernetes.KubernetesClientPort;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Internal API for n8n custom nodes to invoke OSB infrastructure clients.
 */
@Path("/api/internal/workflow-clients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowClientInvokeResource {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final GitClientPort gitClientPort;
    private final KubernetesClientPort kubernetesClientPort;
    private final HttpClientNetworkPort httpClientNetworkPort;
    private final HttpClientInstanceRepository httpClientInstanceRepository;
    private final KubernetesClientInstanceRepository kubernetesClientInstanceRepository;
    private final GitClientInstanceRepository gitClientInstanceRepository;
    private final TemplateRepository templateRepository;
    private final String workflowToken;

    public WorkflowClientInvokeResource(
            GitClientPort gitClientPort,
            KubernetesClientPort kubernetesClientPort,
            HttpClientNetworkPort httpClientNetworkPort,
            HttpClientInstanceRepository httpClientInstanceRepository,
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository,
            GitClientInstanceRepository gitClientInstanceRepository,
            TemplateRepository templateRepository,
            @ConfigProperty(
                            name = "osb.n8n.client-token",
                            defaultValue = "osb-n8n-client-dev-secret")
                    String workflowToken) {
        this.gitClientPort = gitClientPort;
        this.kubernetesClientPort = kubernetesClientPort;
        this.httpClientNetworkPort = httpClientNetworkPort;
        this.httpClientInstanceRepository = httpClientInstanceRepository;
        this.kubernetesClientInstanceRepository = kubernetesClientInstanceRepository;
        this.gitClientInstanceRepository = gitClientInstanceRepository;
        this.templateRepository = templateRepository;
        this.workflowToken = workflowToken;
    }

    @GET
    @Path("/HTTP/instances")
    public Response listHttpInstances(@HeaderParam("X-OSB-Workflow-Token") String token) {
        if (!authorized(token)) {
            return unauthorized();
        }
        List<Map<String, Object>> instances = httpClientInstanceRepository.list().stream()
                .filter(HttpClientInstance::enabled)
                .map(instance -> Map.<String, Object>of(
                        "id", instance.id(),
                        "name", instance.name(),
                        "baseUrl", instance.baseUrl(),
                        "authType", instance.authType().name()))
                .toList();
        return Response.ok(instances).build();
    }

    @GET
    @Path("/KUBERNETES/instances")
    public Response listKubernetesInstances(@HeaderParam("X-OSB-Workflow-Token") String token) {
        if (!authorized(token)) {
            return unauthorized();
        }
        List<Map<String, Object>> instances = kubernetesClientInstanceRepository.list().stream()
                .filter(KubernetesClientInstance::enabled)
                .map(instance -> Map.<String, Object>of(
                        "id", instance.id(),
                        "name", instance.name(),
                        "apiServerUrl", instance.apiServerUrl(),
                        "defaultNamespace", instance.defaultNamespace(),
                        "authType", instance.authType().name()))
                .toList();
        return Response.ok(instances).build();
    }

    @GET
    @Path("/GIT/instances")
    public Response listGitInstances(@HeaderParam("X-OSB-Workflow-Token") String token) {
        if (!authorized(token)) {
            return unauthorized();
        }
        List<Map<String, Object>> instances = gitClientInstanceRepository.list().stream()
                .filter(GitClientInstance::enabled)
                .map(instance -> Map.<String, Object>of(
                        "id", instance.id(),
                        "name", instance.name(),
                        "remoteUrl", instance.remoteUrl(),
                        "defaultBranch", instance.defaultBranch(),
                        "authMethod", instance.authMethod().name()))
                .toList();
        return Response.ok(instances).build();
    }

    @POST
    @Path("/{clientType}")
    public Response invoke(
            @PathParam("clientType") String clientType,
            @HeaderParam("X-OSB-Workflow-Token") String token,
            WorkflowClientInvokeRequest request) {
        if (!authorized(token)) {
            return unauthorized();
        }

        WorkflowClientType type;
        try {
            type = WorkflowClientType.valueOf(clientType.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "unknown client type: " + clientType))
                    .build();
        }

        String action = request == null || request.action() == null ? "status" : request.action();
        Map<String, Object> payload =
                request == null || request.payload() == null
                        ? Map.of()
                        : new LinkedHashMap<>(request.payload());
        try {
            applySelectedTemplate(type, payload);
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
        String payloadJson = toJsonObject(payload);

        ClientCommandResult result =
                switch (type) {
                    case GIT -> gitClientPort.invoke(action, payloadJson);
                    case KUBERNETES -> kubernetesClientPort.invoke(action, payloadJson);
                    case HTTP -> httpClientNetworkPort.invoke(action, payloadJson);
                };

        WorkflowClientInvokeResponse body = new WorkflowClientInvokeResponse(
                result.ok(),
                result.client(),
                result.action(),
                result.message(),
                result.detailsJson());
        return result.ok()
                ? Response.ok(body).build()
                : Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }

    /**
     * When n8n nodes pass {@code templateId}, resolve + render the template into the
     * client-specific payload field ({@code manifest}, HTTP body fields, or {@code commandLine}).
     */
    private void applySelectedTemplate(WorkflowClientType type, Map<String, Object> payload) {
        Object templateIdValue = payload.get("templateId");
        if (templateIdValue == null) {
            return;
        }
        String templateId = String.valueOf(templateIdValue).trim();
        if (templateId.isEmpty()) {
            return;
        }
        Template template = templateRepository
                .findById(templateId)
                .filter(Template::enabled)
                .orElseThrow(() -> new IllegalArgumentException(
                        "template not found or disabled: " + templateId));
        String rendered = TemplateRenderer.render(
                template.content(), WorkflowTemplatesResource.flattenValues(payload));
        switch (type) {
            case KUBERNETES -> {
                if (template.kind() != TemplateKind.KUBERNETES_RESOURCE
                        && template.kind() != TemplateKind.TEXT) {
                    throw new IllegalArgumentException(
                            "template " + templateId + " kind " + template.kind()
                                    + " is not usable for Kubernetes");
                }
                payload.putIfAbsent("manifest", rendered);
            }
            case HTTP -> {
                if (template.kind() != TemplateKind.HTTP_REQUEST
                        && template.kind() != TemplateKind.TEXT) {
                    throw new IllegalArgumentException(
                            "template " + templateId + " kind " + template.kind()
                                    + " is not usable for HTTP");
                }
                applyHttpTemplateContent(payload, rendered);
            }
            case GIT -> {
                if (template.kind() != TemplateKind.GIT_COMMAND
                        && template.kind() != TemplateKind.TEXT) {
                    throw new IllegalArgumentException(
                            "template " + templateId + " kind " + template.kind()
                                    + " is not usable for Git");
                }
                applyGitTemplateContent(payload, rendered);
            }
        }
        payload.put("templateKind", template.kind().name());
        payload.put("renderedTemplateId", template.id());
    }

    /**
     * Git templates are JSON documents:
     * {@code {"path":"instances/Test-${instanceId}.txt","content":"...","message":"..."}}.
     */
    private static void applyGitTemplateContent(Map<String, Object> payload, String rendered) {
        payload.put("templateContent", rendered);
        String trimmed = rendered == null ? "" : rendered.trim();
        if (!trimmed.startsWith("{")) {
            payload.putIfAbsent("content", rendered);
            payload.putIfAbsent("commandLine", rendered);
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(trimmed, Map.class);
            if (doc.get("path") != null) {
                payload.putIfAbsent("path", String.valueOf(doc.get("path")));
            }
            if (doc.get("content") != null) {
                payload.putIfAbsent("content", String.valueOf(doc.get("content")));
            }
            if (doc.get("message") != null) {
                payload.putIfAbsent("message", String.valueOf(doc.get("message")));
            }
            if (doc.get("ref") != null) {
                payload.putIfAbsent("ref", String.valueOf(doc.get("ref")));
            }
        } catch (Exception ex) {
            payload.putIfAbsent("content", rendered);
            payload.putIfAbsent("commandLine", rendered);
        }
    }

    /**
     * HTTP templates are JSON documents:
     * {@code {"method":"GET","path":"/get","query":{...},"body":{...}}}.
     */
    private static void applyHttpTemplateContent(Map<String, Object> payload, String rendered) {
        payload.put("templateContent", rendered);
        String trimmed = rendered == null ? "" : rendered.trim();
        if (!trimmed.startsWith("{")) {
            payload.putIfAbsent("body", rendered);
            payload.putIfAbsent("method", "GET");
            payload.putIfAbsent("path", "/");
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(trimmed, Map.class);
            if (doc.get("method") != null) {
                payload.putIfAbsent("method", String.valueOf(doc.get("method")));
            } else {
                payload.putIfAbsent("method", "GET");
            }
            String path = doc.get("path") != null ? String.valueOf(doc.get("path")) : "/";
            Object query = doc.get("query");
            if (query instanceof Map<?, ?> queryMap && !queryMap.isEmpty()) {
                StringBuilder qs = new StringBuilder();
                for (Map.Entry<?, ?> entry : queryMap.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    if (!qs.isEmpty()) {
                        qs.append('&');
                    }
                    qs.append(entry.getKey())
                            .append('=')
                            .append(entry.getValue() == null ? "" : entry.getValue());
                }
                path = path.contains("?") ? path + "&" + qs : path + "?" + qs;
            }
            payload.putIfAbsent("path", path);
            if (doc.get("body") != null) {
                // Quoted placeholders like "temporary":"${parameters.x}" stay valid JSON in
                // the template editor; coerce "true"/"false" strings for known boolean keys.
                payload.putIfAbsent("body", JsonTemplateValues.coerceBooleans(doc.get("body")));
            } else {
                payload.putIfAbsent("body", Map.of());
            }
        } catch (Exception ex) {
            payload.putIfAbsent("body", rendered);
            payload.putIfAbsent("method", "GET");
            payload.putIfAbsent("path", "/");
        }
    }

    private static String toJsonObject(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return JSON.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize workflow client payload", ex);
        }
    }

    private boolean authorized(String token) {
        return token != null && token.equals(workflowToken);
    }

    private static Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "invalid workflow token"))
                .build();
    }
}
