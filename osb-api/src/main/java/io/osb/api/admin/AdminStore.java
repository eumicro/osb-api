package io.osb.api.admin;

import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.workflow.DeprovisioningCommand;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.ProvisioningCommand;
import io.osb.workflow.ProvisioningWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory admin store for service instances (replace with domain later). */
@ApplicationScoped
public class AdminStore {

    private final ProvisioningWorkflow provisioningWorkflow;
    private final DeprovisioningWorkflow deprovisioningWorkflow;
    private final List<ServiceInstanceDto> instances = new CopyOnWriteArrayList<>();

    public AdminStore(
            ProvisioningWorkflow provisioningWorkflow,
            DeprovisioningWorkflow deprovisioningWorkflow) {
        this.provisioningWorkflow = provisioningWorkflow;
        this.deprovisioningWorkflow = deprovisioningWorkflow;
        // No dummy instance: provision via Admin UI against git-file-store / redis-cache / keycloak-realm.
    }

    public List<ServiceInstanceDto> listInstances() {
        List<ServiceInstanceDto> synced = new ArrayList<>(instances.size());
        for (ServiceInstanceDto instance : List.copyOf(instances)) {
            ServiceInstanceDto next = syncOperation(instance);
            if (isFullyDeprovisioned(next)) {
                // Drop only after deprovision last-operation succeeded — not while deleting.
                instances.removeIf(i -> i.id().equals(next.id()));
                continue;
            }
            synced.add(next);
        }
        return List.copyOf(synced);
    }

    public Optional<ServiceInstanceDto> findById(String id) {
        Optional<ServiceInstanceDto> found = instances.stream()
                .filter(i -> i.id().equals(id))
                .findFirst()
                .map(this::syncOperation);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ServiceInstanceDto synced = found.get();
        if (isFullyDeprovisioned(synced)) {
            // Return the completed snapshot once, then purge so the UI can leave the list.
            instances.removeIf(i -> i.id().equals(id));
        }
        return Optional.of(synced);
    }

    public ServiceInstanceDto provision(
            String serviceId,
            String planId,
            String platformClientId,
            Map<String, Object> parameters) {
        String instanceId = "inst-" + UUID.randomUUID().toString().substring(0, 8);
        ServiceInstanceDto created = new ServiceInstanceDto(
                instanceId,
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
        instances.add(created);

        String operationId;
        try {
            operationId = provisioningWorkflow.start(new ProvisioningCommand(
                    instanceId, serviceId, planId, copyParameters(parameters)));
        } catch (RuntimeException ex) {
            ServiceInstanceDto failed = withOperation(
                    created,
                    null,
                    "failed",
                    "failed",
                    "Provision workflow failed to start: " + ex.getMessage(),
                    "PROVISION");
            replace(failed);
            return failed;
        }

        ServiceInstanceDto withOp = withOperation(
                created,
                operationId,
                "in progress",
                "in progress",
                "Provision workflow started",
                "PROVISION");
        replace(withOp);
        return syncOperation(withOp);
    }

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
        replace(updated);
        return updated;
    }

    public Optional<ServiceInstanceDto> deprovision(String id) {
        // Do not use findById here — it may purge a fully deprovisioned instance.
        Optional<ServiceInstanceDto> existing = instances.stream()
                .filter(i -> i.id().equals(id))
                .findFirst()
                .map(this::syncOperation);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        ServiceInstanceDto current = existing.get();
        if (isFullyDeprovisioned(current)) {
            instances.removeIf(i -> i.id().equals(id));
            return Optional.of(current);
        }
        if ("deleting".equalsIgnoreCase(current.state())
                && "DEPROVISION".equals(current.lastOperationKind())
                && "in progress".equalsIgnoreCase(current.lastOperationState())) {
            // Already deprovisioning — return current status instead of starting again.
            return Optional.of(current);
        }

        ServiceInstanceDto deleting = withOperation(
                current,
                null,
                "deleting",
                "in progress",
                "Starting deprovision workflow",
                "DEPROVISION");
        replace(deleting);

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
            replace(failed);
            return Optional.of(failed);
        }

        ServiceInstanceDto withOp = withOperation(
                deleting,
                operationId,
                "deleting",
                "in progress",
                "Deprovision workflow started",
                "DEPROVISION");
        replace(withOp);
        // Keep the instance visible until last-operation reports success (sync → deprovisioned).
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

        WorkflowStatus status;
        try {
            status = "DEPROVISION".equals(instance.lastOperationKind())
                    ? deprovisioningWorkflow.status(operationId)
                    : provisioningWorkflow.status(operationId);
        } catch (RuntimeException ex) {
            return instance;
        }

        return switch (status) {
            case IN_PROGRESS -> instance;
            case SUCCEEDED -> {
                if ("DEPROVISION".equals(instance.lastOperationKind())) {
                    ServiceInstanceDto done = withOperation(
                            instance,
                            operationId,
                            "deprovisioned",
                            "succeeded",
                            "Deprovision completed",
                            "DEPROVISION");
                    replace(done);
                    yield done;
                }
                ServiceInstanceDto done = withOperation(
                        instance,
                        operationId,
                        "succeeded",
                        "succeeded",
                        "Provision completed",
                        "PROVISION");
                // dashboardUrl comes from the provision workflow webhook response — not hardcoded here.
                done = withDashboardUrl(done, provisioningWorkflow.dashboardUrl(operationId));
                replace(done);
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
                replace(failed);
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

    private void replace(ServiceInstanceDto updated) {
        for (int i = 0; i < instances.size(); i++) {
            if (instances.get(i).id().equals(updated.id())) {
                instances.set(i, updated);
                return;
            }
        }
        instances.add(updated);
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
