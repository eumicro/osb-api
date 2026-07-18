package io.osb.api.dto.admin;

public record KubernetesClientInstanceDto(
        String id,
        String name,
        String description,
        String apiServerUrl,
        String defaultNamespace,
        String authType,
        String username,
        boolean tokenConfigured,
        String oauthClientId,
        boolean oauthClientSecretConfigured,
        String wellKnownUrl,
        boolean insecureSkipTlsVerify,
        int timeoutSeconds,
        boolean enabled) {}
