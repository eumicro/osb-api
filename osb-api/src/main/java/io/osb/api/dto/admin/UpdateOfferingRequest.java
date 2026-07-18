package io.osb.api.dto.admin;

public record UpdateOfferingRequest(
        String name,
        String description,
        boolean bindable) {
}
