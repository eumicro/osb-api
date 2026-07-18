import type { AdminCatalog, ServiceOffering, ServicePlan } from "../models/catalog";
import type {
  CreateCatalogRequest,
  CreateOfferingRequest,
  MovePlanRequest,
  SavePlanRequest,
  UpdateCatalogRequest,
  UpdateOfferingRequest,
} from "../models/catalogAdmin";
import type { PageQuery, PageResult } from "../models/page";
import { toPageQueryString } from "../models/page";
import { http } from "./http";

/** Admin multi-catalog CRUD via `/api/admin/catalogs`. */
export const catalogAdminService = {
  listCatalogs: (query?: PageQuery) =>
    http.get<PageResult<AdminCatalog>>(`/api/admin/catalogs${toPageQueryString(query)}`),

  getCatalog: (catalogId: string) =>
    http.get<AdminCatalog>(`/api/admin/catalogs/${encodeURIComponent(catalogId)}`),

  createCatalog: (request: CreateCatalogRequest) =>
    http.post<AdminCatalog>("/api/admin/catalogs", request),

  updateCatalog: (catalogId: string, request: UpdateCatalogRequest) =>
    http.put<AdminCatalog>(`/api/admin/catalogs/${encodeURIComponent(catalogId)}`, request),

  deleteCatalog: (catalogId: string) =>
    http.delete<void>(`/api/admin/catalogs/${encodeURIComponent(catalogId)}`),

  listOfferings: (catalogId: string, query?: PageQuery) =>
    http.get<PageResult<ServiceOffering>>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings${toPageQueryString(query)}`,
    ),

  getOffering: (catalogId: string, offeringId: string) =>
    http.get<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}`,
    ),

  createOffering: (catalogId: string, request: CreateOfferingRequest) =>
    http.post<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings`,
      request,
    ),

  updateOffering: (catalogId: string, offeringId: string, request: UpdateOfferingRequest) =>
    http.put<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}`,
      request,
    ),

  deleteOffering: (catalogId: string, offeringId: string) =>
    http.delete<void>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}`,
    ),

  createPlan: (catalogId: string, offeringId: string, request: SavePlanRequest) =>
    http.post<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}/plans`,
      request,
    ),

  getPlan: (catalogId: string, offeringId: string, planId: string) =>
    http.get<ServicePlan>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}/plans/${encodeURIComponent(planId)}`,
    ),

  updatePlan: (
    catalogId: string,
    offeringId: string,
    planId: string,
    request: SavePlanRequest,
  ) =>
    http.put<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}/plans/${encodeURIComponent(planId)}`,
      request,
    ),

  deletePlan: (catalogId: string, offeringId: string, planId: string) =>
    http.delete<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}/plans/${encodeURIComponent(planId)}`,
      undefined,
    ),

  movePlan: (
    catalogId: string,
    offeringId: string,
    planId: string,
    request: MovePlanRequest,
  ) =>
    http.post<ServiceOffering>(
      `/api/admin/catalogs/${encodeURIComponent(catalogId)}/offerings/${encodeURIComponent(offeringId)}/plans/${encodeURIComponent(planId)}/move`,
      request,
    ),
};
