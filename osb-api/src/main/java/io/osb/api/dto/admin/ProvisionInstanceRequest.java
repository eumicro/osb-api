package io.osb.api.dto.admin;

import java.util.Map;

public record ProvisionInstanceRequest(
        String serviceId,
        String planId,
        String platformClientId,
        Map<String, Object> parameters) {
}
