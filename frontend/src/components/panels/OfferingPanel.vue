<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import { useOfferingDetail } from "../../controllers/useOfferingDetail";
import type { ServicePlan } from "../../models/catalog";
import type { UpdateOfferingRequest } from "../../models/catalogAdmin";
import {
  OFFERING_SELECTED_EVENT,
} from "../../stores/catalogSelection";
import { applyOfferingSelection, applyPlanSelection } from "../../stores/selectionSync";
import type { OfferingPanelParams } from "../../stores/workspaceLayout";
import { focusPanel } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import OfferingDetailForm from "../organisms/OfferingDetailForm.vue";

interface DockviewComponentProps {
  params?: OfferingPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.offeringId);

const boundCatalogId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.catalogId ?? null)
    : workspace.selectedCatalogId;

const boundOfferingId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.offeringId ?? null)
    : workspace.selectedOfferingId;

const { offering, loading, error, load, save, remove, removePlan } = useOfferingDetail(
  boundCatalogId,
  boundOfferingId,
  { syncWorkspace: !isBoundTab.value },
);

const meta = computed(() => {
  if (!offering.value) return undefined;
  return `${boundCatalogId()} / ${offering.value.id}`;
});

const empty = computed(() => {
  if (!boundCatalogId()) return t("offerings.selectCatalog");
  if (!boundOfferingId()) return t("offering.selectOffering");
  return undefined;
});

async function onSave(request: UpdateOfferingRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundOfferingId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("offerings.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  applyOfferingSelection(null);
}

function onSelectPlan(plan: ServicePlan) {
  const offeringId = boundOfferingId();
  if (!offeringId || !offering.value) return;
  const detail = {
    ...plan,
    serviceId: offeringId,
    serviceName: offering.value.name,
  };
  const catalogs = workspace.selectedCatalog
    ? [{ ...workspace.selectedCatalog, offerings: [offering.value] }]
    : [];
  applyPlanSelection(detail, catalogs);
  focusPanel("plan");
}

async function onRemovePlan(planId: string) {
  const confirmed = await confirm({
    message: t("plans.deleteConfirm", { id: planId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await removePlan(planId);
  if (workspace.selectedPlanId === planId) {
    applyPlanSelection(null);
  }
}

function onExternalSelect() {
  if (isBoundTab.value) return;
  void load();
}

onMounted(() => {
  window.addEventListener(OFFERING_SELECTED_EVENT, onExternalSelect);
});
onUnmounted(() => {
  window.removeEventListener(OFFERING_SELECTED_EVENT, onExternalSelect);
});
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!offering"
      :loading="!!boundOfferingId() && loading && !offering"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <OfferingDetailForm
        v-if="offering"
        ref="formRef"
        :offering="offering"
        @save="onSave"
        @select-plan="onSelectPlan"
        @remove-plan="onRemovePlan"
      />
      <template #actions>
        <DeleteButton :label="$t('common.delete')" @click="onDelete" />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
