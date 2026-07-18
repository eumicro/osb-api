package io.osb.application.httpclients;

import io.osb.domain.httpclients.HttpClientAuthType;
import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import java.util.Objects;
import java.util.UUID;

public final class SaveHttpClientInstanceUseCase {

    private final HttpClientInstanceRepository repository;

    public SaveHttpClientInstanceUseCase(HttpClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public HttpClientInstance create(
            String name,
            String description,
            String baseUrl,
            HttpClientAuthType authType,
            String username,
            String secret,
            String oauthClientId,
            String oauthClientSecret,
            String wellKnownUrl,
            int timeoutSeconds,
            boolean enabled) {
        HttpClientInstance created = new HttpClientInstance(
                "http-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                description,
                baseUrl,
                authType,
                username,
                secret,
                oauthClientId,
                oauthClientSecret,
                wellKnownUrl,
                timeoutSeconds,
                enabled);
        repository.save(created);
        return created;
    }

    public HttpClientInstance update(
            String id,
            String name,
            String description,
            String baseUrl,
            HttpClientAuthType authType,
            String username,
            String secret,
            boolean keepExistingSecret,
            String oauthClientId,
            String oauthClientSecret,
            boolean keepExistingOauthClientSecret,
            String wellKnownUrl,
            int timeoutSeconds,
            boolean enabled) {
        HttpClientInstance existing = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("http client not found: " + id));
        String resolvedSecret = keepExistingSecret || (secret == null || secret.isBlank())
                ? existing.secret()
                : secret;
        String resolvedOauthSecret =
                keepExistingOauthClientSecret
                                || (oauthClientSecret == null || oauthClientSecret.isBlank())
                        ? existing.oauthClientSecret()
                        : oauthClientSecret;
        HttpClientInstance updated = existing.withDetails(
                name,
                description,
                baseUrl,
                authType,
                username,
                resolvedSecret,
                oauthClientId,
                resolvedOauthSecret,
                wellKnownUrl,
                timeoutSeconds,
                enabled);
        repository.save(updated);
        return updated;
    }
}
