import { computed, ref, watch } from "vue";
import type { ServiceOffering } from "../models/catalog";
import type { CreatePlanRequest } from "../models/catalogAdmin";
import type { PlanRow } from "../models/plan";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { catalogAdminService } from "../services/catalogAdminService";
import {
  notifyOfferingsChanged,
  selectPlan,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export type { PlanRow };

/** Controller: Plans des selektierten Katalogs (optional gefiltert nach Offering). */
export function usePlans() {
  const offerings = ref<ServiceOffering[]>([]);
  const { loading, error, run } = useAsyncAction();

  const selectedOfferingId = computed(() => workspace.selectedOfferingId);
  const selectedPlanId = computed(() => workspace.selectedPlanId);
  const selectedPlanOfferingId = computed(() => workspace.selectedPlanOfferingId);
  const catalogId = computed(() => workspace.selectedCatalogId);

  const plans = computed(() => {
    const rows: PlanRow[] = [];
    for (const offering of offerings.value) {
      if (selectedOfferingId.value && offering.id !== selectedOfferingId.value) continue;
      for (const plan of offering.plans) {
        rows.push({ ...plan, serviceId: offering.id, serviceName: offering.name });
      }
    }
    return rows;
  });

  async function load() {
    const id = catalogId.value;
    if (!id) {
      offerings.value = [];
      return;
    }
    const result = await run(() =>
      catalogAdminService.listOfferings(id, { page: 1, pageSize: ADMIN_LOOKUP_PAGE_SIZE }),
    );
    if (result) offerings.value = result.items;
  }

  function select(plan: PlanRow) {
    selectPlan(plan.id, plan.serviceId, plan);
  }

  async function create(request: CreatePlanRequest): Promise<PlanRow | undefined> {
    const id = catalogId.value;
    if (!id) return undefined;
    const { offeringId, ...plan } = request;
    const saved = await run(() => catalogAdminService.createPlan(id, offeringId, plan));
    if (saved) {
      notifyOfferingsChanged();
      await load();
      const created = saved.plans.find((item) => item.id === plan.id);
      if (created) {
        const row = { ...created, serviceId: saved.id, serviceName: saved.name };
        selectPlan(row.id, row.serviceId, row);
        return row;
      }
    }
    return undefined;
  }

  async function remove(offeringId: string, planId: string) {
    const id = catalogId.value;
    if (!id) return;
    await run(() => catalogAdminService.deletePlan(id, offeringId, planId));
    if (workspace.selectedPlanId === planId) selectPlan(null);
    notifyOfferingsChanged();
    await load();
  }

  watch(
    () => [catalogId.value, workspace.offeringRevision, workspace.catalogRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return {
    plans,
    offerings,
    selectedOfferingId,
    selectedPlanId,
    selectedPlanOfferingId,
    catalogId,
    loading,
    error,
    load,
    select,
    create,
    remove,
  };
}
