package io.osb.api.dto.admin;

public record CreateHttpClientInstanceRequest(
        String name,
        String description,
        String baseUrl,
        String authType,
        String username,
        String secret,
        String oauthClientId,
        String oauthClientSecret,
        String wellKnownUrl,
        Integer timeoutSeconds,
        Boolean enabled) {}
