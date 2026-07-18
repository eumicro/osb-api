package io.osb.api.dto.admin;

import io.osb.api.dto.osb.ServicePlanDto;

public record CreateOfferingRequest(
        String id,
        String name,
        String description,
        boolean bindable,
        ServicePlanDto initialPlan) {
}
