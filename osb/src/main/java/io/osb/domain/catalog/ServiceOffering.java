package io.osb.domain.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rich domain aggregate root (skeleton) for a catalog service offering ("product").
 */
public final class ServiceOffering {

    private final String id;
    private final String name;
    private final String description;
    private final boolean bindable;
    private final List<ServicePlan> plans;

    public ServiceOffering(
            String id,
            String name,
            String description,
            boolean bindable,
            List<ServicePlan> plans) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.bindable = bindable;
        Objects.requireNonNull(plans, "plans");
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("plans must contain at least one plan");
        }
        this.plans = List.copyOf(plans);
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

    public boolean bindable() {
        return bindable;
    }

    public List<ServicePlan> plans() {
        return plans;
    }

    public ServiceOffering withDetails(String name, String description, boolean bindable) {
        return new ServiceOffering(id, name, description, bindable, plans);
    }

    public ServiceOffering withAddedPlan(ServicePlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plans.stream().anyMatch(existing -> existing.id().equals(plan.id()))) {
            throw new IllegalArgumentException("plan id already exists: " + plan.id());
        }
        List<ServicePlan> updated = new ArrayList<>(plans);
        updated.add(plan);
        return new ServiceOffering(id, name, description, bindable, updated);
    }

    public ServiceOffering withReplacedPlan(ServicePlan plan) {
        Objects.requireNonNull(plan, "plan");
        List<ServicePlan> updated = new ArrayList<>(plans.size());
        boolean replaced = false;
        for (ServicePlan existing : plans) {
            if (existing.id().equals(plan.id())) {
                updated.add(plan);
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            throw new IllegalArgumentException("plan not found: " + plan.id());
        }
        return new ServiceOffering(id, name, description, bindable, updated);
    }

    public ServiceOffering withoutPlan(String planId) {
        requireText(planId, "planId");
        List<ServicePlan> updated = plans.stream()
                .filter(plan -> !plan.id().equals(planId))
                .toList();
        if (updated.size() == plans.size()) {
            throw new IllegalArgumentException("plan not found: " + planId);
        }
        return new ServiceOffering(id, name, description, bindable, updated);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
