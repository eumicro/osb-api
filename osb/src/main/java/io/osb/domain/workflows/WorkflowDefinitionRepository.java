package io.osb.domain.workflows;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository {

    List<WorkflowDefinition> list();

    Optional<WorkflowDefinition> findById(String id);

    Optional<WorkflowDefinition> findEnabledByKind(WorkflowKind kind);

    /**
     * Prefer a workflow mapped to {@code serviceId} via {@code offering_workflows}; otherwise
     * fall back to {@link #findEnabledByKind(WorkflowKind)}.
     */
    default Optional<WorkflowDefinition> findEnabledByKind(WorkflowKind kind, String serviceId) {
        return findEnabledByKind(kind);
    }

    void save(WorkflowDefinition workflow);

    boolean delete(String id);
}
