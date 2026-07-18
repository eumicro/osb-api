package io.osb.api.dto.admin;

public record CreateCatalogRequest(
        String id,
        String name,
        String description) {
}
