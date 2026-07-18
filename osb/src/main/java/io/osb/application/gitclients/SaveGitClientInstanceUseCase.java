package io.osb.application.gitclients;

import io.osb.domain.gitclients.GitClientAuthMethod;
import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import java.util.Objects;
import java.util.UUID;

public final class SaveGitClientInstanceUseCase {

    private final GitClientInstanceRepository repository;

    public SaveGitClientInstanceUseCase(GitClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
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
        GitClientInstance created = new GitClientInstance(
                "git-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                description,
                remoteUrl,
                defaultBranch,
                authMethod,
                username,
                secret,
                passphrase,
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
        String resolvedSecret = keepExistingSecret || (secret == null || secret.isBlank())
                ? existing.secret()
                : secret;
        String resolvedPassphrase =
                keepExistingPassphrase || passphrase == null
                        ? existing.passphrase()
                        : passphrase;
        GitClientInstance updated = existing.withDetails(
                name,
                description,
                remoteUrl,
                defaultBranch,
                authMethod,
                username,
                resolvedSecret,
                resolvedPassphrase,
                enabled);
        repository.save(updated);
        return updated;
    }
}
