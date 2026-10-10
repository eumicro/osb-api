package io.osb.api.admin;

import io.osb.api.dto.osb.ServiceBindingDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MapServiceBindingRecordStore implements ServiceBindingRecordStore {

    private final Map<String, ServiceBindingDto> byKey = new LinkedHashMap<>();

    private static String key(String instanceId, String bindingId) {
        return instanceId + "::" + bindingId;
    }

    @Override
    public List<ServiceBindingDto> listByInstance(String instanceId) {
        List<ServiceBindingDto> out = new ArrayList<>();
        for (ServiceBindingDto dto : byKey.values()) {
            if (instanceId.equals(dto.instanceId())) {
                out.add(dto);
            }
        }
        return List.copyOf(out);
    }

    @Override
    public Optional<ServiceBindingDto> find(String instanceId, String bindingId) {
        return Optional.ofNullable(byKey.get(key(instanceId, bindingId)));
    }

    @Override
    public void save(ServiceBindingDto dto) {
        byKey.put(key(dto.instanceId(), dto.id()), dto);
    }

    @Override
    public void delete(String instanceId, String bindingId) {
        byKey.remove(key(instanceId, bindingId));
    }
}
