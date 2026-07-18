package io.osb.api.dto.admin;

import java.util.Map;

public record ServiceInstanceDto(
        String id,
        String serviceId,
        String planId,
        String state,
        String platformClientId,
        String dashboardUrl,
        Map<String, Object> parameters,
        String lastOperationId,
        String lastOperationState,
        String lastOperationDescription,
        String lastOperationKind) {
}
