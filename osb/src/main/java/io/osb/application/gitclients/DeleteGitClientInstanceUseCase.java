package io.osb.application.gitclients;

import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;

public final class DeleteGitClientInstanceUseCase {

    private final GitClientInstanceRepository repository;
    private final SecretStore secretStore;

    public DeleteGitClientInstanceUseCase(
            GitClientInstanceRepository repository, SecretStore secretStore) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public void execute(String id) {
        GitClientInstance existing = repository.findById(id).orElse(null);
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("git client not found: " + id);
        }
        if (existing != null) {
            deleteRef(existing.secret());
            deleteRef(existing.passphrase());
            secretStore.delete(SecretRefs.gitSecret(id));
            secretStore.delete(SecretRefs.gitPassphrase(id));
        }
    }

    private void deleteRef(String ref) {
        if (ref != null && SecretRefs.isRef(ref)) {
            secretStore.delete(ref);
        }
    }
}
