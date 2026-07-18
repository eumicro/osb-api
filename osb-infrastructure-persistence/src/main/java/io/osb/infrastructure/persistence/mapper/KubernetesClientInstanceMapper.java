package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.kubernetesclients.KubernetesClientAuthType;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.infrastructure.persistence.entity.KubernetesClientInstanceEntity;

public final class KubernetesClientInstanceMapper {

    private KubernetesClientInstanceMapper() {}

    public static KubernetesClientInstance toDomain(KubernetesClientInstanceEntity entity) {
        return new KubernetesClientInstance(
                entity.id,
                entity.name,
                entity.description,
                entity.apiServerUrl,
                entity.defaultNamespace,
                KubernetesClientAuthType.valueOf(entity.authType),
                entity.username,
                entity.token,
                entity.oauthClientId,
                entity.oauthClientSecret,
                entity.wellKnownUrl,
                entity.insecureSkipTlsVerify,
                entity.timeoutSeconds,
                entity.enabled);
    }

    public static KubernetesClientInstanceEntity toNewEntity(KubernetesClientInstance instance) {
        KubernetesClientInstanceEntity entity = new KubernetesClientInstanceEntity();
        entity.id = instance.id();
        apply(instance, entity);
        return entity;
    }

    public static void apply(
            KubernetesClientInstance instance, KubernetesClientInstanceEntity entity) {
        entity.name = instance.name();
        entity.description = instance.description();
        entity.apiServerUrl = instance.apiServerUrl();
        entity.defaultNamespace = instance.defaultNamespace();
        entity.authType = instance.authType().name();
        entity.username = instance.username();
        entity.token = instance.token();
        entity.oauthClientId = instance.oauthClientId();
        entity.oauthClientSecret = instance.oauthClientSecret();
        entity.wellKnownUrl = instance.wellKnownUrl();
        entity.insecureSkipTlsVerify = instance.insecureSkipTlsVerify();
        entity.timeoutSeconds = instance.timeoutSeconds();
        entity.enabled = instance.enabled();
    }
}
