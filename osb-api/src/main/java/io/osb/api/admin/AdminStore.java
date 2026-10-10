package io.osb.api.admin;

import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.infrastructure.persistence.entity.ServiceInstanceEntity;
import io.osb.workflow.DeprovisioningCommand;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.ProvisioningCommand;
import io.osb.workflow.ProvisioningWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Postgres-backed admin store for service instances. */
@ApplicationScoped
public class AdminStore {

    private final ProvisioningWorkflow provisioningWorkflow;
    private final DeprovisioningWorkflow deprovisioningWorkflow;

    public AdminStore(
            ProvisioningWorkflow provisioningWorkflow,
            DeprovisioningWorkflow deprovisioningWorkflow) {
        this.provisioningWorkflow = provisioningWorkflow;
        this.deprovisioningWorkflow = deprovisioningWorkflow;
    }

    @Transactional
    public List<ServiceInstanceDto> listInstances() {
        List<ServiceInstanceDto> snapshot = ServiceInstanceEntity.<ServiceInstanceEntity>listAll().stream()
                .map(AdminStore::toDto)
                .toList();
        List<ServiceInstanceDto> synced = new ArrayList<>(snapshot.size());
        for (ServiceInstanceDto instance : snapshot) {
            ServiceInstanceDto next = syncOperation(instance);
            if (isFullyDeprovisioned(next)) {
                ServiceInstanceEntity.deleteById(next.id());
                continue;
            }
            synced.add(next);
        }
        return List.copyOf(synced);
    }

    @Transactional
    public Optional<ServiceInstanceDto> findById(String id) {
        ServiceInstanceEntity entity = ServiceInstanceEntity.findById(id);
        if (entity == null) {
            return Optional.empty();
        }
        ServiceInstanceDto synced = syncOperation(toDto(entity));
        if (isFullyDeprovisioned(synced)) {
            ServiceInstanceEntity.deleteById(id);
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
        persist(created);

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
            persist(failed);
            return failed;
        }

        ServiceInstanceDto withOp = withOperation(
                created,
                operationId,
                "in progress",
                "in progress",
                "Provision workflow started",
                "PROVISION");
        persist(withOp);
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
        persist(updated);
        return updated;
    }

    @Transactional
    public Optional<ServiceInstanceDto> deprovision(String id) {
        ServiceInstanceEntity entity = ServiceInstanceEntity.findById(id);
        if (entity == null) {
            return Optional.empty();
        }
        ServiceInstanceDto current = syncOperation(toDto(entity));
        if (isFullyDeprovisioned(current)) {
            ServiceInstanceEntity.deleteById(id);
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
        persist(deleting);

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
            persist(failed);
            return Optional.of(failed);
        }

        ServiceInstanceDto withOp = withOperation(
                deleting,
                operationId,
                "deleting",
                "in progress",
                "Deprovision workflow started",
                "DEPROVISION");
        persist(withOp);
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
                    persist(done);
                    yield done;
                }
                ServiceInstanceDto done = withOperation(
                        instance,
                        operationId,
                        "succeeded",
                        "succeeded",
                        "Provision completed",
                        "PROVISION");
                done = withDashboardUrl(done, provisioningWorkflow.dashboardUrl(operationId));
                persist(done);
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
                persist(failed);
                yield failed;
            }
        };
    }

    private void persist(ServiceInstanceDto dto) {
        ServiceInstanceEntity entity = ServiceInstanceEntity.findById(dto.id());
        if (entity == null) {
            entity = new ServiceInstanceEntity();
            entity.id = dto.id();
            entity.persist();
        }
        entity.serviceId = dto.serviceId();
        entity.planId = dto.planId();
        entity.state = dto.state();
        entity.platformClientId = dto.platformClientId();
        entity.dashboardUrl = dto.dashboardUrl();
        entity.parameters = new LinkedHashMap<>(copyParameters(dto.parameters()));
        entity.lastOperationId = dto.lastOperationId();
        entity.lastOperationState = dto.lastOperationState();
        entity.lastOperationDescription = dto.lastOperationDescription();
        entity.lastOperationKind = dto.lastOperationKind();
    }

    private static ServiceInstanceDto toDto(ServiceInstanceEntity entity) {
        return new ServiceInstanceDto(
                entity.id,
                entity.serviceId,
                entity.planId,
                entity.state,
                entity.platformClientId,
                entity.dashboardUrl,
                copyParameters(entity.parameters),
                entity.lastOperationId,
                entity.lastOperationState,
                entity.lastOperationDescription,
                entity.lastOperationKind);
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
