package io.osb.api.dto.osb;

import java.util.List;

public record ServiceOfferingDto(
        String id,
        String name,
        String description,
        boolean bindable,
        List<ServicePlanDto> plans) {
}
