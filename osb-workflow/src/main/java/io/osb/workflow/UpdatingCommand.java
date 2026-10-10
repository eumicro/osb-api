package io.osb.workflow;

import java.util.Map;

/** Command to update (PATCH) an existing service instance. */
public record UpdatingCommand(
        String instanceId,
        String serviceId,
        String planId,
        Map<String, Object> parameters,
        Map<String, Object> previousParameters) {}
