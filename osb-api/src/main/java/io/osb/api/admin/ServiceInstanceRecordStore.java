package io.osb.api.admin;

import io.osb.api.dto.admin.ServiceInstanceDto;
import java.util.List;
import java.util.Optional;

/** Persistence port for admin service-instance records. */
public interface ServiceInstanceRecordStore {

    List<ServiceInstanceDto> listAll();

    Optional<ServiceInstanceDto> findById(String id);

    void save(ServiceInstanceDto dto);

    void delete(String id);
}
