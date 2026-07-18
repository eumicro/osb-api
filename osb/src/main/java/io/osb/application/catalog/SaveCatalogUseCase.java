package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import java.util.List;
import java.util.Objects;

public final class SaveCatalogUseCase {

    private final CatalogRepository catalogRepository;

    public SaveCatalogUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public Catalog create(String id, String name, String description) {
        if (catalogRepository.findCatalog(id).isPresent()) {
            throw new IllegalArgumentException("catalog already exists: " + id);
        }
        Catalog created = new Catalog(id, name, description, List.of());
        catalogRepository.saveCatalog(created);
        return created;
    }

    public Catalog update(String id, String name, String description) {
        Catalog existing = catalogRepository
                .findCatalog(id)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + id));
        Catalog updated = existing.withDetails(name, description);
        catalogRepository.saveCatalog(updated);
        return updated;
    }
}
