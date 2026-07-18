<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import { usePlanDetail } from "../../controllers/usePlanDetail";
import type { SavePlanRequest } from "../../models/catalogAdmin";
import { PLAN_SELECTED_EVENT } from "../../stores/catalogSelection";
import { applyPlanSelection } from "../../stores/selectionSync";
import type { PlanPanelParams } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import PlanDetailForm from "../organisms/PlanDetailForm.vue";

interface DockviewComponentProps {
  params?: PlanPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.planId);

const boundCatalogId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.catalogId ?? null)
    : workspace.selectedCatalogId;

const boundOfferingId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.offeringId ?? null)
    : workspace.selectedPlanOfferingId ?? workspace.selectedOfferingId;

const boundPlanId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.planId ?? null)
    : workspace.selectedPlanId;

const { plan, offerings, loading, error, load, save, moveToOffering, remove } = usePlanDetail(
  boundCatalogId,
  boundOfferingId,
  boundPlanId,
  { syncWorkspace: !isBoundTab.value },
);

const meta = computed(() => {
  if (!plan.value) return undefined;
  return `${boundCatalogId()} / ${plan.value.serviceId} / ${plan.value.id}`;
});

const empty = computed(() => {
  if (!boundCatalogId()) return t("offerings.selectCatalog");
  if (!boundPlanId()) return t("plan.selectPlan");
  return undefined;
});

const deleteDisabled = computed(() => {
  if (!plan.value) return true;
  const offering = offerings.value.find((entry) => entry.id === plan.value?.serviceId);
  return (offering?.plans.length ?? 0) <= 1;
});

async function onSave(request: SavePlanRequest, targetOfferingId: string) {
  const sourceOfferingId = boundOfferingId();
  const ok = await save(request);
  if (!ok) return;
  if (sourceOfferingId && targetOfferingId !== sourceOfferingId) {
    await moveToOffering(targetOfferingId);
  }
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundPlanId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("plans.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  applyPlanSelection(null);
}

function onExternalSelect() {
  if (isBoundTab.value) return;
  void load();
}

onMounted(() => {
  window.addEventListener(PLAN_SELECTED_EVENT, onExternalSelect);
});
onUnmounted(() => {
  window.removeEventListener(PLAN_SELECTED_EVENT, onExternalSelect);
});
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!plan"
      :loading="!!boundPlanId() && loading && !plan"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <PlanDetailForm
        v-if="plan"
        ref="formRef"
        :plan="plan"
        :offerings="offerings"
        @save="onSave"
      />
      <template #actions>
        <DeleteButton
          :label="$t('common.delete')"
          :disabled="deleteDisabled"
          @click="onDelete"
        />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
