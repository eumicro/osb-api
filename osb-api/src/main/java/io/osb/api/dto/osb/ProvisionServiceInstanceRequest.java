package io.osb.api.dto.osb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProvisionServiceInstanceRequest(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("plan_id") String planId,
        Map<String, Object> parameters,
        Context context) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Context(
            @JsonProperty("platform") String platform,
            @JsonProperty("organization_guid") String organizationGuid,
            @JsonProperty("space_guid") String spaceGuid) {}
}
