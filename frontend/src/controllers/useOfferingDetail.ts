import { ref, watch, type Ref } from "vue";
import type { ServiceOffering } from "../models/catalog";
import type { UpdateOfferingRequest } from "../models/catalogAdmin";
import { catalogAdminService } from "../services/catalogAdminService";
import { applyOfferingSelection } from "../stores/selectionSync";
import {
  notifyOfferingsChanged,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Offering-Detail; Pläne werden im Plan-Panel bearbeitet. */
export function useOfferingDetail(
  catalogId: () => string | null,
  offeringId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const offering = ref<ServiceOffering | null>(null) as Ref<ServiceOffering | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const catId = catalogId();
    const offId = offeringId();
    if (!catId || !offId) {
      offering.value = null;
      if (syncWorkspace) workspace.selectedOffering = null;
      return;
    }
    if (syncWorkspace && workspace.selectedOffering?.id === offId) {
      offering.value = workspace.selectedOffering;
    }
    const result = await run(() => catalogAdminService.getOffering(catId, offId));
    if (result) {
      offering.value = result;
      if (syncWorkspace) workspace.selectedOffering = result;
    }
  }

  async function save(request: UpdateOfferingRequest): Promise<boolean> {
    const catId = catalogId();
    const offId = offeringId();
    if (!catId || !offId) return false;
    const saved = await run(() => catalogAdminService.updateOffering(catId, offId, request));
    if (saved) {
      offering.value = saved;
      if (syncWorkspace) {
        workspace.selectedOfferingId = saved.id;
        workspace.selectedOffering = saved;
      }
      notifyOfferingsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const catId = catalogId();
    const offId = offeringId();
    if (!catId || !offId) return false;
    await run(() => catalogAdminService.deleteOffering(catId, offId));
    offering.value = null;
    if (syncWorkspace) applyOfferingSelection(null);
    notifyOfferingsChanged();
    return error.value === "";
  }

  async function removePlan(planId: string) {
    const catId = catalogId();
    const offId = offeringId();
    if (!catId || !offId) return;
    const saved = await run(() => catalogAdminService.deletePlan(catId, offId, planId));
    if (saved) {
      offering.value = saved;
      if (syncWorkspace) {
        workspace.selectedOfferingId = saved.id;
        workspace.selectedOffering = saved;
        if (workspace.selectedPlanId === planId) {
          workspace.selectedPlanId = null;
          workspace.selectedPlanOfferingId = null;
          workspace.selectedPlan = null;
        }
      }
      notifyOfferingsChanged();
    }
  }

  watch(
    () =>
      [catalogId(), offeringId(), workspace.offeringRevision, workspace.catalogRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return {
    offering,
    loading,
    error,
    load,
    save,
    remove,
    removePlan,
  };
}
