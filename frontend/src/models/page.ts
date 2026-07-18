/** Paginated admin list response (`/api/admin/*`, not OSB `/v2/*`). */
export interface PageResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  pageCount: number;
}

export interface PageQuery {
  page?: number;
  pageSize?: number;
}

/** Max admin page size (backend `Pages.MAX_PAGE_SIZE`) for dropdowns / lookups. */
export const ADMIN_LOOKUP_PAGE_SIZE = 100;

export function toPageQueryString(query?: PageQuery): string {
  const params = new URLSearchParams();
  if (query?.page != null) params.set("page", String(query.page));
  if (query?.pageSize != null) params.set("pageSize", String(query.pageSize));
  const value = params.toString();
  return value ? `?${value}` : "";
}
