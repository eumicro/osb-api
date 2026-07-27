package io.osb.application.httpclients;

import io.osb.domain.httpclients.HttpClientAuthType;
import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;
import java.util.UUID;

public final class SaveHttpClientInstanceUseCase {

    private final HttpClientInstanceRepository repository;
    private final SecretStore secretStore;

    public SaveHttpClientInstanceUseCase(
            HttpClientInstanceRepository repository, SecretStore secretStore) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
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
        String id = "http-" + UUID.randomUUID().toString().substring(0, 8);
        String secretRef = storeIfPresent(SecretRefs.httpSecret(id), secret);
        String oauthRef = storeIfPresent(SecretRefs.httpOauthClientSecret(id), oauthClientSecret);
        HttpClientInstance created = new HttpClientInstance(
                id,
                name,
                description,
                baseUrl,
                authType,
                username,
                secretRef,
                oauthClientId,
                oauthRef,
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
        String secretRef = resolveUpdateRef(
                existing.secret(),
                SecretRefs.httpSecret(id),
                secret,
                keepExistingSecret);
        String oauthRef = resolveUpdateRef(
                existing.oauthClientSecret(),
                SecretRefs.httpOauthClientSecret(id),
                oauthClientSecret,
                keepExistingOauthClientSecret);
        HttpClientInstance updated = existing.withDetails(
                name,
                description,
                baseUrl,
                authType,
                username,
                secretRef,
                oauthClientId,
                oauthRef,
                wellKnownUrl,
                timeoutSeconds,
                enabled);
        repository.save(updated);
        return updated;
    }

    private String storeIfPresent(String ref, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        secretStore.put(ref, value);
        return ref;
    }

    private String resolveUpdateRef(
            String existingRef, String canonicalRef, String newValue, boolean keepExisting) {
        if (keepExisting || newValue == null || newValue.isBlank()) {
            return existingRef == null ? "" : existingRef;
        }
        String ref = (existingRef != null && SecretRefs.isRef(existingRef))
                ? existingRef
                : canonicalRef;
        secretStore.put(ref, newValue);
        return ref;
    }
}
