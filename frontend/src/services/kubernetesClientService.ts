import type {
  CreateKubernetesClientInstanceRequest,
  KubernetesClientInstance,
  UpdateKubernetesClientInstanceRequest,
} from "../models/kubernetesClient";
import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import { http } from "./http";

export const kubernetesClientService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<KubernetesClientInstance>>(
      `/api/admin/kubernetes-clients${toPageQueryString(query)}`,
    ),

  get: (id: string) =>
    http.get<KubernetesClientInstance>(
      `/api/admin/kubernetes-clients/${encodeURIComponent(id)}`,
    ),

  create: (request: CreateKubernetesClientInstanceRequest) =>
    http.post<KubernetesClientInstance>("/api/admin/kubernetes-clients", request),

  update: (id: string, request: UpdateKubernetesClientInstanceRequest) =>
    http.put<KubernetesClientInstance>(
      `/api/admin/kubernetes-clients/${encodeURIComponent(id)}`,
      request,
    ),

  delete: (id: string) =>
    http.delete<void>(`/api/admin/kubernetes-clients/${encodeURIComponent(id)}`),
};
