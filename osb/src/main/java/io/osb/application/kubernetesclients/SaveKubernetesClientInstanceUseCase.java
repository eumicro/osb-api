package io.osb.application.kubernetesclients;

import io.osb.domain.kubernetesclients.KubernetesClientAuthType;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import java.util.Objects;
import java.util.UUID;

public final class SaveKubernetesClientInstanceUseCase {

    private final KubernetesClientInstanceRepository repository;

    public SaveKubernetesClientInstanceUseCase(KubernetesClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
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
        KubernetesClientInstance created = new KubernetesClientInstance(
                "k8s-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                description,
                apiServerUrl,
                defaultNamespace,
                authType,
                username,
                token,
                oauthClientId,
                oauthClientSecret,
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
        String resolvedToken = keepExistingToken || (token == null || token.isBlank())
                ? existing.token()
                : token;
        String resolvedOauthSecret =
                keepExistingOauthClientSecret
                                || (oauthClientSecret == null || oauthClientSecret.isBlank())
                        ? existing.oauthClientSecret()
                        : oauthClientSecret;
        KubernetesClientInstance updated = existing.withDetails(
                name,
                description,
                apiServerUrl,
                defaultNamespace,
                authType,
                username,
                resolvedToken,
                oauthClientId,
                resolvedOauthSecret,
                wellKnownUrl,
                insecureSkipTlsVerify,
                timeoutSeconds,
                enabled);
        repository.save(updated);
        return updated;
    }
}
