package io.osb.api.dto.osb;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LastOperationResponse(String state, String description) {
}
