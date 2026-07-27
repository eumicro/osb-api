package io.osb.infrastructure.secrets;

import io.osb.domain.secrets.SecretStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Selects in-memory or OpenBao {@link SecretStore} based on {@code osb.secrets.provider}.
 */
@ApplicationScoped
public class ConfigurableSecretStore implements SecretStore {

    private final SecretStore delegate;

    public ConfigurableSecretStore(
            @ConfigProperty(name = "osb.secrets.provider", defaultValue = "memory") String provider,
            @ConfigProperty(name = "osb.secrets.openbao.url", defaultValue = "http://localhost:8200")
                    String openBaoUrl,
            @ConfigProperty(name = "osb.secrets.openbao.token", defaultValue = "osb-root")
                    String openBaoToken,
            @ConfigProperty(name = "osb.secrets.openbao.mount", defaultValue = "secret")
                    String openBaoMount) {
        if ("openbao".equalsIgnoreCase(provider.trim())) {
            this.delegate = new OpenBaoSecretStore(openBaoUrl, openBaoToken, openBaoMount);
        } else {
            this.delegate = new InMemorySecretStore();
        }
    }

    @Override
    public void put(String ref, String value) {
        delegate.put(ref, value);
    }

    @Override
    public Optional<String> get(String ref) {
        return delegate.get(ref);
    }

    @Override
    public void delete(String ref) {
        delegate.delete(ref);
    }
}
