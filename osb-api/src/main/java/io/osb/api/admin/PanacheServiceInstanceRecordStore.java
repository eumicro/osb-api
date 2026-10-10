package io.osb.api.admin;

import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.infrastructure.persistence.entity.ServiceInstanceEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PanacheServiceInstanceRecordStore implements ServiceInstanceRecordStore {

    @Override
    @Transactional
    public List<ServiceInstanceDto> listAll() {
        List<ServiceInstanceDto> out = new ArrayList<>();
        for (ServiceInstanceEntity entity : ServiceInstanceEntity.<ServiceInstanceEntity>listAll()) {
            out.add(toDto(entity));
        }
        return List.copyOf(out);
    }

    @Override
    @Transactional
    public Optional<ServiceInstanceDto> findById(String id) {
        ServiceInstanceEntity entity = ServiceInstanceEntity.findById(id);
        return entity == null ? Optional.empty() : Optional.of(toDto(entity));
    }

    @Override
    @Transactional
    public void save(ServiceInstanceDto dto) {
        ServiceInstanceEntity entity = ServiceInstanceEntity.findById(dto.id());
        boolean create = entity == null;
        if (create) {
            entity = new ServiceInstanceEntity();
            entity.id = dto.id();
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
        if (create) {
            entity.persist();
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        ServiceInstanceEntity.deleteById(id);
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

    private static Map<String, Object> copyParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(parameters));
    }
}
