package io.osb.application.kubernetesclients;

import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import java.util.Objects;

public final class GetKubernetesClientInstanceUseCase {

    private final KubernetesClientInstanceRepository repository;

    public GetKubernetesClientInstanceUseCase(KubernetesClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public KubernetesClientInstance execute(String id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("kubernetes client not found: " + id));
    }
}
