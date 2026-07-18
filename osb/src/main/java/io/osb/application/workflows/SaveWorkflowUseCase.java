package io.osb.application.workflows;

import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.domain.templates.TemplateRepository;
import io.osb.domain.workflows.WorkflowClientType;
import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowDefinitionRepository;
import io.osb.domain.workflows.WorkflowKind;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SaveWorkflowUseCase {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final HttpClientInstanceRepository httpClientInstanceRepository;
    private final KubernetesClientInstanceRepository kubernetesClientInstanceRepository;
    private final GitClientInstanceRepository gitClientInstanceRepository;
    private final TemplateRepository templateRepository;

    public SaveWorkflowUseCase(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            HttpClientInstanceRepository httpClientInstanceRepository,
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository,
            GitClientInstanceRepository gitClientInstanceRepository,
            TemplateRepository templateRepository) {
        this.workflowDefinitionRepository =
                Objects.requireNonNull(workflowDefinitionRepository, "workflowDefinitionRepository");
        this.httpClientInstanceRepository =
                Objects.requireNonNull(httpClientInstanceRepository, "httpClientInstanceRepository");
        this.kubernetesClientInstanceRepository = Objects.requireNonNull(
                kubernetesClientInstanceRepository, "kubernetesClientInstanceRepository");
        this.gitClientInstanceRepository =
                Objects.requireNonNull(gitClientInstanceRepository, "gitClientInstanceRepository");
        this.templateRepository = Objects.requireNonNull(templateRepository, "templateRepository");
    }

    public WorkflowDefinition create(
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
        Set<WorkflowClientType> resolvedClients =
                resolveClients(clients, httpClientIds, kubernetesClientIds, gitClientIds);
        validateHttpClients(httpClientIds);
        validateKubernetesClients(kubernetesClientIds);
        validateGitClients(gitClientIds);
        validateTemplates(templateIds);
        WorkflowDefinition created = new WorkflowDefinition(
                "wf-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                description == null ? "" : description,
                kind,
                n8nWebhookPath,
                n8nWorkflowId == null ? "" : n8nWorkflowId,
                enabled,
                resolvedClients,
                httpClientIds,
                kubernetesClientIds,
                gitClientIds,
                templateIds);
        workflowDefinitionRepository.save(created);
        return created;
    }

    public WorkflowDefinition update(
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
        WorkflowDefinition existing = workflowDefinitionRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("workflow not found: " + id));
        Set<WorkflowClientType> resolvedClients =
                resolveClients(clients, httpClientIds, kubernetesClientIds, gitClientIds);
        validateHttpClients(httpClientIds);
        validateKubernetesClients(kubernetesClientIds);
        validateGitClients(gitClientIds);
        validateTemplates(templateIds);
        WorkflowDefinition updated = existing.withDetails(
                name,
                description == null ? "" : description,
                kind,
                n8nWebhookPath,
                n8nWorkflowId == null ? "" : n8nWorkflowId,
                enabled,
                resolvedClients,
                httpClientIds,
                kubernetesClientIds,
                gitClientIds,
                templateIds);
        workflowDefinitionRepository.save(updated);
        return updated;
    }

    private void validateHttpClients(Set<String> httpClientIds) {
        if (httpClientIds == null) {
            return;
        }
        for (String clientId : httpClientIds) {
            if (clientId == null || clientId.isBlank()) {
                continue;
            }
            httpClientInstanceRepository
                    .findById(clientId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "http client not found: " + clientId));
        }
    }

    private void validateKubernetesClients(Set<String> kubernetesClientIds) {
        if (kubernetesClientIds == null) {
            return;
        }
        for (String clientId : kubernetesClientIds) {
            if (clientId == null || clientId.isBlank()) {
                continue;
            }
            kubernetesClientInstanceRepository
                    .findById(clientId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "kubernetes client not found: " + clientId));
        }
    }

    private void validateGitClients(Set<String> gitClientIds) {
        if (gitClientIds == null) {
            return;
        }
        for (String clientId : gitClientIds) {
            if (clientId == null || clientId.isBlank()) {
                continue;
            }
            gitClientInstanceRepository
                    .findById(clientId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "git client not found: " + clientId));
        }
    }

    private void validateTemplates(Set<String> templateIds) {
        if (templateIds == null) {
            return;
        }
        for (String templateId : templateIds) {
            if (templateId == null || templateId.isBlank()) {
                continue;
            }
            templateRepository
                    .findById(templateId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "template not found: " + templateId));
        }
    }

    private static Set<WorkflowClientType> resolveClients(
            Set<WorkflowClientType> clients,
            Set<String> httpClientIds,
            Set<String> kubernetesClientIds,
            Set<String> gitClientIds) {
        EnumSet<WorkflowClientType> resolved = clients == null || clients.isEmpty()
                ? EnumSet.noneOf(WorkflowClientType.class)
                : EnumSet.copyOf(clients);
        if (httpClientIds != null && !httpClientIds.isEmpty()) {
            resolved.add(WorkflowClientType.HTTP);
        }
        if (kubernetesClientIds != null && !kubernetesClientIds.isEmpty()) {
            resolved.add(WorkflowClientType.KUBERNETES);
        }
        if (gitClientIds != null && !gitClientIds.isEmpty()) {
            resolved.add(WorkflowClientType.GIT);
        }
        return resolved;
    }
}
