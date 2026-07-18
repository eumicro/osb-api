package io.osb.application.workflows;

import io.osb.domain.workflows.WorkflowDefinitionRepository;
import java.util.Objects;

public final class DeleteWorkflowUseCase {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public DeleteWorkflowUseCase(WorkflowDefinitionRepository workflowDefinitionRepository) {
        this.workflowDefinitionRepository =
                Objects.requireNonNull(workflowDefinitionRepository, "workflowDefinitionRepository");
    }

    public void execute(String id) {
        if (!workflowDefinitionRepository.delete(id)) {
            throw new IllegalArgumentException("workflow not found: " + id);
        }
    }
}
