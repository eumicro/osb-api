package io.osb.api.dto.osb;

import java.util.Map;

public record ServicePlanDto(
        String id,
        String name,
        String description,
        boolean free,
        boolean bindable,
        Map<String, Object> schemas,
        Map<String, Object> parametersUiSchema) {
}
