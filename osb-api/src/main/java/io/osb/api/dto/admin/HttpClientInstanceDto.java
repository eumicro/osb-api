package io.osb.api.dto.admin;

public record HttpClientInstanceDto(
        String id,
        String name,
        String description,
        String baseUrl,
        String authType,
        String username,
        boolean secretConfigured,
        String oauthClientId,
        boolean oauthClientSecretConfigured,
        String wellKnownUrl,
        int timeoutSeconds,
        boolean enabled) {}
