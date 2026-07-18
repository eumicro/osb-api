<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCatalogs } from "../../controllers/useCatalogs";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useInstances } from "../../controllers/useInstances";
import { usePlatforms } from "../../controllers/usePlatforms";
import type { ProvisionInstanceRequest, ServiceInstance } from "../../models/serviceInstance";
import { applyInstanceSelection } from "../../stores/selectionSync";
import { focusPanel, openInstanceInNewTab } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import InstanceForm from "../organisms/InstanceForm.vue";
import InstanceTable from "../organisms/InstanceTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  instances,
  page,
  pageSize,
  total,
  pageCount,
  selectedInstanceId,
  loading,
  error,
  load,
  setPage,
  setPageSize,
  create,
  remove,
} = useInstances({ paged: true });
const { catalogs, load: loadCatalogs } = useCatalogs();
const { platforms, load: loadPlatforms } = usePlatforms();
const { mode } = useCollectionViewMode("instances");
const createDialogOpen = ref(false);

onMounted(() => {
  void loadCatalogs();
  void loadPlatforms();
});

function onSelect(instance: ServiceInstance) {
  applyInstanceSelection(instance, catalogs.value, platforms.value);
  focusPanel("instance");
}

function onOpenInTab(instance: ServiceInstance) {
  openInstanceInNewTab(instance);
}

async function onRemove(instanceId: string) {
  const confirmed = await confirm({
    message: t("instances.deleteConfirm", { id: instanceId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(instanceId);
  // Selection is cleared by remove() only after deprovision last-operation succeeded.
  if (workspace.selectedInstanceId === instanceId) {
    applyInstanceSelection(null, catalogs.value, platforms.value);
  }
}

async function onCreate(request: ProvisionInstanceRequest) {
  createDialogOpen.value = false;
  const saved = await create(request);
  if (saved) {
    applyInstanceSelection(saved, catalogs.value, platforms.value);
    focusPanel("instance");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="instances"
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
        <CreateButton :label="$t('instances.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <InstanceTable
          :instances="items"
          :selected-instance-id="selectedInstanceId"
          :mode="mode"
          scope="instances"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('instances.new')" size="lg">
          <InstanceForm
            :catalogs="catalogs"
            :platforms="platforms"
            :preferred-catalog-id="workspace.selectedCatalogId"
            :preferred-offering-id="workspace.selectedOfferingId"
            @save="onCreate"
          />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
