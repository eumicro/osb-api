package io.osb.application.gitclients;

import io.osb.domain.gitclients.GitClientInstanceRepository;
import java.util.Objects;

public final class DeleteGitClientInstanceUseCase {

    private final GitClientInstanceRepository repository;

    public DeleteGitClientInstanceUseCase(GitClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(String id) {
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("git client not found: " + id);
        }
    }
}
