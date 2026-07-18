package io.osb.api.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime endpoints for platform services.
 *
 * <p>Env overrides (examples): {@code OSB_POSTGRES_JDBC_URL}, {@code OSB_KEYCLOAK_URL},
 * {@code OSB_N8N_BASE_URL}.
 */
public final class OsbServicesConfig {

    private OsbServicesConfig() {}

    @ConfigMapping(prefix = "osb.postgres")
    public interface Postgres {
        @WithDefault("jdbc:postgresql://localhost:5432/osb")
        String jdbcUrl();

        @WithDefault("osb")
        String username();

        @WithDefault("osb")
        String password();
    }

    @ConfigMapping(prefix = "osb.keycloak")
    public interface Keycloak {
        @WithDefault("http://localhost:8180")
        String url();

        @WithDefault("osb")
        String realm();
    }
}
