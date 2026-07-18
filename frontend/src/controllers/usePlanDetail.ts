import { ref, watch, type Ref } from "vue";
import type { ServiceOffering } from "../models/catalog";
import type { SavePlanRequest } from "../models/catalogAdmin";
import type { PlanDetail } from "../models/plan";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { catalogAdminService } from "../services/catalogAdminService";
import { applyPlanSelection } from "../stores/selectionSync";
import {
  notifyOfferingsChanged,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export type { PlanDetail };

/** Controller: Plan-Detail inkl. Offering-Zuordnung. */
export function usePlanDetail(
  catalogId: () => string | null,
  offeringId: () => string | null,
  planId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const plan = ref<PlanDetail | null>(null) as Ref<PlanDetail | null>;
  const offerings = ref<ServiceOffering[]>([]);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const catId = catalogId();
    const offId = offeringId();
    const pId = planId();
    if (!catId || !offId || !pId) {
      plan.value = null;
      offerings.value = [];
      return;
    }

    if (
      syncWorkspace
      && workspace.selectedPlan?.id === pId
      && workspace.selectedPlan.serviceId === offId
    ) {
      plan.value = workspace.selectedPlan;
    }

    const [planResult, offeringList] = await Promise.all([
      run(() => catalogAdminService.getPlan(catId, offId, pId)),
      catalogAdminService
        .listOfferings(catId, { page: 1, pageSize: ADMIN_LOOKUP_PAGE_SIZE })
        .catch(() => null),
    ]);

    if (offeringList) offerings.value = offeringList.items;
    if (!planResult) {
      if (!plan.value) plan.value = null;
      return;
    }

    const offeringName =
      offeringList?.items.find((item) => item.id === offId)?.name
      ?? workspace.selectedPlan?.serviceName
      ?? offId;
    const detail: PlanDetail = {
      ...planResult,
      serviceId: offId,
      serviceName: offeringName,
    };
    plan.value = detail;
    if (syncWorkspace) workspace.selectedPlan = detail;
  }

  async function save(request: SavePlanRequest): Promise<boolean> {
    const catId = catalogId();
    const offId = offeringId();
    if (!catId || !offId) return false;
    const saved = await run(() => catalogAdminService.updatePlan(catId, offId, request.id, request));
    if (saved) {
      const updated = saved.plans.find((item) => item.id === request.id);
      if (updated) {
        const detail: PlanDetail = {
          ...updated,
          serviceId: saved.id,
          serviceName: saved.name,
        };
        plan.value = detail;
        if (syncWorkspace) {
          workspace.selectedPlanId = detail.id;
          workspace.selectedPlanOfferingId = detail.serviceId;
          workspace.selectedPlan = detail;
          workspace.selectedOfferingId = detail.serviceId;
        }
      }
      notifyOfferingsChanged();
      return true;
    }
    return false;
  }

  async function moveToOffering(targetOfferingId: string): Promise<boolean> {
    const catId = catalogId();
    const offId = offeringId();
    const pId = planId();
    if (!catId || !offId || !pId) return false;
    if (targetOfferingId === offId) return true;
    const saved = await run(() =>
      catalogAdminService.movePlan(catId, offId, pId, { targetOfferingId }),
    );
    if (saved) {
      const updated = saved.plans.find((item) => item.id === pId);
      if (updated) {
        const detail: PlanDetail = {
          ...updated,
          serviceId: saved.id,
          serviceName: saved.name,
        };
        plan.value = detail;
        if (syncWorkspace) {
          applyPlanSelection(detail, workspace.selectedCatalog
            ? [{ ...workspace.selectedCatalog, offerings: [saved] }]
            : []);
        }
      }
      notifyOfferingsChanged();
      await load();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const catId = catalogId();
    const offId = offeringId();
    const pId = planId();
    if (!catId || !offId || !pId) return false;
    await run(() => catalogAdminService.deletePlan(catId, offId, pId));
    plan.value = null;
    if (syncWorkspace) applyPlanSelection(null);
    notifyOfferingsChanged();
    return error.value === "";
  }

  watch(
    () =>
      [
        catalogId(),
        offeringId(),
        planId(),
        workspace.offeringRevision,
        workspace.catalogRevision,
      ] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return {
    plan,
    offerings,
    loading,
    error,
    load,
    save,
    moveToOffering,
    remove,
  };
}
