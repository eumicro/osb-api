package io.osb.api.mapper;

import io.osb.api.dto.admin.CatalogDto;
import io.osb.api.dto.admin.PlatformClientDto;
import io.osb.api.dto.osb.CatalogResponse;
import io.osb.api.dto.osb.ServiceOfferingDto;
import io.osb.api.dto.osb.ServicePlanDto;
import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.domain.platforms.PlatformClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class CatalogMapper {

    /** OSB broker projection: offerings only. */
    public CatalogResponse toResponse(Catalog catalog) {
        return new CatalogResponse(catalog.offerings().stream().map(this::toDto).toList());
    }

    public CatalogDto toAdminDto(Catalog catalog) {
        return new CatalogDto(
                catalog.id(),
                catalog.name(),
                catalog.description(),
                catalog.offerings().stream().map(this::toDto).toList());
    }

    public PlatformClientDto toDto(PlatformClient client) {
        return new PlatformClientDto(
                client.id(),
                client.displayName(),
                client.username(),
                client.catalogId(),
                client.enabled());
    }

    public ServiceOfferingDto toDto(ServiceOffering offering) {
        return new ServiceOfferingDto(
                offering.id(),
                offering.name(),
                offering.description(),
                offering.bindable(),
                offering.plans().stream().map(this::toDto).toList());
    }

    public ServicePlanDto toDto(ServicePlan plan) {
        return new ServicePlanDto(
                plan.id(),
                plan.name(),
                plan.description(),
                plan.free(),
                plan.bindable(),
                plan.schemas(),
                plan.parametersUiSchema());
    }

    public ServicePlan toDomain(ServicePlanDto plan) {
        return new ServicePlan(
                plan.id(),
                plan.name(),
                plan.description(),
                plan.free(),
                plan.bindable(),
                nullToEmpty(plan.schemas()),
                nullToEmpty(plan.parametersUiSchema()));
    }

    private static Map<String, Object> nullToEmpty(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }
}
