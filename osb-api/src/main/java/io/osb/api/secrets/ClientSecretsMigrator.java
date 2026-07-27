package io.osb.api.secrets;

import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * One-shot dual-read migration: plaintext client credential columns → SecretStore + refs.
 */
@ApplicationScoped
public class ClientSecretsMigrator {

    private static final Logger LOG = Logger.getLogger(ClientSecretsMigrator.class);

    @Inject
    SecretStore secretStore;

    @Inject
    HttpClientInstanceRepository httpClients;

    @Inject
    GitClientInstanceRepository gitClients;

    @Inject
    KubernetesClientInstanceRepository kubernetesClients;

    @Inject
    PlatformClientRepository platformClients;

    void onStart(@Observes StartupEvent event) {
        migrateAll();
    }

    @Transactional
    void migrateAll() {
        int moved = 0;
        for (HttpClientInstance instance : httpClients.list()) {
            if (migrateHttp(instance)) {
                moved++;
            }
        }
        for (GitClientInstance instance : gitClients.list()) {
            if (migrateGit(instance)) {
                moved++;
            }
        }
        for (KubernetesClientInstance instance : kubernetesClients.list()) {
            if (migrateK8s(instance)) {
                moved++;
            }
        }
        for (PlatformClient platform : platformClients.list()) {
            if (migratePlatform(platform)) {
                moved++;
            }
        }
        if (moved > 0) {
            LOG.info("Migrated plaintext client secrets to SecretStore for " + moved + " client(s)");
        }
    }

    private boolean migrateHttp(HttpClientInstance instance) {
        String secretRef = instance.secret();
        String oauthRef = instance.oauthClientSecret();
        boolean changed = false;
        String newSecret = secretRef;
        String newOauth = oauthRef;
        if (needsMigration(secretRef)) {
            newSecret = SecretRefs.httpSecret(instance.id());
            secretStore.put(newSecret, secretRef);
            changed = true;
        }
        if (needsMigration(oauthRef)) {
            newOauth = SecretRefs.httpOauthClientSecret(instance.id());
            secretStore.put(newOauth, oauthRef);
            changed = true;
        }
        if (changed) {
            httpClients.save(instance.withDetails(
                    instance.name(),
                    instance.description(),
                    instance.baseUrl(),
                    instance.authType(),
                    instance.username(),
                    newSecret,
                    instance.oauthClientId(),
                    newOauth,
                    instance.wellKnownUrl(),
                    instance.timeoutSeconds(),
                    instance.enabled()));
        }
        return changed;
    }

    private boolean migrateGit(GitClientInstance instance) {
        String secretRef = instance.secret();
        String passphraseRef = instance.passphrase();
        boolean changed = false;
        String newSecret = secretRef;
        String newPassphrase = passphraseRef;
        if (needsMigration(secretRef)) {
            newSecret = SecretRefs.gitSecret(instance.id());
            secretStore.put(newSecret, secretRef);
            changed = true;
        }
        if (needsMigration(passphraseRef)) {
            newPassphrase = SecretRefs.gitPassphrase(instance.id());
            secretStore.put(newPassphrase, passphraseRef);
            changed = true;
        }
        if (changed) {
            gitClients.save(instance.withDetails(
                    instance.name(),
                    instance.description(),
                    instance.remoteUrl(),
                    instance.defaultBranch(),
                    instance.authMethod(),
                    instance.username(),
                    newSecret,
                    newPassphrase,
                    instance.enabled()));
        }
        return changed;
    }

    private boolean migrateK8s(KubernetesClientInstance instance) {
        String tokenRef = instance.token();
        String oauthRef = instance.oauthClientSecret();
        boolean changed = false;
        String newToken = tokenRef;
        String newOauth = oauthRef;
        if (needsMigration(tokenRef)) {
            newToken = SecretRefs.k8sToken(instance.id());
            secretStore.put(newToken, tokenRef);
            changed = true;
        }
        if (needsMigration(oauthRef)) {
            newOauth = SecretRefs.k8sOauthClientSecret(instance.id());
            secretStore.put(newOauth, oauthRef);
            changed = true;
        }
        if (changed) {
            kubernetesClients.save(instance.withDetails(
                    instance.name(),
                    instance.description(),
                    instance.apiServerUrl(),
                    instance.defaultNamespace(),
                    instance.authType(),
                    instance.username(),
                    newToken,
                    instance.oauthClientId(),
                    newOauth,
                    instance.wellKnownUrl(),
                    instance.insecureSkipTlsVerify(),
                    instance.timeoutSeconds(),
                    instance.enabled()));
        }
        return changed;
    }

    private boolean migratePlatform(PlatformClient platform) {
        String passwordRef = platform.passwordRef();
        if (!needsMigration(passwordRef)) {
            return false;
        }
        String newRef = SecretRefs.platformPassword(platform.id());
        secretStore.put(newRef, passwordRef);
        platformClients.save(platform.withDetails(
                platform.displayName(),
                platform.username(),
                platform.catalogId(),
                newRef,
                platform.enabled()));
        return true;
    }

    private static boolean needsMigration(String value) {
        return value != null && !value.isBlank() && !SecretRefs.isRef(value);
    }
}
