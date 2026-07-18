package io.osb.infrastructure.persistence;

import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.infrastructure.persistence.entity.PlatformClientEntity;
import io.osb.infrastructure.persistence.mapper.PlatformClientMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresPlatformClientRepository implements PlatformClientRepository {

    @Override
    public List<PlatformClient> list() {
        return PlatformClientEntity.<PlatformClientEntity>listAll().stream()
                .map(PlatformClientMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PlatformClient> findById(String id) {
        return Optional.ofNullable(PlatformClientEntity.<PlatformClientEntity>findById(id))
                .map(PlatformClientMapper::toDomain);
    }

    @Override
    public Optional<PlatformClient> findByUsername(String username) {
        return PlatformClientEntity.<PlatformClientEntity>find("username", username)
                .firstResultOptional()
                .map(PlatformClientMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(PlatformClient platformClient) {
        PlatformClientEntity entity = PlatformClientEntity.findById(platformClient.id());
        if (entity != null) {
            PlatformClientMapper.apply(platformClient, entity);
            return;
        }
        if (findByUsername(platformClient.username()).isPresent()) {
            throw new IllegalArgumentException(
                    "username already in use: " + platformClient.username());
        }
        PlatformClientMapper.toNewEntity(platformClient).persist();
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return PlatformClientEntity.deleteById(id);
    }
}
