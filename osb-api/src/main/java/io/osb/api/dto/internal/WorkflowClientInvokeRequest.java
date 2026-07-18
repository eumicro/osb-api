package io.osb.api.dto.internal;

import java.util.Map;

public record WorkflowClientInvokeRequest(
        String action, Map<String, Object> payload, String operationId) {}
