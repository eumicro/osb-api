package io.osb.api.dto.osb;

import java.util.Map;

public record ServiceBindingDto(
        String id,
        String instanceId,
        String serviceId,
        String planId,
        String platformClientId,
        String state,
        Map<String, Object> credentials,
        Map<String, Object> parameters,
        String lastOperationId,
        String lastOperationState,
        String lastOperationDescription,
        String lastOperationKind) {}
