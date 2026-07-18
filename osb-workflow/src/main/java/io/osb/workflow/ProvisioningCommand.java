package io.osb.workflow;

import java.util.Map;

public record ProvisioningCommand(
        String instanceId,
        String serviceId,
        String planId,
        Map<String, Object> parameters) {

    public ProvisioningCommand(String instanceId, String serviceId, String planId) {
        this(instanceId, serviceId, planId, Map.of());
    }
}
