package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.platforms.PlatformClient;
import io.osb.infrastructure.persistence.entity.PlatformClientEntity;

public final class PlatformClientMapper {

    private PlatformClientMapper() {}

    public static PlatformClient toDomain(PlatformClientEntity entity) {
        return new PlatformClient(
                entity.id,
                entity.displayName,
                entity.username,
                entity.catalogId,
                entity.enabled);
    }

    public static PlatformClientEntity toNewEntity(PlatformClient client) {
        PlatformClientEntity entity = new PlatformClientEntity();
        entity.id = client.id();
        apply(client, entity);
        return entity;
    }

    public static void apply(PlatformClient client, PlatformClientEntity entity) {
        entity.displayName = client.displayName();
        entity.username = client.username();
        entity.catalogId = client.catalogId();
        entity.enabled = client.enabled();
    }
}
