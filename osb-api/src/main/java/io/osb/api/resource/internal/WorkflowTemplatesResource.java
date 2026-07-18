package io.osb.api.resource.internal;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateKind;
import io.osb.domain.templates.TemplateRepository;
import io.osb.domain.templates.TemplateRenderer;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Internal API for n8n nodes: list/render OSB text templates.
 */
@Path("/api/internal/templates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowTemplatesResource {

    private final TemplateRepository templateRepository;
    private final String workflowToken;

    public WorkflowTemplatesResource(
            TemplateRepository templateRepository,
            @ConfigProperty(
                            name = "osb.n8n.client-token",
                            defaultValue = "osb-n8n-client-dev-secret")
                    String workflowToken) {
        this.templateRepository = templateRepository;
        this.workflowToken = workflowToken;
    }

    @GET
    public Response list(
            @HeaderParam("X-OSB-Workflow-Token") String token,
            @QueryParam("kind") String kind) {
        if (!authorized(token)) {
            return unauthorized();
        }
        TemplateKind filter = parseKind(kind);
        List<Map<String, Object>> templates = templateRepository.list().stream()
                .filter(Template::enabled)
                .filter(template -> filter == null || template.kind() == filter)
                .map(template -> Map.<String, Object>of(
                        "id", template.id(),
                        "name", template.name(),
                        "description", template.description(),
                        "kind", template.kind().name()))
                .toList();
        return Response.ok(templates).build();
    }

    @GET
    @Path("/{id}")
    public Response get(
            @PathParam("id") String id, @HeaderParam("X-OSB-Workflow-Token") String token) {
        if (!authorized(token)) {
            return unauthorized();
        }
        return templateRepository
                .findById(id)
                .filter(Template::enabled)
                .map(template -> Response.ok(toDetail(template)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "template not found: " + id))
                        .build());
    }

    @POST
    @Path("/{id}/render")
    public Response render(
            @PathParam("id") String id,
            @HeaderParam("X-OSB-Workflow-Token") String token,
            Map<String, Object> values) {
        if (!authorized(token)) {
            return unauthorized();
        }
        return templateRepository
                .findById(id)
                .filter(Template::enabled)
                .map(template -> {
                    Map<String, Object> renderValues = flattenValues(values);
                    String content = TemplateRenderer.render(template.content(), renderValues);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("id", template.id());
                    body.put("name", template.name());
                    body.put("kind", template.kind().name());
                    body.put("content", content);
                    return Response.ok(body).build();
                })
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "template not found: " + id))
                        .build());
    }

    static Map<String, Object> flattenValues(Map<String, Object> values) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (values == null) {
            return out;
        }
        out.putAll(values);
        Object command = values.get("command");
        if (command instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    out.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        return out;
    }

    private static Map<String, Object> toDetail(Template template) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", template.id());
        body.put("name", template.name());
        body.put("description", template.description());
        body.put("kind", template.kind().name());
        body.put("content", template.content());
        body.put("enabled", template.enabled());
        return body;
    }

    private static TemplateKind parseKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return null;
        }
        try {
            return TemplateKind.valueOf(kind.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return null;
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
