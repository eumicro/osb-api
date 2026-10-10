package io.osb.api.admin;

import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.workflow.DeprovisioningCommand;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.ProvisioningCommand;
import io.osb.workflow.ProvisioningWorkflow;
import io.osb.workflow.UpdatingCommand;
import io.osb.workflow.UpdatingWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Admin store for service instances (Postgres via {@link ServiceInstanceRecordStore}). */
@ApplicationScoped
public class AdminStore {

    private final ProvisioningWorkflow provisioningWorkflow;
    private final DeprovisioningWorkflow deprovisioningWorkflow;
    private final UpdatingWorkflow updatingWorkflow;
    private final ServiceInstanceRecordStore records;

    public AdminStore(
            ProvisioningWorkflow provisioningWorkflow,
            DeprovisioningWorkflow deprovisioningWorkflow,
            UpdatingWorkflow updatingWorkflow,
            ServiceInstanceRecordStore records) {
        this.provisioningWorkflow = provisioningWorkflow;
        this.deprovisioningWorkflow = deprovisioningWorkflow;
        this.updatingWorkflow = updatingWorkflow;
        this.records = records;
    }

    @Transactional
    public List<ServiceInstanceDto> listInstances() {
        List<ServiceInstanceDto> snapshot = records.listAll();
        List<ServiceInstanceDto> synced = new ArrayList<>(snapshot.size());
        for (ServiceInstanceDto instance : snapshot) {
            ServiceInstanceDto next = syncOperation(instance);
            if (isFullyDeprovisioned(next)) {
                records.delete(next.id());
                continue;
            }
            synced.add(next);
        }
        return List.copyOf(synced);
    }

    @Transactional
    public Optional<ServiceInstanceDto> findById(String id) {
        Optional<ServiceInstanceDto> found = records.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ServiceInstanceDto synced = syncOperation(found.get());
        if (isFullyDeprovisioned(synced)) {
            records.delete(id);
        }
        return Optional.of(synced);
    }

    public ServiceInstanceDto provision(
            String serviceId,
            String planId,
            String platformClientId,
            Map<String, Object> parameters) {
        return provision(null, serviceId, planId, platformClientId, parameters);
    }

    /**
     * @param instanceId OSB-provided instance id; when null/blank a random admin id is generated
     */
    @Transactional
    public ServiceInstanceDto provision(
            String instanceId,
            String serviceId,
            String planId,
            String platformClientId,
            Map<String, Object> parameters) {
        String id = (instanceId == null || instanceId.isBlank())
                ? "inst-" + UUID.randomUUID().toString().substring(0, 8)
                : instanceId.trim();
        ServiceInstanceDto created = new ServiceInstanceDto(
                id,
                serviceId,
                planId,
                "in progress",
                platformClientId,
                null,
                copyParameters(parameters),
                null,
                "in progress",
                "Starting provision workflow",
                "PROVISION");
        records.save(created);

        String operationId;
        try {
            operationId = provisioningWorkflow.start(new ProvisioningCommand(
                    id, serviceId, planId, copyParameters(parameters)));
        } catch (RuntimeException ex) {
            ServiceInstanceDto failed = withOperation(
                    created,
                    null,
                    "failed",
                    "failed",
                    "Provision workflow failed to start: " + ex.getMessage(),
                    "PROVISION");
            records.save(failed);
            return failed;
        }

        ServiceInstanceDto withOp = withOperation(
                created,
                operationId,
                "in progress",
                "in progress",
                "Provision workflow started",
                "PROVISION");
        records.save(withOp);
        return syncOperation(withOp);
    }

    @Transactional
    public ServiceInstanceDto update(String id, String platformClientId, String dashboardUrl) {
        ServiceInstanceDto existing = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("instance not found: " + id));
        ServiceInstanceDto updated = new ServiceInstanceDto(
                existing.id(),
                existing.serviceId(),
                existing.planId(),
                existing.state(),
                platformClientId,
                dashboardUrl,
                existing.parameters(),
                existing.lastOperationId(),
                existing.lastOperationState(),
                existing.lastOperationDescription(),
                existing.lastOperationKind());
        records.save(updated);
        return updated;
    }

    /**
     * OSB {@code PATCH /v2/service_instances/{id}} — starts an async UPDATE workflow and applies
     * the new plan/parameters once the operation succeeds (polled via last_operation).
     */
    @Transactional
    public ServiceInstanceDto requestUpdate(
            String instanceId,
            String serviceId,
            String planId,
            Map<String, Object> parameters) {
        ServiceInstanceDto existing = findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("instance not found: " + instanceId));
        if (isPending(existing.state())) {
            throw new IllegalStateException("instance has an operation in progress");
        }

        Map<String, Object> previousParameters = copyParameters(existing.parameters());
        Map<String, Object> nextParameters = copyParameters(parameters);
        ServiceInstanceDto updating = new ServiceInstanceDto(
                existing.id(),
                serviceId,
                planId,
                "in progress",
                existing.platformClientId(),
                existing.dashboardUrl(),
                nextParameters,
                null,
                "in progress",
                "Starting update workflow",
                "UPDATE");
        records.save(updating);

        String operationId;
        try {
            operationId = updatingWorkflow.start(new UpdatingCommand(
                    existing.id(),
                    serviceId,
                    planId,
                    nextParameters,
                    previousParameters));
        } catch (RuntimeException ex) {
            ServiceInstanceDto failed = withOperation(
                    updating,
                    null,
                    "failed",
                    "failed",
                    "Update workflow failed to start: " + ex.getMessage(),
                    "UPDATE");
            records.save(failed);
            return failed;
        }

        ServiceInstanceDto withOp = withOperation(
                updating,
                operationId,
                "in progress",
                "in progress",
                "Update workflow started",
                "UPDATE");
        records.save(withOp);
        return syncOperation(withOp);
    }

    @Transactional
    public Optional<ServiceInstanceDto> deprovision(String id) {
        Optional<ServiceInstanceDto> found = records.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ServiceInstanceDto current = syncOperation(found.get());
        if (isFullyDeprovisioned(current)) {
            records.delete(id);
            return Optional.of(current);
        }
        if ("deleting".equalsIgnoreCase(current.state())
                && "DEPROVISION".equals(current.lastOperationKind())
                && "in progress".equalsIgnoreCase(current.lastOperationState())) {
            return Optional.of(current);
        }

        ServiceInstanceDto deleting = withOperation(
                current,
                null,
                "deleting",
                "in progress",
                "Starting deprovision workflow",
                "DEPROVISION");
        records.save(deleting);

        String operationId;
        try {
            operationId = deprovisioningWorkflow.start(new DeprovisioningCommand(
                    current.id(),
                    current.serviceId(),
                    current.planId(),
                    copyParameters(current.parameters())));
        } catch (RuntimeException ex) {
            ServiceInstanceDto failed = withOperation(
                    deleting,
                    null,
                    "failed",
                    "failed",
                    "Deprovision workflow failed to start: " + ex.getMessage(),
                    "DEPROVISION");
            records.save(failed);
            return Optional.of(failed);
        }

        ServiceInstanceDto withOp = withOperation(
                deleting,
                operationId,
                "deleting",
                "in progress",
                "Deprovision workflow started",
                "DEPROVISION");
        records.save(withOp);
        return Optional.of(syncOperation(withOp));
    }

    private ServiceInstanceDto syncOperation(ServiceInstanceDto instance) {
        String operationId = instance.lastOperationId();
        if (operationId == null || operationId.isBlank()) {
            return instance;
        }
        if (!isPending(instance.state()) && !"in progress".equalsIgnoreCase(instance.lastOperationState())) {
            return instance;
        }

        String kind = instance.lastOperationKind() == null ? "" : instance.lastOperationKind();
        WorkflowStatus status;
        try {
            status = switch (kind) {
                case "DEPROVISION" -> deprovisioningWorkflow.status(operationId);
                case "UPDATE" -> updatingWorkflow.status(operationId);
                default -> provisioningWorkflow.status(operationId);
            };
        } catch (RuntimeException ex) {
            return instance;
        }

        return switch (status) {
            case IN_PROGRESS -> instance;
            case SUCCEEDED -> {
                if ("DEPROVISION".equals(kind)) {
                    ServiceInstanceDto done = withOperation(
                            instance,
                            operationId,
                            "deprovisioned",
                            "succeeded",
                            "Deprovision completed",
                            "DEPROVISION");
                    records.save(done);
                    yield done;
                }
                String completedKind = "UPDATE".equals(kind) ? "UPDATE" : "PROVISION";
                String description = "UPDATE".equals(kind) ? "Update completed" : "Provision completed";
                ServiceInstanceDto done = withOperation(
                        instance,
                        operationId,
                        "succeeded",
                        "succeeded",
                        description,
                        completedKind);
                if ("PROVISION".equals(completedKind)) {
                    done = withDashboardUrl(done, provisioningWorkflow.dashboardUrl(operationId));
                }
                records.save(done);
                yield done;
            }
            case FAILED -> {
                ServiceInstanceDto failed = withOperation(
                        instance,
                        operationId,
                        "failed",
                        "failed",
                        "Workflow failed",
                        instance.lastOperationKind());
                records.save(failed);
                yield failed;
            }
        };
    }

    private static ServiceInstanceDto withDashboardUrl(ServiceInstanceDto base, String dashboardUrl) {
        if (dashboardUrl == null || dashboardUrl.isBlank()) {
            return base;
        }
        return new ServiceInstanceDto(
                base.id(),
                base.serviceId(),
                base.planId(),
                base.state(),
                base.platformClientId(),
                dashboardUrl,
                base.parameters(),
                base.lastOperationId(),
                base.lastOperationState(),
                base.lastOperationDescription(),
                base.lastOperationKind());
    }

    private static boolean isPending(String state) {
        if (state == null) {
            return false;
        }
        String normalized = state.trim().toLowerCase();
        return "in progress".equals(normalized) || "deleting".equals(normalized);
    }

    /** True when deprovision workflow finished successfully — safe to drop from the admin UI. */
    private static boolean isFullyDeprovisioned(ServiceInstanceDto instance) {
        if (instance == null) {
            return false;
        }
        if ("deprovisioned".equalsIgnoreCase(instance.state())) {
            return true;
        }
        return "DEPROVISION".equals(instance.lastOperationKind())
                && "succeeded".equalsIgnoreCase(instance.lastOperationState())
                && !"failed".equalsIgnoreCase(instance.state());
    }

    private static ServiceInstanceDto withOperation(
            ServiceInstanceDto base,
            String operationId,
            String state,
            String lastOperationState,
            String description,
            String kind) {
        return new ServiceInstanceDto(
                base.id(),
                base.serviceId(),
                base.planId(),
                state,
                base.platformClientId(),
                base.dashboardUrl(),
                base.parameters(),
                operationId,
                lastOperationState,
                description,
                kind);
    }

    private static Map<String, Object> copyParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(parameters));
    }
}
