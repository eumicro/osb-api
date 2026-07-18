package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.gitclients.GitClientAuthMethod;
import io.osb.domain.gitclients.GitClientInstance;
import io.osb.infrastructure.persistence.entity.GitClientInstanceEntity;

public final class GitClientInstanceMapper {

    private GitClientInstanceMapper() {}

    public static GitClientInstance toDomain(GitClientInstanceEntity entity) {
        return new GitClientInstance(
                entity.id,
                entity.name,
                entity.description,
                entity.remoteUrl,
                entity.defaultBranch,
                GitClientAuthMethod.valueOf(entity.authMethod),
                entity.username,
                entity.secret,
                entity.passphrase,
                entity.enabled);
    }

    public static GitClientInstanceEntity toNewEntity(GitClientInstance instance) {
        GitClientInstanceEntity entity = new GitClientInstanceEntity();
        entity.id = instance.id();
        apply(instance, entity);
        return entity;
    }

    public static void apply(GitClientInstance instance, GitClientInstanceEntity entity) {
        entity.name = instance.name();
        entity.description = instance.description();
        entity.remoteUrl = instance.remoteUrl();
        entity.defaultBranch = instance.defaultBranch();
        entity.authMethod = instance.authMethod().name();
        entity.username = instance.username();
        entity.secret = instance.secret();
        entity.passphrase = instance.passphrase();
        entity.enabled = instance.enabled();
    }
}
