import type {
  CreateGitClientInstanceRequest,
  GitClientInstance,
  UpdateGitClientInstanceRequest,
} from "../models/gitClient";
import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import { http } from "./http";

export const gitClientService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<GitClientInstance>>(
      `/api/admin/git-clients${toPageQueryString(query)}`,
    ),

  get: (id: string) =>
    http.get<GitClientInstance>(`/api/admin/git-clients/${encodeURIComponent(id)}`),

  create: (request: CreateGitClientInstanceRequest) =>
    http.post<GitClientInstance>("/api/admin/git-clients", request),

  update: (id: string, request: UpdateGitClientInstanceRequest) =>
    http.put<GitClientInstance>(`/api/admin/git-clients/${encodeURIComponent(id)}`, request),

  delete: (id: string) =>
    http.delete<void>(`/api/admin/git-clients/${encodeURIComponent(id)}`),
};
