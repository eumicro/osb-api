package io.osb.infrastructure.persistence;

import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.infrastructure.persistence.entity.GitClientInstanceEntity;
import io.osb.infrastructure.persistence.mapper.GitClientInstanceMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresGitClientInstanceRepository implements GitClientInstanceRepository {

    @Override
    public List<GitClientInstance> list() {
        return GitClientInstanceEntity.<GitClientInstanceEntity>listAll().stream()
                .map(GitClientInstanceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<GitClientInstance> findById(String id) {
        return Optional.ofNullable(GitClientInstanceEntity.<GitClientInstanceEntity>findById(id))
                .map(GitClientInstanceMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(GitClientInstance instance) {
        GitClientInstanceEntity entity = GitClientInstanceEntity.findById(instance.id());
        if (entity == null) {
            GitClientInstanceMapper.toNewEntity(instance).persist();
            return;
        }
        GitClientInstanceMapper.apply(instance, entity);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return GitClientInstanceEntity.deleteById(id);
    }
}
