package io.osb.api.dto.admin;

import java.util.List;

/** Builds {@link PageDto} slices for admin list endpoints. */
public final class Pages {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private Pages() {}

    public static <T> PageDto<T> of(List<T> all, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safeSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        long total = all.size();
        int pageCount = total == 0 ? 1 : (int) Math.ceil((double) total / safeSize);
        if (safePage > pageCount) {
            safePage = pageCount;
        }
        int from = (int) Math.min((long) (safePage - 1) * safeSize, total);
        int to = (int) Math.min(from + safeSize, total);
        return new PageDto<>(List.copyOf(all.subList(from, to)), safePage, safeSize, total, pageCount);
    }
}
