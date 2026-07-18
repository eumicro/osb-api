package io.osb.api.dto.admin;

import java.util.List;

/**
 * Paginated admin list response. Not part of OSB {@code /v2/*} (catalog has no pagination in the
 * OSB specification).
 */
public record PageDto<T>(List<T> items, int page, int pageSize, long total, int pageCount) {}
