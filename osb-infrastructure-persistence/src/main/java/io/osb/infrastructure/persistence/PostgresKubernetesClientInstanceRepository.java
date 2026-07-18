package io.osb.infrastructure.persistence;

import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.infrastructure.persistence.entity.KubernetesClientInstanceEntity;
import io.osb.infrastructure.persistence.mapper.KubernetesClientInstanceMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresKubernetesClientInstanceRepository
        implements KubernetesClientInstanceRepository {

    @Override
    public List<KubernetesClientInstance> list() {
        return KubernetesClientInstanceEntity.<KubernetesClientInstanceEntity>listAll().stream()
                .map(KubernetesClientInstanceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<KubernetesClientInstance> findById(String id) {
        return Optional.ofNullable(
                        KubernetesClientInstanceEntity.<KubernetesClientInstanceEntity>findById(id))
                .map(KubernetesClientInstanceMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(KubernetesClientInstance instance) {
        KubernetesClientInstanceEntity entity =
                KubernetesClientInstanceEntity.findById(instance.id());
        if (entity == null) {
            KubernetesClientInstanceMapper.toNewEntity(instance).persist();
            return;
        }
        KubernetesClientInstanceMapper.apply(instance, entity);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return KubernetesClientInstanceEntity.deleteById(id);
    }
}
