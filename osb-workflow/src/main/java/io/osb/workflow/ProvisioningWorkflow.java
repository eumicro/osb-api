package io.osb.workflow;

/**
 * Workflow port for provisioning orchestration.
 */
public interface ProvisioningWorkflow {

    String start(ProvisioningCommand command);

    WorkflowStatus status(String operationId);

    /**
     * Optional dashboard URL returned by the provision workflow response
     * ({@code dashboardUrl} / {@code dashboard_url}).
     */
    default String dashboardUrl(String operationId) {
        return null;
    }
}
