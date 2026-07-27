package io.osb.application.kubernetesclients;

import io.osb.domain.kubernetesclients.KubernetesClientAuthType;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;
import java.util.UUID;

public final class SaveKubernetesClientInstanceUseCase {

    private final KubernetesClientInstanceRepository repository;
    private final SecretStore secretStore;

    public SaveKubernetesClientInstanceUseCase(
            KubernetesClientInstanceRepository repository, SecretStore secretStore) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public KubernetesClientInstance create(
            String name,
            String description,
            String apiServerUrl,
            String defaultNamespace,
            KubernetesClientAuthType authType,
            String username,
            String token,
            String oauthClientId,
            String oauthClientSecret,
            String wellKnownUrl,
            boolean insecureSkipTlsVerify,
            int timeoutSeconds,
            boolean enabled) {
        String id = "k8s-" + UUID.randomUUID().toString().substring(0, 8);
        String tokenRef = storeIfPresent(SecretRefs.k8sToken(id), token);
        String oauthRef = storeIfPresent(SecretRefs.k8sOauthClientSecret(id), oauthClientSecret);
        KubernetesClientInstance created = new KubernetesClientInstance(
                id,
                name,
                description,
                apiServerUrl,
                defaultNamespace,
                authType,
                username,
                tokenRef,
                oauthClientId,
                oauthRef,
                wellKnownUrl,
                insecureSkipTlsVerify,
                timeoutSeconds,
                enabled);
        repository.save(created);
        return created;
    }

    public KubernetesClientInstance update(
            String id,
            String name,
            String description,
            String apiServerUrl,
            String defaultNamespace,
            KubernetesClientAuthType authType,
            String username,
            String token,
            boolean keepExistingToken,
            String oauthClientId,
            String oauthClientSecret,
            boolean keepExistingOauthClientSecret,
            String wellKnownUrl,
            boolean insecureSkipTlsVerify,
            int timeoutSeconds,
            boolean enabled) {
        KubernetesClientInstance existing = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("kubernetes client not found: " + id));
        String tokenRef = resolveUpdateRef(
                existing.token(), SecretRefs.k8sToken(id), token, keepExistingToken);
        String oauthRef = resolveUpdateRef(
                existing.oauthClientSecret(),
                SecretRefs.k8sOauthClientSecret(id),
                oauthClientSecret,
                keepExistingOauthClientSecret);
        KubernetesClientInstance updated = existing.withDetails(
                name,
                description,
                apiServerUrl,
                defaultNamespace,
                authType,
                username,
                tokenRef,
                oauthClientId,
                oauthRef,
                wellKnownUrl,
                insecureSkipTlsVerify,
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
