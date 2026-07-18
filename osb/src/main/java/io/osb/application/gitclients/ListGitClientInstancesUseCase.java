package io.osb.application.gitclients;

import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import java.util.List;
import java.util.Objects;

public final class ListGitClientInstancesUseCase {

    private final GitClientInstanceRepository repository;

    public ListGitClientInstancesUseCase(GitClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<GitClientInstance> execute() {
        return repository.list();
    }
}
