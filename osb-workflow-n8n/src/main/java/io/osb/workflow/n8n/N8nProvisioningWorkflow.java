package io.osb.workflow.n8n;

import io.osb.domain.workflows.WorkflowKind;
import io.osb.workflow.ProvisioningCommand;
import io.osb.workflow.ProvisioningWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class N8nProvisioningWorkflow implements ProvisioningWorkflow {

    private final N8nWorkflowInvoker invoker;

    public N8nProvisioningWorkflow(N8nWorkflowInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public String start(ProvisioningCommand command) {
        return invoker.start(
                WorkflowKind.PROVISION,
                command.serviceId(),
                CommandJson.of(
                        command.instanceId(),
                        command.serviceId(),
                        command.planId(),
                        command.parameters()));
    }

    @Override
    public WorkflowStatus status(String operationId) {
        return invoker.status(operationId);
    }

    @Override
    public String dashboardUrl(String operationId) {
        return invoker.dashboardUrl(operationId);
    }
}
