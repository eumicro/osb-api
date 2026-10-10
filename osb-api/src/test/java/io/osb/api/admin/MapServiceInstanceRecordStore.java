package io.osb.api.admin;

import io.osb.api.dto.admin.ServiceInstanceDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link ServiceInstanceRecordStore} for unit tests. */
public final class MapServiceInstanceRecordStore implements ServiceInstanceRecordStore {

    private final Map<String, ServiceInstanceDto> byId = new LinkedHashMap<>();

    @Override
    public List<ServiceInstanceDto> listAll() {
        return List.copyOf(new ArrayList<>(byId.values()));
    }

    @Override
    public Optional<ServiceInstanceDto> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void save(ServiceInstanceDto dto) {
        byId.put(dto.id(), dto);
    }

    @Override
    public void delete(String id) {
        byId.remove(id);
    }
}
