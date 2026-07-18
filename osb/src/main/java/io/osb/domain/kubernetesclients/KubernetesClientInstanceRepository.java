package io.osb.domain.kubernetesclients;

import java.util.List;
import java.util.Optional;

public interface KubernetesClientInstanceRepository {

    List<KubernetesClientInstance> list();

    Optional<KubernetesClientInstance> findById(String id);

    void save(KubernetesClientInstance instance);

    boolean delete(String id);
}
