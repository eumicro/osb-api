package io.osb.application.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Platform A must only see catalog A offerings — never catalog B.
 */
class GetCatalogForPlatformUseCaseIsolationTest {

    private MapCatalogRepo catalogs;
    private MapPlatformRepo platforms;
    private GetCatalogForPlatformUseCase useCase;

    @BeforeEach
    void setUp() {
        catalogs = new MapCatalogRepo();
        platforms = new MapPlatformRepo();
        useCase = new GetCatalogForPlatformUseCase(platforms, catalogs);

        catalogs.saveCatalog(catalog(
                "catalog-a",
                offering("svc-a", "plan-a")));
        catalogs.saveCatalog(catalog(
                "catalog-b",
                offering("svc-b", "plan-b")));

        platforms.save(new PlatformClient(
                "platform-a", "Platform A", "user-a", "catalog-a", "osb/platform/platform-a/password", true));
        platforms.save(new PlatformClient(
                "platform-b", "Platform B", "user-b", "catalog-b", "osb/platform/platform-b/password", true));
    }

    @Test
    void platformASeesOnlyOwnCatalogOfferings() {
        Catalog catalog = useCase.execute("user-a");

        assertEquals("catalog-a", catalog.id());
        Set<String> offeringIds =
                catalog.offerings().stream().map(ServiceOffering::id).collect(Collectors.toSet());
        assertEquals(Set.of("svc-a"), offeringIds);
        assertTrue(offeringIds.stream().noneMatch(id -> id.equals("svc-b")));
    }

    @Test
    void platformBSeesOnlyOwnCatalogOfferings() {
        Catalog catalog = useCase.execute("user-b");

        assertEquals("catalog-b", catalog.id());
        Set<String> offeringIds =
                catalog.offerings().stream().map(ServiceOffering::id).collect(Collectors.toSet());
        assertEquals(Set.of("svc-b"), offeringIds);
        assertTrue(offeringIds.stream().noneMatch(id -> id.equals("svc-a")));
    }

    private static Catalog catalog(String id, ServiceOffering offering) {
        return new Catalog(id, id, "desc", List.of(offering));
    }

    private static ServiceOffering offering(String serviceId, String planId) {
        return new ServiceOffering(
                serviceId,
                serviceId,
                "desc",
                false,
                List.of(new ServicePlan(planId, planId, "desc", true, false, Map.of(), Map.of())));
    }

    private static final class MapCatalogRepo implements CatalogRepository {
        private final Map<String, Catalog> byId = new HashMap<>();

        @Override
        public List<Catalog> listCatalogs() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<Catalog> findCatalog(String catalogId) {
            return Optional.ofNullable(byId.get(catalogId));
        }

        @Override
        public void saveCatalog(Catalog catalog) {
            byId.put(catalog.id(), catalog);
        }

        @Override
        public boolean deleteCatalog(String catalogId) {
            return byId.remove(catalogId) != null;
        }
    }

    private static final class MapPlatformRepo implements PlatformClientRepository {
        private final Map<String, PlatformClient> byId = new HashMap<>();

        @Override
        public List<PlatformClient> list() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<PlatformClient> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<PlatformClient> findByUsername(String username) {
            return byId.values().stream().filter(p -> p.username().equals(username)).findFirst();
        }

        @Override
        public void save(PlatformClient platformClient) {
            byId.put(platformClient.id(), platformClient);
        }

        @Override
        public boolean delete(String id) {
            return byId.remove(id) != null;
        }
    }
}
