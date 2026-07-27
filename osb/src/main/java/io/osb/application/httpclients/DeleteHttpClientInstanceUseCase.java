package io.osb.application.httpclients;

import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;

public final class DeleteHttpClientInstanceUseCase {

    private final HttpClientInstanceRepository repository;
    private final SecretStore secretStore;

    public DeleteHttpClientInstanceUseCase(
            HttpClientInstanceRepository repository, SecretStore secretStore) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public void execute(String id) {
        HttpClientInstance existing = repository.findById(id).orElse(null);
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("http client not found: " + id);
        }
        if (existing != null) {
            deleteRef(existing.secret());
            deleteRef(existing.oauthClientSecret());
            // Canonical paths in case entity still held plaintext
            secretStore.delete(SecretRefs.httpSecret(id));
            secretStore.delete(SecretRefs.httpOauthClientSecret(id));
        }
    }

    private void deleteRef(String ref) {
        if (ref != null && SecretRefs.isRef(ref)) {
            secretStore.delete(ref);
        }
    }
}
