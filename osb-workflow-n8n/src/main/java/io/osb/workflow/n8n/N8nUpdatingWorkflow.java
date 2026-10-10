package io.osb.workflow.n8n;

import io.osb.domain.workflows.WorkflowKind;
import io.osb.workflow.UpdatingCommand;
import io.osb.workflow.UpdatingWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class N8nUpdatingWorkflow implements UpdatingWorkflow {

    private final N8nWorkflowInvoker invoker;

    public N8nUpdatingWorkflow(N8nWorkflowInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public String start(UpdatingCommand command) {
        return invoker.start(
                WorkflowKind.UPDATE,
                command.serviceId(),
                CommandJson.ofUpdate(
                        command.instanceId(),
                        command.serviceId(),
                        command.planId(),
                        command.parameters(),
                        command.previousParameters()));
    }

    @Override
    public WorkflowStatus status(String operationId) {
        return invoker.status(operationId);
    }
}
