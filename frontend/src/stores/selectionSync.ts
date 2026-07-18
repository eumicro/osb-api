import type { AdminCatalog, ServiceOffering } from "../models/catalog";
import type { PlanDetail } from "../models/plan";
import type { PlatformClient } from "../models/platformClient";
import type { ServiceInstance } from "../models/serviceInstance";
import { broadcastDomainSelection } from "./catalogSelection";
import {
  selectCatalog,
  selectInstanceContext,
  selectOffering,
  selectPlan,
  selectPlatformClient,
  workspace,
} from "./workspace";

function clearPlatformIfCatalogMismatch(catalogId: string | null) {
  if (
    !catalogId
    || (workspace.selectedPlatformClient
      && workspace.selectedPlatformClient.catalogId !== catalogId)
  ) {
    workspace.selectedPlatformClientId = null;
    workspace.selectedPlatformClient = null;
  }
}

function findCatalogForOffering(
  catalogs: AdminCatalog[],
  offeringId: string,
): { catalog: AdminCatalog; offering: ServiceOffering } | null {
  for (const catalog of catalogs) {
    const offering = catalog.offerings.find((item) => item.id === offeringId);
    if (offering) return { catalog, offering };
  }
  return null;
}

/** Catalog selected → clear offering/plan/instance; drop mismatched platform; broadcast. */
export function applyCatalogSelection(catalog: AdminCatalog | null) {
  selectCatalog(catalog?.id ?? null, catalog);
  clearPlatformIfCatalogMismatch(catalog?.id ?? null);
  broadcastDomainSelection();
}

/**
 * Offering selected → sync catalog when known, clear plan/instance,
 * drop mismatched platform, broadcast.
 */
export function applyOfferingSelection(
  offering: ServiceOffering | null,
  catalogs: AdminCatalog[] = [],
) {
  if (!offering) {
    selectOffering(null);
    broadcastDomainSelection();
    return;
  }

  const found = findCatalogForOffering(catalogs, offering.id);
  if (found) {
    workspace.selectedCatalogId = found.catalog.id;
    workspace.selectedCatalog = found.catalog;
    clearPlatformIfCatalogMismatch(found.catalog.id);
  }

  selectOffering(offering.id, offering);
  broadcastDomainSelection();
}

/**
 * Plan selected → sync catalog/offering when known, clear mismatched instance, broadcast.
 */
export function applyPlanSelection(
  plan: PlanDetail | null,
  catalogs: AdminCatalog[] = [],
) {
  if (!plan) {
    selectPlan(null);
    broadcastDomainSelection();
    return;
  }

  const found = findCatalogForOffering(catalogs, plan.serviceId);
  if (found) {
    workspace.selectedCatalogId = found.catalog.id;
    workspace.selectedCatalog = found.catalog;
    workspace.selectedOfferingId = found.offering.id;
    workspace.selectedOffering = found.offering;
    clearPlatformIfCatalogMismatch(found.catalog.id);
  } else {
    workspace.selectedOfferingId = plan.serviceId;
    if (workspace.selectedOffering?.id !== plan.serviceId) {
      workspace.selectedOffering = null;
    }
  }

  selectPlan(plan.id, plan.serviceId, plan);
  broadcastDomainSelection();
}

/**
 * Instance selected → sync catalog/offering/plan (+ platform if present), broadcast.
 */
export function applyInstanceSelection(
  instance: ServiceInstance | null,
  catalogs: AdminCatalog[],
  platforms: PlatformClient[] = [],
) {
  const context = selectInstanceContext(instance, catalogs);

  if (instance?.platformClientId) {
    const platform =
      platforms.find((item) => item.id === instance.platformClientId) ?? null;
    workspace.selectedPlatformClientId = platform?.id ?? instance.platformClientId;
    workspace.selectedPlatformClient = platform;
  } else {
    workspace.selectedPlatformClientId = null;
    workspace.selectedPlatformClient = null;
  }

  broadcastDomainSelection();
  return context;
}

/**
 * Platform selected → sync its catalog, clear offering/plan/instance, broadcast.
 */
export function applyPlatformSelection(
  platform: PlatformClient | null,
  catalogs: AdminCatalog[] = [],
) {
  if (!platform) {
    selectPlatformClient(null);
    broadcastDomainSelection();
    return;
  }

  const catalog =
    catalogs.find((item) => item.id === platform.catalogId)
    ?? (workspace.selectedCatalog?.id === platform.catalogId
      ? workspace.selectedCatalog
      : null);

  workspace.selectedCatalogId = platform.catalogId;
  workspace.selectedCatalog = catalog;
  workspace.selectedOfferingId = null;
  workspace.selectedOffering = null;
  workspace.selectedPlanId = null;
  workspace.selectedPlanOfferingId = null;
  workspace.selectedPlan = null;
  workspace.selectedInstanceId = null;
  workspace.selectedInstance = null;
  selectPlatformClient(platform.id, platform);
  broadcastDomainSelection();
}
