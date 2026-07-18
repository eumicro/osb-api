package io.osb.domain.workflows;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Admin-managed workflow binding to an n8n webhook/editor workflow and optional client adapters.
 */
public final class WorkflowDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final WorkflowKind kind;
    private final String n8nWebhookPath;
    private final String n8nWorkflowId;
    private final boolean enabled;
    private final Set<WorkflowClientType> clients;
    private final Set<String> httpClientIds;
    private final Set<String> kubernetesClientIds;
    private final Set<String> gitClientIds;
    private final Set<String> templateIds;

    public WorkflowDefinition(
            String id,
            String name,
            String description,
            WorkflowKind kind,
            String n8nWebhookPath,
            String n8nWorkflowId,
            boolean enabled,
            Set<WorkflowClientType> clients,
            Set<String> httpClientIds,
            Set<String> kubernetesClientIds,
            Set<String> gitClientIds,
            Set<String> templateIds) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.n8nWebhookPath = requireText(n8nWebhookPath, "n8nWebhookPath");
        if (!this.n8nWebhookPath.startsWith("/")) {
            throw new IllegalArgumentException("n8nWebhookPath must start with /");
        }
        this.n8nWorkflowId = Objects.requireNonNullElse(n8nWorkflowId, "").trim();
        this.enabled = enabled;
        this.clients = clients == null || clients.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(clients));
        this.httpClientIds = normalizeIds(httpClientIds);
        this.kubernetesClientIds = normalizeIds(kubernetesClientIds);
        this.gitClientIds = normalizeIds(gitClientIds);
        this.templateIds = normalizeIds(templateIds);
        if (!this.httpClientIds.isEmpty() && !this.clients.contains(WorkflowClientType.HTTP)) {
            throw new IllegalArgumentException("httpClientIds require clients to include HTTP");
        }
        if (!this.kubernetesClientIds.isEmpty()
                && !this.clients.contains(WorkflowClientType.KUBERNETES)) {
            throw new IllegalArgumentException(
                    "kubernetesClientIds require clients to include KUBERNETES");
        }
        if (!this.gitClientIds.isEmpty() && !this.clients.contains(WorkflowClientType.GIT)) {
            throw new IllegalArgumentException("gitClientIds require clients to include GIT");
        }
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public WorkflowKind kind() {
        return kind;
    }

    public String n8nWebhookPath() {
        return n8nWebhookPath;
    }

    /** n8n editor workflow id for deep-link {@code /workflow/{id}}; may be blank. */
    public String n8nWorkflowId() {
        return n8nWorkflowId;
    }

    public boolean enabled() {
        return enabled;
    }

    public Set<WorkflowClientType> clients() {
        return clients;
    }

    public Set<String> httpClientIds() {
        return httpClientIds;
    }

    public Set<String> kubernetesClientIds() {
        return kubernetesClientIds;
    }

    public Set<String> gitClientIds() {
        return gitClientIds;
    }

    /** Independent text templates (placeholders) available to this workflow. */
    public Set<String> templateIds() {
        return templateIds;
    }

    public WorkflowDefinition withDetails(
            String name,
            String description,
            WorkflowKind kind,
            String n8nWebhookPath,
            String n8nWorkflowId,
            boolean enabled,
            Set<WorkflowClientType> clients,
            Set<String> httpClientIds,
            Set<String> kubernetesClientIds,
            Set<String> gitClientIds,
            Set<String> templateIds) {
        return new WorkflowDefinition(
                id,
                name,
                description,
                kind,
                n8nWebhookPath,
                n8nWorkflowId,
                enabled,
                clients,
                httpClientIds,
                kubernetesClientIds,
                gitClientIds,
                templateIds);
    }

    private static Set<String> normalizeIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalized.add(id.trim());
            }
        }
        return Set.copyOf(normalized);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
