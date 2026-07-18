package io.osb.application.workflows;

import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowDefinitionRepository;
import java.util.List;
import java.util.Objects;

public final class ListWorkflowsUseCase {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public ListWorkflowsUseCase(WorkflowDefinitionRepository workflowDefinitionRepository) {
        this.workflowDefinitionRepository =
                Objects.requireNonNull(workflowDefinitionRepository, "workflowDefinitionRepository");
    }

    public List<WorkflowDefinition> execute() {
        return workflowDefinitionRepository.list();
    }
}
