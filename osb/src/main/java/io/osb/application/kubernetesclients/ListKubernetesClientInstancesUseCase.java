package io.osb.application.kubernetesclients;

import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import java.util.List;
import java.util.Objects;

public final class ListKubernetesClientInstancesUseCase {

    private final KubernetesClientInstanceRepository repository;

    public ListKubernetesClientInstancesUseCase(KubernetesClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<KubernetesClientInstance> execute() {
        return repository.list();
    }
}
