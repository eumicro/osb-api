package io.osb.infrastructure.persistence;

import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowDefinitionRepository;
import io.osb.domain.workflows.WorkflowKind;
import io.osb.infrastructure.persistence.entity.WorkflowDefinitionEntity;
import io.osb.infrastructure.persistence.mapper.WorkflowDefinitionMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresWorkflowDefinitionRepository implements WorkflowDefinitionRepository {

    @Override
    public List<WorkflowDefinition> list() {
        return WorkflowDefinitionEntity.<WorkflowDefinitionEntity>listAll().stream()
                .map(WorkflowDefinitionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<WorkflowDefinition> findById(String id) {
        return Optional.ofNullable(WorkflowDefinitionEntity.<WorkflowDefinitionEntity>findById(id))
                .map(WorkflowDefinitionMapper::toDomain);
    }

    @Override
    public Optional<WorkflowDefinition> findEnabledByKind(WorkflowKind kind) {
        return WorkflowDefinitionEntity
                .<WorkflowDefinitionEntity>find("enabled = ?1 and kind = ?2", true, kind.name())
                .firstResultOptional()
                .map(WorkflowDefinitionMapper::toDomain);
    }

    @Override
    public Optional<WorkflowDefinition> findEnabledByKind(WorkflowKind kind, String serviceId) {
        if (serviceId != null && !serviceId.isBlank()) {
            Optional<String> mappedId = findOfferingWorkflowId(serviceId.trim(), kind.name());
            if (mappedId.isPresent()) {
                Optional<WorkflowDefinition> mapped = findById(mappedId.get()).filter(WorkflowDefinition::enabled);
                if (mapped.isPresent()) {
                    return mapped;
                }
            }
        }
        return findEnabledByKind(kind);
    }

    private static Optional<String> findOfferingWorkflowId(String serviceId, String kind) {
        @SuppressWarnings("unchecked")
        java.util.List<String> ids = WorkflowDefinitionEntity.getEntityManager()
                .createNativeQuery(
                        "SELECT workflow_id FROM offering_workflows WHERE service_id = ?1 AND kind = ?2")
                .setParameter(1, serviceId)
                .setParameter(2, kind)
                .getResultList();
        if (ids == null || ids.isEmpty() || ids.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(ids.get(0)));
    }

    @Override
    @Transactional
    public void save(WorkflowDefinition workflow) {
        WorkflowDefinitionEntity entity = WorkflowDefinitionEntity.findById(workflow.id());
        if (entity == null) {
            WorkflowDefinitionMapper.toNewEntity(workflow).persist();
            return;
        }
        WorkflowDefinitionMapper.apply(workflow, entity);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return WorkflowDefinitionEntity.deleteById(id);
    }
}
