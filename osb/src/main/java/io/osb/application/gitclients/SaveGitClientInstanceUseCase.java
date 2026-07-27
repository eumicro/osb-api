package io.osb.application.gitclients;

import io.osb.domain.gitclients.GitClientAuthMethod;
import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;
import java.util.UUID;

public final class SaveGitClientInstanceUseCase {

    private final GitClientInstanceRepository repository;
    private final SecretStore secretStore;

    public SaveGitClientInstanceUseCase(
            GitClientInstanceRepository repository, SecretStore secretStore) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public GitClientInstance create(
            String name,
            String description,
            String remoteUrl,
            String defaultBranch,
            GitClientAuthMethod authMethod,
            String username,
            String secret,
            String passphrase,
            boolean enabled) {
        String id = "git-" + UUID.randomUUID().toString().substring(0, 8);
        String secretRef = storeRequired(SecretRefs.gitSecret(id), secret);
        String passphraseRef = storeIfPresent(SecretRefs.gitPassphrase(id), passphrase);
        GitClientInstance created = new GitClientInstance(
                id,
                name,
                description,
                remoteUrl,
                defaultBranch,
                authMethod,
                username,
                secretRef,
                passphraseRef,
                enabled);
        repository.save(created);
        return created;
    }

    public GitClientInstance update(
            String id,
            String name,
            String description,
            String remoteUrl,
            String defaultBranch,
            GitClientAuthMethod authMethod,
            String username,
            String secret,
            boolean keepExistingSecret,
            String passphrase,
            boolean keepExistingPassphrase,
            boolean enabled) {
        GitClientInstance existing = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("git client not found: " + id));
        String secretRef = resolveUpdateRef(
                existing.secret(), SecretRefs.gitSecret(id), secret, keepExistingSecret);
        String passphraseRef;
        if (keepExistingPassphrase || passphrase == null) {
            passphraseRef = existing.passphrase();
        } else if (passphrase.isBlank()) {
            deleteRef(existing.passphrase());
            passphraseRef = "";
        } else {
            passphraseRef = resolveUpdateRef(
                    existing.passphrase(),
                    SecretRefs.gitPassphrase(id),
                    passphrase,
                    false);
        }
        GitClientInstance updated = existing.withDetails(
                name,
                description,
                remoteUrl,
                defaultBranch,
                authMethod,
                username,
                secretRef,
                passphraseRef,
                enabled);
        repository.save(updated);
        return updated;
    }

    private String storeRequired(String ref, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("secret must not be blank");
        }
        secretStore.put(ref, value);
        return ref;
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

    private void deleteRef(String ref) {
        if (ref != null && SecretRefs.isRef(ref)) {
            secretStore.delete(ref);
        }
    }
}
