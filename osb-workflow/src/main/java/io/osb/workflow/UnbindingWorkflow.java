package io.osb.workflow;

public interface UnbindingWorkflow {

    String start(UnbindingCommand command);

    WorkflowStatus status(String operationId);
}
