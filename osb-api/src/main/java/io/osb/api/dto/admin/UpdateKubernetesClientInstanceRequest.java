package io.osb.api.dto.admin;

public record UpdateKubernetesClientInstanceRequest(
        String name,
        String description,
        String apiServerUrl,
        String defaultNamespace,
        String authType,
        String username,
        String token,
        String oauthClientId,
        String oauthClientSecret,
        String wellKnownUrl,
        Boolean insecureSkipTlsVerify,
        Integer timeoutSeconds,
        Boolean enabled) {}
