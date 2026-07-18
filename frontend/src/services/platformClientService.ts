import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import type {
  CreatePlatformClientRequest,
  PlatformClient,
  UpdatePlatformClientRequest,
} from "../models/platformClient";
import { http } from "./http";

export const platformClientService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<PlatformClient>>(
      `/api/admin/platform-clients${toPageQueryString(query)}`,
    ),

  get: (id: string) =>
    http.get<PlatformClient>(`/api/admin/platform-clients/${encodeURIComponent(id)}`),

  create: (request: CreatePlatformClientRequest) =>
    http.post<PlatformClient>("/api/admin/platform-clients", request),

  update: (id: string, request: UpdatePlatformClientRequest) =>
    http.put<PlatformClient>(`/api/admin/platform-clients/${encodeURIComponent(id)}`, request),

  delete: (id: string) =>
    http.delete<void>(`/api/admin/platform-clients/${encodeURIComponent(id)}`),
};
