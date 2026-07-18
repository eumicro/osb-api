package io.osb.application.kubernetesclients;

import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import java.util.Objects;

public final class DeleteKubernetesClientInstanceUseCase {

    private final KubernetesClientInstanceRepository repository;

    public DeleteKubernetesClientInstanceUseCase(KubernetesClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(String id) {
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("kubernetes client not found: " + id);
        }
    }
}
