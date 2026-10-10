package io.osb.workflow;

/** Workflow port for service-instance update (OSB PATCH) orchestration. */
public interface UpdatingWorkflow {

    String start(UpdatingCommand command);

    WorkflowStatus status(String operationId);
}
