package io.osb.api.dto.admin;

import java.util.List;

public record UpdateWorkflowRequest(
        String name,
        String description,
        String kind,
        String n8nWebhookPath,
        String n8nWorkflowId,
        boolean enabled,
        List<String> clients,
        List<String> httpClientIds,
        List<String> kubernetesClientIds,
        List<String> gitClientIds,
        List<String> templateIds) {}
