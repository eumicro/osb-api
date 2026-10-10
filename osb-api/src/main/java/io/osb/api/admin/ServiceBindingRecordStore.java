package io.osb.api.admin;

import io.osb.api.dto.osb.ServiceBindingDto;
import java.util.List;
import java.util.Optional;

public interface ServiceBindingRecordStore {

    List<ServiceBindingDto> listByInstance(String instanceId);

    Optional<ServiceBindingDto> find(String instanceId, String bindingId);

    void save(ServiceBindingDto dto);

    void delete(String instanceId, String bindingId);
}
