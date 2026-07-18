package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import java.util.List;
import java.util.Objects;

public final class ListCatalogsUseCase {

    private final CatalogRepository catalogRepository;

    public ListCatalogsUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public List<Catalog> execute() {
        return catalogRepository.listCatalogs();
    }
}
