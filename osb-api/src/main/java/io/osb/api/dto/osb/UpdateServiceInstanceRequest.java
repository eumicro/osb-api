package io.osb.api.dto.osb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateServiceInstanceRequest(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("plan_id") String planId,
        Map<String, Object> parameters,
        @JsonProperty("previous_values") PreviousValues previousValues,
        ProvisionServiceInstanceRequest.Context context) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PreviousValues(
            @JsonProperty("service_id") String serviceId,
            @JsonProperty("plan_id") String planId,
            Map<String, Object> parameters) {}
}
