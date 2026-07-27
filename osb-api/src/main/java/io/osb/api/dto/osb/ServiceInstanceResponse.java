package io.osb.api.dto.osb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceInstanceResponse(
        @JsonProperty("dashboard_url") String dashboardUrl,
        String operation) {
}
