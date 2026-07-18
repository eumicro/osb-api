<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCatalogs } from "../../controllers/useCatalogs";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import type { AdminCatalog } from "../../models/catalog";
import type { CreateCatalogRequest } from "../../models/catalogAdmin";
import { applyCatalogSelection } from "../../stores/selectionSync";
import { focusPanel, openCatalogInNewTab } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import CatalogForm from "../organisms/CatalogForm.vue";
import CatalogTable from "../organisms/CatalogTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const { catalogs, page, pageSize, total, pageCount, loading, error, load, setPage, setPageSize, create, remove } =
  useCatalogs({ paged: true });
const { mode } = useCollectionViewMode("catalogs");
const createDialogOpen = ref(false);

function onSelect(catalog: AdminCatalog) {
  applyCatalogSelection(catalog);
  focusPanel("catalog");
}

function onOpenInTab(catalog: AdminCatalog) {
  openCatalogInNewTab(catalog);
}

async function onRemove(catalogId: string) {
  const confirmed = await confirm({
    message: t("catalogs.deleteConfirm", { id: catalogId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(catalogId);
  if (workspace.selectedCatalogId === catalogId) {
    applyCatalogSelection(null);
  }
}

async function onCreate(request: CreateCatalogRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    applyCatalogSelection(saved);
    focusPanel("catalog");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="catalogs"
      mode="server"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :page-count="pageCount"
      :error="error"
      :loading="loading"
      @update:page="setPage"
      @update:page-size="setPageSize"
    >
      <template #toolbar>
        <CreateButton :label="$t('catalogs.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <CatalogTable
          :catalogs="items"
          :selected-catalog-id="workspace.selectedCatalogId"
          :mode="mode"
          scope="catalogs"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('catalogs.new')">
          <CatalogForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
