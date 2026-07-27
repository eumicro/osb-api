package io.osb.application.kubernetesclients;

import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;

public final class DeleteKubernetesClientInstanceUseCase {

    private final KubernetesClientInstanceRepository repository;
    private final SecretStore secretStore;

    public DeleteKubernetesClientInstanceUseCase(
            KubernetesClientInstanceRepository repository, SecretStore secretStore) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public void execute(String id) {
        KubernetesClientInstance existing = repository.findById(id).orElse(null);
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("kubernetes client not found: " + id);
        }
        if (existing != null) {
            deleteRef(existing.token());
            deleteRef(existing.oauthClientSecret());
            secretStore.delete(SecretRefs.k8sToken(id));
            secretStore.delete(SecretRefs.k8sOauthClientSecret(id));
        }
    }

    private void deleteRef(String ref) {
        if (ref != null && SecretRefs.isRef(ref)) {
            secretStore.delete(ref);
        }
    }
}
