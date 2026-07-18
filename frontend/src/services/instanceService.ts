import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import type {
  ProvisionInstanceRequest,
  ServiceInstance,
  UpdateInstanceRequest,
} from "../models/serviceInstance";
import { http } from "./http";

export const instanceService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<ServiceInstance>>(
      `/api/admin/service-instances${toPageQueryString(query)}`,
    ),

  get: (id: string) =>
    http.get<ServiceInstance>(`/api/admin/service-instances/${encodeURIComponent(id)}`),

  provision: (request: ProvisionInstanceRequest) =>
    http.post<ServiceInstance>("/api/admin/service-instances", request),

  update: (id: string, request: UpdateInstanceRequest) =>
    http.put<ServiceInstance>(
      `/api/admin/service-instances/${encodeURIComponent(id)}`,
      request,
    ),

  deprovision: (id: string) =>
    http.delete<ServiceInstance>(
      `/api/admin/service-instances/${encodeURIComponent(id)}`,
    ),
};
