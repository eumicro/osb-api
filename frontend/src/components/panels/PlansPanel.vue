<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { usePlans } from "../../controllers/usePlans";
import type { AdminCatalog } from "../../models/catalog";
import type { CreatePlanRequest } from "../../models/catalogAdmin";
import type { PlanRow } from "../../models/plan";
import { applyPlanSelection } from "../../stores/selectionSync";
import { focusPanel, openPlanInNewTab } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import PlanForm from "../organisms/PlanForm.vue";
import PlanTable from "../organisms/PlanTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  plans,
  offerings,
  selectedOfferingId,
  selectedPlanId,
  selectedPlanOfferingId,
  catalogId,
  loading,
  error,
  load,
  create,
  remove,
} = usePlans();
const { mode } = useCollectionViewMode("plans");
const createDialogOpen = ref(false);

const empty = computed(() =>
  !catalogId.value ? t("offerings.selectCatalog") : undefined,
);

const hint = computed(() =>
  catalogId.value ? t("plans.catalogPath", { id: catalogId.value }) : undefined,
);

const filterHint = computed(() => {
  if (!catalogId.value) return undefined;
  if (selectedOfferingId.value) {
    return t("plans.filteredByOffering", { id: selectedOfferingId.value });
  }
  return t("plans.allOfferings");
});

function catalogsForSync(): AdminCatalog[] {
  if (!workspace.selectedCatalogId) return [];
  return [
    {
      id: workspace.selectedCatalogId,
      name: workspace.selectedCatalog?.name ?? workspace.selectedCatalogId,
      description: workspace.selectedCatalog?.description ?? "",
      offerings: offerings.value,
    },
  ];
}

function onSelect(plan: PlanRow) {
  applyPlanSelection(plan, catalogsForSync());
  focusPanel("plan");
}

function onOpenInTab(plan: PlanRow) {
  if (!catalogId.value) return;
  openPlanInNewTab(catalogId.value, plan);
}

async function onRemove(plan: PlanRow) {
  const confirmed = await confirm({
    message: t("plans.deleteConfirm", { id: plan.id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(plan.serviceId, plan.id);
  if (
    workspace.selectedPlanId === plan.id
    && workspace.selectedPlanOfferingId === plan.serviceId
  ) {
    applyPlanSelection(null);
  }
}

async function onCreate(request: CreatePlanRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    applyPlanSelection(saved, catalogsForSync());
    focusPanel("plan");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="plans"
      :error="error"
      :loading="loading"
      :ready="!!catalogId"
      :empty="empty"
      :hint="hint"
    >
      <template #toolbar>
        <CreateButton
          :label="$t('plans.new')"
          :disabled="offerings.length === 0"
          @click="createDialogOpen = true"
        />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #before>
        <p v-if="filterHint" class="muted">{{ filterHint }}</p>
      </template>
      <template #default="{ items }">
        <PlanTable
          :plans="items"
          :selected-plan-id="selectedPlanId"
          :selected-plan-offering-id="selectedPlanOfferingId"
          :mode="mode"
          scope="plans"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('plans.new')" size="lg">
          <PlanForm
            :offerings="offerings"
            :preferred-offering-id="selectedOfferingId"
            @save="onCreate"
          />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
