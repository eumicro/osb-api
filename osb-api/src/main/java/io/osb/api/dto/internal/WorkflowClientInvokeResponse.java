package io.osb.api.dto.internal;

public record WorkflowClientInvokeResponse(
        boolean ok, String client, String action, String message, String detailsJson) {}
