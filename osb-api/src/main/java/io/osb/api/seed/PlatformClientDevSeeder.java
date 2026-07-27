package io.osb.api.seed;

import io.osb.application.platforms.SavePlatformClientUseCase;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.platforms.PlatformClientRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Optional dev seed: platform client {@code cf-broker} / password for local OSB catalog demos.
 * Password is written only to {@link io.osb.domain.secrets.SecretStore}, not to SQL.
 */
@ApplicationScoped
public class PlatformClientDevSeeder {

    private static final Logger LOG = Logger.getLogger(PlatformClientDevSeeder.class);

    private static final String DEMO_USERNAME = "cf-broker";
    private static final String DEMO_CATALOG_ID = "default";

    @Inject
    PlatformClientRepository platformClientRepository;

    @Inject
    CatalogRepository catalogRepository;

    @Inject
    SavePlatformClientUseCase savePlatformClientUseCase;

    @ConfigProperty(name = "osb.seed.platform-client.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "osb.seed.platform-client.password", defaultValue = "secret")
    String password;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }
        seedIfMissing();
    }

    @Transactional
    void seedIfMissing() {
        if (platformClientRepository.findByUsername(DEMO_USERNAME).isPresent()) {
            return;
        }
        if (catalogRepository.findCatalog(DEMO_CATALOG_ID).isEmpty()) {
            LOG.warn("Skip platform client seed: catalog '" + DEMO_CATALOG_ID + "' missing");
            return;
        }
        savePlatformClientUseCase.create(
                "CF Broker (dev)", DEMO_USERNAME, DEMO_CATALOG_ID, password, true);
        LOG.info("Seeded demo platform client '" + DEMO_USERNAME + "' for catalog " + DEMO_CATALOG_ID);
    }
}
