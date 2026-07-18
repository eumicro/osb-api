import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import type {
  CreateTemplateRequest,
  Template,
  UpdateTemplateRequest,
} from "../models/template";
import { http } from "./http";

export const templateService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<Template>>(`/api/admin/templates${toPageQueryString(query)}`),

  get: (id: string) =>
    http.get<Template>(`/api/admin/templates/${encodeURIComponent(id)}`),

  create: (request: CreateTemplateRequest) =>
    http.post<Template>("/api/admin/templates", request),

  update: (id: string, request: UpdateTemplateRequest) =>
    http.put<Template>(`/api/admin/templates/${encodeURIComponent(id)}`, request),

  remove: (id: string) =>
    http.delete<void>(`/api/admin/templates/${encodeURIComponent(id)}`),
};
