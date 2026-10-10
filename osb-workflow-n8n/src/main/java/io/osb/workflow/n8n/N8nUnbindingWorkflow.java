package io.osb.workflow.n8n;

import io.osb.domain.workflows.WorkflowKind;
import io.osb.workflow.UnbindingCommand;
import io.osb.workflow.UnbindingWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class N8nUnbindingWorkflow implements UnbindingWorkflow {

    private final N8nWorkflowInvoker invoker;

    public N8nUnbindingWorkflow(N8nWorkflowInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public String start(UnbindingCommand command) {
        return invoker.start(
                WorkflowKind.UNBIND,
                command.serviceId(),
                CommandJson.ofBinding(
                        command.instanceId(),
                        command.bindingId(),
                        command.serviceId(),
                        command.planId(),
                        command.parameters()));
    }

    @Override
    public WorkflowStatus status(String operationId) {
        return invoker.status(operationId);
    }
}
