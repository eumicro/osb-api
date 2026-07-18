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
import java.util.Locale;
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

        if (!n8nSettings.invokeWebhooks()) {
            operationStore.put(operationId, WorkflowStatus.IN_PROGRESS);
            return operationId;
        }

        try {
            String responseBody = postWebhook(definition, kind, operationId, commandJsonObject);
            if (isExplicitFailure(responseBody)) {
                operationStore.put(operationId, WorkflowStatus.FAILED);
            } else {
                // Status comes from the workflow response (`state`), not from Java heuristics.
                applyWorkflowReportedStatus(
                        kind, operationId, serviceId, commandJsonObject, responseBody);
            }
        } catch (RuntimeException ex) {
            operationStore.put(operationId, WorkflowStatus.FAILED);
        }
        return operationId;
    }

    public WorkflowStatus status(String operationId) {
        N8nOperationStore.Entry entry = operationStore.getEntry(operationId);
        if (entry == null) {
            return WorkflowStatus.FAILED;
        }
        if (entry.status() != WorkflowStatus.IN_PROGRESS || !entry.pollLastOperation()) {
            return entry.status();
        }
        return refreshViaLastOperation(operationId, entry);
    }

    public String dashboardUrl(String operationId) {
        return operationStore.dashboardUrl(operationId);
    }

    private WorkflowStatus refreshViaLastOperation(
            String operationId, N8nOperationStore.Entry entry) {
        try {
            WorkflowDefinition lastOp = workflowDefinitionRepository
                    .findEnabledByKind(WorkflowKind.INSTANCE_LAST_OPERATION, entry.serviceId())
                    .orElse(null);
            if (lastOp == null) {
                return WorkflowStatus.IN_PROGRESS;
            }
            String responseBody = postWebhook(
                    lastOp,
                    WorkflowKind.INSTANCE_LAST_OPERATION,
                    operationId,
                    entry.commandJson());
            if (isExplicitFailure(responseBody)) {
                operationStore.put(operationId, WorkflowStatus.FAILED, entry.dashboardUrl());
                return WorkflowStatus.FAILED;
            }
            WorkflowStatus next = readOperationState(responseBody);
            if (next == WorkflowStatus.IN_PROGRESS) {
                return WorkflowStatus.IN_PROGRESS;
            }
            operationStore.put(operationId, next, entry.dashboardUrl());
            return next;
        } catch (RuntimeException ex) {
            // Transient poll errors must not flip the UI to failed while pods are still starting.
            return WorkflowStatus.IN_PROGRESS;
        }
    }

    private void applyWorkflowReportedStatus(
            WorkflowKind kind,
            String operationId,
            String serviceId,
            String commandJsonObject,
            String responseBody) {
        String dashboardUrl = readDashboardUrl(responseBody);
        WorkflowStatus reported = readOperationState(responseBody);
        if (reported == WorkflowStatus.FAILED) {
            operationStore.put(operationId, WorkflowStatus.FAILED, dashboardUrl);
            return;
        }
        if (reported == WorkflowStatus.IN_PROGRESS && kind == WorkflowKind.PROVISION) {
            // Provision workflow declared async completion via last-operation.
            operationStore.putPendingProvision(
                    operationId, dashboardUrl, serviceId, commandJsonObject);
            return;
        }
        operationStore.put(operationId, reported, dashboardUrl);
    }

    private String postWebhook(
            WorkflowDefinition definition,
            WorkflowKind kind,
            String operationId,
            String commandJsonObject) {
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

        String base = n8nSettings.baseUrl().endsWith("/")
                ? n8nSettings.baseUrl().substring(0, n8nSettings.baseUrl().length() - 1)
                : n8nSettings.baseUrl();
        // Seed webhooks use responseMode=responseNode → POST waits until workflow finishes.
        return httpClientNetworkPort.postJson(base + definition.n8nWebhookPath(), payload);
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

    /**
     * Maps workflow webhook JSON {@code state} to a {@link WorkflowStatus}. Missing {@code state}
     * means the workflow finished synchronously ({@link WorkflowStatus#SUCCEEDED}).
     */
    static WorkflowStatus readOperationState(String responseBody) {
        JsonNode root = parseJson(responseBody);
        if (root == null) {
            return WorkflowStatus.SUCCEEDED;
        }
        JsonNode stateNode = root.get("state");
        if (stateNode == null || stateNode.isNull() || !stateNode.isTextual()) {
            return WorkflowStatus.SUCCEEDED;
        }
        return mapState(stateNode.asText());
    }

    private static WorkflowStatus mapState(String raw) {
        if (raw == null || raw.isBlank()) {
            return WorkflowStatus.IN_PROGRESS;
        }
        String state = raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return switch (state) {
            case "failed", "failure", "error" -> WorkflowStatus.FAILED;
            case "succeeded", "success", "done" -> WorkflowStatus.SUCCEEDED;
            case "in progress", "progressing" -> WorkflowStatus.IN_PROGRESS;
            default -> WorkflowStatus.IN_PROGRESS;
        };
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
