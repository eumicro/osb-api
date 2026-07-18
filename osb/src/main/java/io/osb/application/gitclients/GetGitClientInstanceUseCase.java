package io.osb.application.gitclients;

import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import java.util.Objects;

public final class GetGitClientInstanceUseCase {

    private final GitClientInstanceRepository repository;

    public GetGitClientInstanceUseCase(GitClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public GitClientInstance execute(String id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("git client not found: " + id));
    }
}
