import type {
  CreateHttpClientInstanceRequest,
  HttpClientInstance,
  UpdateHttpClientInstanceRequest,
} from "../models/httpClient";
import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import { http } from "./http";

export const httpClientService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<HttpClientInstance>>(
      `/api/admin/http-clients${toPageQueryString(query)}`,
    ),

  get: (id: string) =>
    http.get<HttpClientInstance>(`/api/admin/http-clients/${encodeURIComponent(id)}`),

  create: (request: CreateHttpClientInstanceRequest) =>
    http.post<HttpClientInstance>("/api/admin/http-clients", request),

  update: (id: string, request: UpdateHttpClientInstanceRequest) =>
    http.put<HttpClientInstance>(`/api/admin/http-clients/${encodeURIComponent(id)}`, request),

  delete: (id: string) =>
    http.delete<void>(`/api/admin/http-clients/${encodeURIComponent(id)}`),
};
