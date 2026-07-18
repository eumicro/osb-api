package io.osb.domain.catalog;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for multiple catalogs.
 */
public interface CatalogRepository {

    List<Catalog> listCatalogs();

    Optional<Catalog> findCatalog(String catalogId);

    void saveCatalog(Catalog catalog);

    boolean deleteCatalog(String catalogId);
}
