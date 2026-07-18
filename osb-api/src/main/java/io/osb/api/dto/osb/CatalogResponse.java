package io.osb.api.dto.osb;

import java.util.List;

public record CatalogResponse(List<ServiceOfferingDto> services) {
}
