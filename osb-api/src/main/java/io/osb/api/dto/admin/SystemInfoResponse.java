package io.osb.api.dto.admin;

import java.util.Map;

public record SystemInfoResponse(
        String name,
        String version,
        String status,
        Map<String, String> adapters,
        Map<String, String> devServices) {
}
