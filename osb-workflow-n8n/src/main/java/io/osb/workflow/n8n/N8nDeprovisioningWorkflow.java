package io.osb.workflow.n8n;

import io.osb.domain.workflows.WorkflowKind;
import io.osb.workflow.DeprovisioningCommand;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class N8nDeprovisioningWorkflow implements DeprovisioningWorkflow {

    private final N8nWorkflowInvoker invoker;

    public N8nDeprovisioningWorkflow(N8nWorkflowInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public String start(DeprovisioningCommand command) {
        return invoker.start(
                WorkflowKind.DEPROVISION,
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
}
