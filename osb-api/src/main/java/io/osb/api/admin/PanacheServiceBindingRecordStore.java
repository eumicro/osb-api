package io.osb.api.admin;

import io.osb.api.dto.osb.ServiceBindingDto;
import io.osb.infrastructure.persistence.entity.ServiceBindingEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PanacheServiceBindingRecordStore implements ServiceBindingRecordStore {

    @Override
    @Transactional
    public List<ServiceBindingDto> listByInstance(String instanceId) {
        List<ServiceBindingDto> out = new ArrayList<>();
        for (ServiceBindingEntity entity :
                ServiceBindingEntity.<ServiceBindingEntity>list("instanceId", instanceId)) {
            out.add(toDto(entity));
        }
        return List.copyOf(out);
    }

    @Override
    @Transactional
    public Optional<ServiceBindingDto> find(String instanceId, String bindingId) {
        ServiceBindingEntity entity = ServiceBindingEntity.findById(
                new ServiceBindingEntity.Pk(bindingId, instanceId));
        return entity == null ? Optional.empty() : Optional.of(toDto(entity));
    }

    @Override
    @Transactional
    public void save(ServiceBindingDto dto) {
        ServiceBindingEntity entity = ServiceBindingEntity.findById(
                new ServiceBindingEntity.Pk(dto.id(), dto.instanceId()));
        if (entity == null) {
            entity = new ServiceBindingEntity();
            entity.id = dto.id();
            entity.instanceId = dto.instanceId();
            entity.persist();
        }
        entity.serviceId = dto.serviceId();
        entity.planId = dto.planId();
        entity.platformClientId = dto.platformClientId();
        entity.state = dto.state();
        entity.credentials = new LinkedHashMap<>(copy(dto.credentials()));
        entity.parameters = new LinkedHashMap<>(copy(dto.parameters()));
        entity.lastOperationId = dto.lastOperationId();
        entity.lastOperationState = dto.lastOperationState();
        entity.lastOperationDescription = dto.lastOperationDescription();
        entity.lastOperationKind = dto.lastOperationKind();
    }

    @Override
    @Transactional
    public void delete(String instanceId, String bindingId) {
        ServiceBindingEntity.deleteById(new ServiceBindingEntity.Pk(bindingId, instanceId));
    }

    private static ServiceBindingDto toDto(ServiceBindingEntity entity) {
        return new ServiceBindingDto(
                entity.id,
                entity.instanceId,
                entity.serviceId,
                entity.planId,
                entity.platformClientId,
                entity.state,
                copy(entity.credentials),
                copy(entity.parameters),
                entity.lastOperationId,
                entity.lastOperationState,
                entity.lastOperationDescription,
                entity.lastOperationKind);
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(value));
    }
}
