package io.osb.workflow;

import java.util.Map;

public record DeprovisioningCommand(
        String instanceId,
        String serviceId,
        String planId,
        Map<String, Object> parameters) {

    public DeprovisioningCommand(String instanceId, String serviceId, String planId) {
        this(instanceId, serviceId, planId, Map.of());
    }
}
