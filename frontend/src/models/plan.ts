import type { ServicePlan } from "./catalog";

/** Plan with offering context (list row / detail / selection). */
export type PlanDetail = ServicePlan & {
  serviceId: string;
  serviceName: string;
};

/** Alias used by plan list collections. */
export type PlanRow = PlanDetail;
