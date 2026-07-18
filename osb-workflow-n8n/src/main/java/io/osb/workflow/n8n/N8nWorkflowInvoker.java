package io.osb.workflow.n8n;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateRepository;
import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowDefinitionRepository;
import io.osb.domain.workflows.WorkflowKind;
import io.osb.port.http.HttpClientNetworkPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class N8nWorkflowInvoker {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final TemplateRepository templateRepository;
    private final HttpClientNetworkPort httpClientNetworkPort;
    private final N8nSettings n8nSettings;
    private final N8nOperationStore operationStore;

    public N8nWorkflowInvoker(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            TemplateRepository templateRepository,
            HttpClientNetworkPort httpClientNetworkPort,
            N8nSettings n8nSettings,
            N8nOperationStore operationStore) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.templateRepository = templateRepository;
        this.httpClientNetworkPort = httpClientNetworkPort;
        this.n8nSettings = n8nSettings;
        this.operationStore = operationStore;
    }

    public String start(WorkflowKind kind, String commandJsonObject) {
        return start(kind, null, commandJsonObject);
    }

    public String start(WorkflowKind kind, String serviceId, String commandJsonObject) {
        WorkflowDefinition definition = workflowDefinitionRepository
                .findEnabledByKind(kind, serviceId)
                .orElseThrow(() -> new IllegalStateException(
                        "no enabled workflow definition for kind " + kind
                                + (serviceId == null || serviceId.isBlank()
                                        ? ""
                                        : " / service " + serviceId)));

        String operationId = "n8n-" + kind.name().toLowerCase() + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        String clientsJson = definition.clients().stream()
                .map(client -> "\"" + client.name() + "\"")
                .collect(Collectors.joining(","));
        String httpClientIdsJson = definition.httpClientIds().stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(","));
        String kubernetesClientIdsJson = definition.kubernetesClientIds().stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(","));
        String gitClientIdsJson = definition.gitClientIds().stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(","));
        String templateIdsJson = definition.templateIds().stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(","));
        String templatesJson = resolveTemplatesJson(definition);
        String payload = "{"
                + "\"operationId\":\"" + operationId + "\","
                + "\"workflowId\":\"" + definition.id() + "\","
                + "\"kind\":\"" + kind.name() + "\","
                + "\"clients\":[" + clientsJson + "],"
                + "\"httpClientIds\":[" + httpClientIdsJson + "],"
                + "\"kubernetesClientIds\":[" + kubernetesClientIdsJson + "],"
                + "\"gitClientIds\":[" + gitClientIdsJson + "],"
                + "\"templateIds\":[" + templateIdsJson + "],"
                + "\"templates\":" + templatesJson + ","
                + "\"command\":" + (commandJsonObject == null ? "{}" : commandJsonObject)
                + "}";

        if (!n8nSettings.invokeWebhooks()) {
            operationStore.put(operationId, WorkflowStatus.IN_PROGRESS);
            return operationId;
        }

        String base = n8nSettings.baseUrl().endsWith("/")
                ? n8nSettings.baseUrl().substring(0, n8nSettings.baseUrl().length() - 1)
                : n8nSettings.baseUrl();
        String url = base + definition.n8nWebhookPath();
        try {
            // Seed webhooks use responseMode=responseNode → POST waits until workflow finishes.
            String responseBody = httpClientNetworkPort.postJson(url, payload);
            if (isExplicitFailure(responseBody)) {
                operationStore.put(operationId, WorkflowStatus.FAILED);
            } else {
                operationStore.put(
                        operationId, WorkflowStatus.SUCCEEDED, readDashboardUrl(responseBody));
            }
        } catch (RuntimeException ex) {
            operationStore.put(operationId, WorkflowStatus.FAILED);
        }
        return operationId;
    }

    public WorkflowStatus status(String operationId) {
        return operationStore.get(operationId);
    }

    public String dashboardUrl(String operationId) {
        return operationStore.dashboardUrl(operationId);
    }

    /** Workflow may return {@code {"ok":false,...}} with HTTP 200. */
    static boolean isExplicitFailure(String responseBody) {
        JsonNode root = parseJson(responseBody);
        if (root == null || !root.has("ok") || root.get("ok").isNull()) {
            return false;
        }
        return root.get("ok").isBoolean() && !root.get("ok").booleanValue();
    }

    static String readDashboardUrl(String responseBody) {
        JsonNode root = parseJson(responseBody);
        if (root == null) {
            return null;
        }
        JsonNode node = root.get("dashboardUrl");
        if (node == null || node.isNull()) {
            node = root.get("dashboard_url");
        }
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    private static JsonNode parseJson(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return JSON.readTree(responseBody);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveTemplatesJson(WorkflowDefinition definition) {
        List<String> items = new ArrayList<>();
        for (String templateId : definition.templateIds()) {
            templateRepository.findById(templateId).ifPresent(template -> {
                if (template.enabled()) {
                    items.add(toTemplateJson(template));
                }
            });
        }
        return "[" + String.join(",", items) + "]";
    }

    private static String toTemplateJson(Template template) {
        return "{"
                + "\"id\":\"" + escape(template.id()) + "\","
                + "\"name\":\"" + escape(template.name()) + "\","
                + "\"kind\":\"" + template.kind().name() + "\","
                + "\"content\":\"" + escape(template.content()) + "\""
                + "}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
