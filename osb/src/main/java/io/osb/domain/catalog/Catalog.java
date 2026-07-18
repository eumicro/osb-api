package io.osb.domain.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Catalog aggregate: named collection of service offerings.
 * Visibility to platforms is controlled via {@code PlatformClient.catalogId},
 * not a global publish flag.
 */
public final class Catalog {

    private final String id;
    private final String name;
    private final String description;
    private final List<ServiceOffering> offerings;

    public Catalog(String id, String name, String description, List<ServiceOffering> offerings) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.offerings = List.copyOf(Objects.requireNonNull(offerings, "offerings"));
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<ServiceOffering> offerings() {
        return offerings;
    }

    public boolean isEmpty() {
        return offerings.isEmpty();
    }

    public Optional<ServiceOffering> findOffering(String offeringId) {
        return offerings.stream().filter(offering -> offering.id().equals(offeringId)).findFirst();
    }

    public Catalog withDetails(String name, String description) {
        return new Catalog(id, name, description, offerings);
    }

    public Catalog withAddedOffering(ServiceOffering offering) {
        Objects.requireNonNull(offering, "offering");
        if (findOffering(offering.id()).isPresent()) {
            throw new IllegalArgumentException("offering already exists: " + offering.id());
        }
        List<ServiceOffering> updated = new ArrayList<>(offerings);
        updated.add(offering);
        return new Catalog(id, name, description, updated);
    }

    public Catalog withReplacedOffering(ServiceOffering offering) {
        Objects.requireNonNull(offering, "offering");
        List<ServiceOffering> updated = new ArrayList<>(offerings.size());
        boolean replaced = false;
        for (ServiceOffering existing : offerings) {
            if (existing.id().equals(offering.id())) {
                updated.add(offering);
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            throw new IllegalArgumentException("offering not found: " + offering.id());
        }
        return new Catalog(id, name, description, updated);
    }

    public Catalog withoutOffering(String offeringId) {
        requireText(offeringId, "offeringId");
        List<ServiceOffering> updated = offerings.stream()
                .filter(offering -> !offering.id().equals(offeringId))
                .toList();
        if (updated.size() == offerings.size()) {
            throw new IllegalArgumentException("offering not found: " + offeringId);
        }
        return new Catalog(id, name, description, updated);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
