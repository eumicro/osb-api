package io.osb.api.admin;

import io.osb.api.dto.osb.ServiceBindingDto;
import io.osb.workflow.BindingCommand;
import io.osb.workflow.BindingWorkflow;
import io.osb.workflow.UnbindingCommand;
import io.osb.workflow.UnbindingWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class BindingStore {

    private final BindingWorkflow bindingWorkflow;
    private final UnbindingWorkflow unbindingWorkflow;
    private final ServiceBindingRecordStore records;

    public BindingStore(
            BindingWorkflow bindingWorkflow,
            UnbindingWorkflow unbindingWorkflow,
            ServiceBindingRecordStore records) {
        this.bindingWorkflow = bindingWorkflow;
        this.unbindingWorkflow = unbindingWorkflow;
        this.records = records;
    }

    @Transactional
    public Optional<ServiceBindingDto> find(String instanceId, String bindingId) {
        Optional<ServiceBindingDto> found = records.find(instanceId, bindingId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ServiceBindingDto synced = syncOperation(found.get());
        if (isFullyUnbound(synced)) {
            records.delete(instanceId, bindingId);
            return Optional.of(synced);
        }
        return Optional.of(synced);
    }

    @Transactional
    public ServiceBindingDto bind(
            String instanceId,
            String bindingId,
            String serviceId,
            String planId,
            String platformClientId,
            Map<String, Object> parameters) {
        Optional<ServiceBindingDto> existing = records.find(instanceId, bindingId);
        if (existing.isPresent()) {
            ServiceBindingDto current = syncOperation(existing.get());
            if (isFullyUnbound(current)) {
                records.delete(instanceId, bindingId);
            } else {
                return current;
            }
        }

        ServiceBindingDto created = new ServiceBindingDto(
                bindingId,
                instanceId,
                serviceId,
                planId,
                platformClientId,
                "in progress",
                Map.of(),
                copy(parameters),
                null,
                "in progress",
                "Starting bind workflow",
                "BIND");
        records.save(created);

        String operationId;
        try {
            operationId = bindingWorkflow.start(new BindingCommand(
                    instanceId, bindingId, serviceId, planId, copy(parameters)));
        } catch (RuntimeException ex) {
            ServiceBindingDto failed = withOperation(
                    created,
                    null,
                    "failed",
                    "failed",
                    "Bind workflow failed to start: " + ex.getMessage(),
                    "BIND");
            records.save(failed);
            return failed;
        }

        ServiceBindingDto withOp = withOperation(
                created,
                operationId,
                "in progress",
                "in progress",
                "Bind workflow started",
                "BIND");
        records.save(withOp);
        return syncOperation(withOp);
    }

    @Transactional
    public Optional<ServiceBindingDto> unbind(String instanceId, String bindingId) {
        Optional<ServiceBindingDto> found = records.find(instanceId, bindingId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ServiceBindingDto current = syncOperation(found.get());
        if (isFullyUnbound(current)) {
            records.delete(instanceId, bindingId);
            return Optional.of(current);
        }
        if ("deleting".equalsIgnoreCase(current.state())
                && "UNBIND".equals(current.lastOperationKind())
                && "in progress".equalsIgnoreCase(current.lastOperationState())) {
            return Optional.of(current);
        }

        ServiceBindingDto deleting = withOperation(
                current,
                null,
                "deleting",
                "in progress",
                "Starting unbind workflow",
                "UNBIND");
        records.save(deleting);

        String operationId;
        try {
            operationId = unbindingWorkflow.start(new UnbindingCommand(
                    current.instanceId(),
                    current.id(),
                    current.serviceId(),
                    current.planId(),
                    copy(current.parameters())));
        } catch (RuntimeException ex) {
            ServiceBindingDto failed = withOperation(
                    deleting,
                    null,
                    "failed",
                    "failed",
                    "Unbind workflow failed to start: " + ex.getMessage(),
                    "UNBIND");
            records.save(failed);
            return Optional.of(failed);
        }

        ServiceBindingDto withOp = withOperation(
                deleting,
                operationId,
                "deleting",
                "in progress",
                "Unbind workflow started",
                "UNBIND");
        records.save(withOp);
        return Optional.of(syncOperation(withOp));
    }

    private ServiceBindingDto syncOperation(ServiceBindingDto binding) {
        String operationId = binding.lastOperationId();
        if (operationId == null || operationId.isBlank()) {
            return binding;
        }
        if (!isPending(binding.state())
                && !"in progress".equalsIgnoreCase(binding.lastOperationState())) {
            return binding;
        }

        String kind = binding.lastOperationKind() == null ? "" : binding.lastOperationKind();
        WorkflowStatus status;
        try {
            status = "UNBIND".equals(kind)
                    ? unbindingWorkflow.status(operationId)
                    : bindingWorkflow.status(operationId);
        } catch (RuntimeException ex) {
            return binding;
        }

        return switch (status) {
            case IN_PROGRESS -> binding;
            case SUCCEEDED -> {
                if ("UNBIND".equals(kind)) {
                    ServiceBindingDto done = withOperation(
                            binding,
                            operationId,
                            "unbound",
                            "succeeded",
                            "Unbind completed",
                            "UNBIND");
                    records.save(done);
                    yield done;
                }
                Map<String, Object> credentials = bindingWorkflow.credentials(operationId);
                ServiceBindingDto done = new ServiceBindingDto(
                        binding.id(),
                        binding.instanceId(),
                        binding.serviceId(),
                        binding.planId(),
                        binding.platformClientId(),
                        "succeeded",
                        copy(credentials),
                        binding.parameters(),
                        operationId,
                        "succeeded",
                        "Bind completed",
                        "BIND");
                records.save(done);
                yield done;
            }
            case FAILED -> {
                ServiceBindingDto failed = withOperation(
                        binding,
                        operationId,
                        "failed",
                        "failed",
                        "Workflow failed",
                        binding.lastOperationKind());
                records.save(failed);
                yield failed;
            }
        };
    }

    private static boolean isPending(String state) {
        if (state == null) {
            return false;
        }
        String normalized = state.trim().toLowerCase();
        return "in progress".equals(normalized) || "deleting".equals(normalized);
    }

    private static boolean isFullyUnbound(ServiceBindingDto binding) {
        if (binding == null) {
            return false;
        }
        if ("unbound".equalsIgnoreCase(binding.state())) {
            return true;
        }
        return "UNBIND".equals(binding.lastOperationKind())
                && "succeeded".equalsIgnoreCase(binding.lastOperationState())
                && !"failed".equalsIgnoreCase(binding.state());
    }

    private static ServiceBindingDto withOperation(
            ServiceBindingDto base,
            String operationId,
            String state,
            String lastOperationState,
            String description,
            String kind) {
        return new ServiceBindingDto(
                base.id(),
                base.instanceId(),
                base.serviceId(),
                base.planId(),
                base.platformClientId(),
                state,
                base.credentials(),
                base.parameters(),
                operationId,
                lastOperationState,
                description,
                kind);
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(value));
    }
}
