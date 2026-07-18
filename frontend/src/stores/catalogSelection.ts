import type { AdminCatalog, ServiceOffering } from "../models/catalog";
import type { PlanDetail } from "../models/plan";
import type { PlatformClient } from "../models/platformClient";
import type { ServiceInstance } from "../models/serviceInstance";
import type { WorkflowDefinition } from "../models/workflow";
import { workspace } from "./workspace";

export const CATALOG_SELECTED_EVENT = "osb:catalog-selected";
export const OFFERING_SELECTED_EVENT = "osb:offering-selected";
export const PLAN_SELECTED_EVENT = "osb:plan-selected";
export const INSTANCE_SELECTED_EVENT = "osb:instance-selected";
export const PLATFORM_SELECTED_EVENT = "osb:platform-selected";
export const WORKFLOW_SELECTED_EVENT = "osb:workflow-selected";

export function notifyCatalogPanelRefresh(catalog?: AdminCatalog | null) {
  window.dispatchEvent(
    new CustomEvent(CATALOG_SELECTED_EVENT, { detail: catalog ?? null }),
  );
}

export function notifyOfferingPanelRefresh(offering?: ServiceOffering | null) {
  window.dispatchEvent(
    new CustomEvent(OFFERING_SELECTED_EVENT, { detail: offering ?? null }),
  );
}

export function notifyPlanPanelRefresh(plan?: PlanDetail | null) {
  window.dispatchEvent(
    new CustomEvent(PLAN_SELECTED_EVENT, { detail: plan ?? null }),
  );
}

export function notifyInstancePanelRefresh(instance?: ServiceInstance | null) {
  window.dispatchEvent(
    new CustomEvent(INSTANCE_SELECTED_EVENT, { detail: instance ?? null }),
  );
}

export function notifyPlatformPanelRefresh(platform?: PlatformClient | null) {
  window.dispatchEvent(
    new CustomEvent(PLATFORM_SELECTED_EVENT, { detail: platform ?? null }),
  );
}

export function notifyWorkflowPanelRefresh(workflow?: WorkflowDefinition | null) {
  window.dispatchEvent(
    new CustomEvent(WORKFLOW_SELECTED_EVENT, { detail: workflow ?? null }),
  );
}

/** Notify all catalog-domain detail panels from current workspace selection. */
export function broadcastDomainSelection() {
  notifyCatalogPanelRefresh(workspace.selectedCatalog);
  notifyOfferingPanelRefresh(workspace.selectedOffering);
  notifyPlanPanelRefresh(workspace.selectedPlan);
  notifyInstancePanelRefresh(workspace.selectedInstance);
  notifyPlatformPanelRefresh(workspace.selectedPlatformClient);
}
