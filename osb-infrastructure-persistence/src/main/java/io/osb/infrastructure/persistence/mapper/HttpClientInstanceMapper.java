package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.httpclients.HttpClientAuthType;
import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.infrastructure.persistence.entity.HttpClientInstanceEntity;

public final class HttpClientInstanceMapper {

    private HttpClientInstanceMapper() {}

    public static HttpClientInstance toDomain(HttpClientInstanceEntity entity) {
        return new HttpClientInstance(
                entity.id,
                entity.name,
                entity.description,
                entity.baseUrl,
                HttpClientAuthType.valueOf(entity.authType),
                entity.username,
                entity.secret,
                entity.oauthClientId,
                entity.oauthClientSecret,
                entity.wellKnownUrl,
                entity.timeoutSeconds,
                entity.enabled);
    }

    public static HttpClientInstanceEntity toNewEntity(HttpClientInstance instance) {
        HttpClientInstanceEntity entity = new HttpClientInstanceEntity();
        entity.id = instance.id();
        apply(instance, entity);
        return entity;
    }

    public static void apply(HttpClientInstance instance, HttpClientInstanceEntity entity) {
        entity.name = instance.name();
        entity.description = instance.description();
        entity.baseUrl = instance.baseUrl();
        entity.authType = instance.authType().name();
        entity.username = instance.username();
        entity.secret = instance.secret();
        entity.oauthClientId = instance.oauthClientId();
        entity.oauthClientSecret = instance.oauthClientSecret();
        entity.wellKnownUrl = instance.wellKnownUrl();
        entity.timeoutSeconds = instance.timeoutSeconds();
        entity.enabled = instance.enabled();
    }
}
