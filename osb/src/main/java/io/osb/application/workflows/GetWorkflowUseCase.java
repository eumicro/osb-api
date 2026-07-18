package io.osb.application.workflows;

import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowDefinitionRepository;
import java.util.Objects;

public final class GetWorkflowUseCase {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public GetWorkflowUseCase(WorkflowDefinitionRepository workflowDefinitionRepository) {
        this.workflowDefinitionRepository =
                Objects.requireNonNull(workflowDefinitionRepository, "workflowDefinitionRepository");
    }

    public WorkflowDefinition execute(String id) {
        return workflowDefinitionRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("workflow not found: " + id));
    }
}
