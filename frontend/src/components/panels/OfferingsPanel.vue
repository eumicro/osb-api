<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useOfferings } from "../../controllers/useOfferings";
import type { AdminCatalog, ServiceOffering } from "../../models/catalog";
import type { CreateOfferingRequest } from "../../models/catalogAdmin";
import { applyOfferingSelection } from "../../stores/selectionSync";
import { focusPanel, openOfferingInNewTab } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import OfferingForm from "../organisms/OfferingForm.vue";
import OfferingTable from "../organisms/OfferingTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  offerings,
  page,
  pageSize,
  total,
  pageCount,
  selectedOfferingId,
  catalogId,
  loading,
  error,
  load,
  setPage,
  setPageSize,
  create,
  remove,
} = useOfferings();
const { mode } = useCollectionViewMode("offerings");
const createDialogOpen = ref(false);

const empty = computed(() =>
  !catalogId.value ? t("offerings.selectCatalog") : undefined,
);

const hint = computed(() =>
  catalogId.value ? t("offerings.catalogPath", { id: catalogId.value }) : undefined,
);

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

function onSelect(offering: ServiceOffering) {
  applyOfferingSelection(offering, catalogsForSync());
  focusPanel("offering");
}

function onOpenInTab(offering: ServiceOffering) {
  if (!catalogId.value) return;
  openOfferingInNewTab(catalogId.value, offering);
}

async function onRemove(offeringId: string) {
  const confirmed = await confirm({
    message: t("offerings.deleteConfirm", { id: offeringId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(offeringId);
  if (workspace.selectedOfferingId === offeringId) {
    applyOfferingSelection(null);
  }
}

async function onCreate(request: CreateOfferingRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    applyOfferingSelection(saved, catalogsForSync());
    focusPanel("offering");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="offerings"
      mode="server"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :page-count="pageCount"
      :error="error"
      :loading="loading"
      :ready="!!catalogId"
      :empty="empty"
      :hint="hint"
      @update:page="setPage"
      @update:page-size="setPageSize"
    >
      <template #toolbar>
        <CreateButton :label="$t('offerings.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <OfferingTable
          :offerings="items"
          :selected-offering-id="selectedOfferingId"
          :mode="mode"
          scope="offerings"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('offerings.new')" size="lg">
          <OfferingForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
