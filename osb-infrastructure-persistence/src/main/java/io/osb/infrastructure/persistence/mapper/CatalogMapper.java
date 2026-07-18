package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.infrastructure.persistence.entity.CatalogEntity;
import io.osb.infrastructure.persistence.entity.ServiceOfferingEntity;
import io.osb.infrastructure.persistence.entity.ServicePlanEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CatalogMapper {

    private CatalogMapper() {}

    public static Catalog toDomain(CatalogEntity entity) {
        List<ServiceOffering> offerings = entity.offerings.stream()
                .map(CatalogMapper::toDomain)
                .toList();
        return new Catalog(entity.id, entity.name, entity.description, offerings);
    }

    public static ServiceOffering toDomain(ServiceOfferingEntity entity) {
        List<ServicePlan> plans = entity.plans.stream().map(CatalogMapper::toDomain).toList();
        return new ServiceOffering(
                entity.id, entity.name, entity.description, entity.bindable, plans);
    }

    public static ServicePlan toDomain(ServicePlanEntity entity) {
        return new ServicePlan(
                entity.id,
                entity.name,
                entity.description,
                entity.free,
                entity.bindable,
                entity.schemas,
                entity.parametersUiSchema);
    }

    public static CatalogEntity toNewEntity(Catalog catalog) {
        CatalogEntity entity = new CatalogEntity();
        entity.id = catalog.id();
        apply(catalog, entity);
        return entity;
    }

    public static void apply(Catalog catalog, CatalogEntity entity) {
        entity.name = catalog.name();
        entity.description = catalog.description();
        syncOfferings(catalog, entity);
    }

    private static void syncOfferings(Catalog catalog, CatalogEntity entity) {
        Map<String, ServiceOfferingEntity> existing = new HashMap<>();
        for (ServiceOfferingEntity offering : entity.offerings) {
            existing.put(offering.id, offering);
        }

        List<ServiceOfferingEntity> next = new ArrayList<>();
        int order = 0;
        for (ServiceOffering offering : catalog.offerings()) {
            ServiceOfferingEntity offeringEntity = existing.remove(offering.id());
            if (offeringEntity == null) {
                offeringEntity = new ServiceOfferingEntity();
                offeringEntity.id = offering.id();
                offeringEntity.catalog = entity;
            }
            offeringEntity.name = offering.name();
            offeringEntity.description = offering.description();
            offeringEntity.bindable = offering.bindable();
            offeringEntity.sortOrder = order++;
            syncPlans(offering, offeringEntity);
            next.add(offeringEntity);
        }
        entity.offerings.clear();
        entity.offerings.addAll(next);
    }

    private static void syncPlans(ServiceOffering offering, ServiceOfferingEntity entity) {
        Map<String, ServicePlanEntity> existing = new HashMap<>();
        for (ServicePlanEntity plan : entity.plans) {
            existing.put(plan.id, plan);
        }

        List<ServicePlanEntity> next = new ArrayList<>();
        int order = 0;
        for (ServicePlan plan : offering.plans()) {
            ServicePlanEntity planEntity = existing.remove(plan.id());
            if (planEntity == null) {
                planEntity = new ServicePlanEntity();
                planEntity.id = plan.id();
                planEntity.offering = entity;
            }
            planEntity.name = plan.name();
            planEntity.description = plan.description();
            planEntity.free = plan.free();
            planEntity.bindable = plan.bindable();
            planEntity.schemas = plan.schemas().isEmpty()
                    ? new HashMap<>()
                    : new HashMap<>(plan.schemas());
            planEntity.parametersUiSchema = plan.parametersUiSchema().isEmpty()
                    ? new HashMap<>()
                    : new HashMap<>(plan.parametersUiSchema());
            planEntity.sortOrder = order++;
            next.add(planEntity);
        }
        entity.plans.clear();
        entity.plans.addAll(next);
    }
}
