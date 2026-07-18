package io.osb.infrastructure.persistence;

import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.infrastructure.persistence.entity.HttpClientInstanceEntity;
import io.osb.infrastructure.persistence.mapper.HttpClientInstanceMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresHttpClientInstanceRepository implements HttpClientInstanceRepository {

    @Override
    public List<HttpClientInstance> list() {
        return HttpClientInstanceEntity.<HttpClientInstanceEntity>listAll().stream()
                .map(HttpClientInstanceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<HttpClientInstance> findById(String id) {
        return Optional.ofNullable(HttpClientInstanceEntity.<HttpClientInstanceEntity>findById(id))
                .map(HttpClientInstanceMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(HttpClientInstance instance) {
        HttpClientInstanceEntity entity = HttpClientInstanceEntity.findById(instance.id());
        if (entity == null) {
            HttpClientInstanceMapper.toNewEntity(instance).persist();
            return;
        }
        HttpClientInstanceMapper.apply(instance, entity);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return HttpClientInstanceEntity.deleteById(id);
    }
}
