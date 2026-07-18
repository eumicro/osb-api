package io.osb.api.dto.admin;

import io.osb.api.dto.osb.ServiceOfferingDto;
import java.util.List;

public record CatalogDto(
        String id,
        String name,
        String description,
        List<ServiceOfferingDto> offerings) {
}
