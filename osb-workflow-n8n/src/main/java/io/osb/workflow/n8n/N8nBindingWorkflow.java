package io.osb.workflow.n8n;

import io.osb.domain.workflows.WorkflowKind;
import io.osb.workflow.BindingCommand;
import io.osb.workflow.BindingWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class N8nBindingWorkflow implements BindingWorkflow {

    private final N8nWorkflowInvoker invoker;

    public N8nBindingWorkflow(N8nWorkflowInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public String start(BindingCommand command) {
        return invoker.start(
                WorkflowKind.BIND,
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

    @Override
    public Map<String, Object> credentials(String operationId) {
        return Map.of();
    }
}
