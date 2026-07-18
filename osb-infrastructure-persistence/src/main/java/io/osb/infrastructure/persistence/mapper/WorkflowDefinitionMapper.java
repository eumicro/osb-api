package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.workflows.WorkflowClientType;
import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowKind;
import io.osb.infrastructure.persistence.entity.WorkflowDefinitionEntity;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class WorkflowDefinitionMapper {

    private WorkflowDefinitionMapper() {}

    public static WorkflowDefinition toDomain(WorkflowDefinitionEntity entity) {
        Set<WorkflowClientType> clients = entity.clientTypes.stream()
                .map(WorkflowClientType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(WorkflowClientType.class)));
        return new WorkflowDefinition(
                entity.id,
                entity.name,
                entity.description,
                WorkflowKind.valueOf(entity.kind),
                entity.n8nWebhookPath,
                entity.n8nWorkflowId,
                entity.enabled,
                clients,
                new LinkedHashSet<>(entity.httpClientIds),
                new LinkedHashSet<>(entity.kubernetesClientIds),
                new LinkedHashSet<>(entity.gitClientIds),
                new LinkedHashSet<>(entity.templateIds));
    }

    public static WorkflowDefinitionEntity toNewEntity(WorkflowDefinition workflow) {
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.id = workflow.id();
        apply(workflow, entity);
        return entity;
    }

    public static void apply(WorkflowDefinition workflow, WorkflowDefinitionEntity entity) {
        entity.name = workflow.name();
        entity.description = workflow.description();
        entity.kind = workflow.kind().name();
        entity.n8nWebhookPath = workflow.n8nWebhookPath();
        entity.n8nWorkflowId = workflow.n8nWorkflowId();
        entity.enabled = workflow.enabled();
        entity.clientTypes.clear();
        entity.clientTypes.addAll(
                workflow.clients().stream().map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new)));
        entity.httpClientIds.clear();
        entity.httpClientIds.addAll(workflow.httpClientIds());
        entity.kubernetesClientIds.clear();
        entity.kubernetesClientIds.addAll(workflow.kubernetesClientIds());
        entity.gitClientIds.clear();
        entity.gitClientIds.addAll(workflow.gitClientIds());
        entity.templateIds.clear();
        entity.templateIds.addAll(workflow.templateIds());
    }
}
