import type { ServicePlan } from "./catalog";

export interface CreateCatalogRequest {
  id: string;
  name: string;
  description: string;
}

export interface UpdateCatalogRequest {
  name: string;
  description: string;
}

/** Save payload from catalog detail form (catalog fields + platform assignment). */
export interface CatalogSavePayload {
  catalog: UpdateCatalogRequest;
  assignedPlatformIds: string[];
  fallbackCatalogId: string | null;
}

export interface CreateOfferingRequest {
  id: string;
  name: string;
  description: string;
  bindable: boolean;
  initialPlan: ServicePlan;
}

export interface UpdateOfferingRequest {
  name: string;
  description: string;
  bindable: boolean;
}

export type SavePlanRequest = ServicePlan;

export interface CreatePlanRequest extends SavePlanRequest {
  offeringId: string;
}

export interface MovePlanRequest {
  targetOfferingId: string;
}

export interface UpdatePlanRequest extends SavePlanRequest {
  offeringId: string;
}
