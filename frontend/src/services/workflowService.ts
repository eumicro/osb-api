import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import type {
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
  WorkflowClientStatus,
  WorkflowDefinition,
} from "../models/workflow";
import { http } from "./http";

export const workflowService = {
  list: (query?: PageQuery) =>
    http.get<PageResult<WorkflowDefinition>>(`/api/admin/workflows${toPageQueryString(query)}`),

  get: (id: string) =>
    http.get<WorkflowDefinition>(`/api/admin/workflows/${encodeURIComponent(id)}`),

  create: (request: CreateWorkflowRequest) =>
    http.post<WorkflowDefinition>("/api/admin/workflows", request),

  update: (id: string, request: UpdateWorkflowRequest) =>
    http.put<WorkflowDefinition>(
      `/api/admin/workflows/${encodeURIComponent(id)}`,
      request,
    ),

  remove: (id: string) =>
    http.delete<void>(`/api/admin/workflows/${encodeURIComponent(id)}`),

  listClients: () => http.get<WorkflowClientStatus[]>("/api/admin/workflow-clients"),
};
