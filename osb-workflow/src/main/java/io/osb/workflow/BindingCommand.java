package io.osb.workflow;

import java.util.Map;

public record BindingCommand(
        String instanceId,
        String bindingId,
        String serviceId,
        String planId,
        Map<String, Object> parameters) {}
