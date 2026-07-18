package io.osb.api.dto.admin;

import java.util.Map;

public record SavePlanRequest(
        String id,
        String name,
        String description,
        boolean free,
        boolean bindable,
        Map<String, Object> schemas,
        Map<String, Object> parametersUiSchema) {
}
