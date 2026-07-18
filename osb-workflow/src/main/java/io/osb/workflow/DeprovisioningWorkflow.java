package io.osb.workflow;

/**
 * Workflow port for deprovisioning orchestration.
 */
public interface DeprovisioningWorkflow {

    String start(DeprovisioningCommand command);

    WorkflowStatus status(String operationId);
}
