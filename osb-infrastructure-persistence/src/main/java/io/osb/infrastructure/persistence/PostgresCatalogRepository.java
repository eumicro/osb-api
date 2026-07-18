package io.osb.infrastructure.persistence;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.infrastructure.persistence.entity.CatalogEntity;
import io.osb.infrastructure.persistence.mapper.CatalogMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresCatalogRepository implements CatalogRepository {

    @Override
    public List<Catalog> listCatalogs() {
        return CatalogEntity.<CatalogEntity>listAll().stream()
                .map(CatalogMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Catalog> findCatalog(String catalogId) {
        return Optional.ofNullable(CatalogEntity.<CatalogEntity>findById(catalogId))
                .map(CatalogMapper::toDomain);
    }

    @Override
    @Transactional
    public void saveCatalog(Catalog catalog) {
        CatalogEntity entity = CatalogEntity.findById(catalog.id());
        if (entity == null) {
            CatalogMapper.toNewEntity(catalog).persist();
            return;
        }
        CatalogMapper.apply(catalog, entity);
    }

    @Override
    @Transactional
    public boolean deleteCatalog(String catalogId) {
        return CatalogEntity.deleteById(catalogId);
    }
}
