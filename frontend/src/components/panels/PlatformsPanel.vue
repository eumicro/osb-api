<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCatalogs } from "../../controllers/useCatalogs";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { usePlatforms } from "../../controllers/usePlatforms";
import type { CreatePlatformClientRequest, PlatformClient } from "../../models/platformClient";
import { applyPlatformSelection } from "../../stores/selectionSync";
import { focusPanel, openPlatformInNewTab } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import PlatformForm from "../organisms/PlatformForm.vue";
import PlatformTable from "../organisms/PlatformTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  platforms,
  page,
  pageSize,
  total,
  pageCount,
  loading,
  error,
  load,
  setPage,
  setPageSize,
  create,
  remove,
} = usePlatforms({ paged: true });
const { catalogs, load: loadCatalogs } = useCatalogs();
const { mode } = useCollectionViewMode("platforms");
const createDialogOpen = ref(false);

onMounted(() => {
  void loadCatalogs();
});

function onSelect(platform: PlatformClient) {
  applyPlatformSelection(platform, catalogs.value);
  focusPanel("platform");
}

function onOpenInTab(platform: PlatformClient) {
  openPlatformInNewTab(platform);
}

async function onRemove(platformId: string) {
  const confirmed = await confirm({
    message: t("platforms.deleteConfirm", { id: platformId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(platformId);
  if (workspace.selectedPlatformClientId === platformId) {
    applyPlatformSelection(null, catalogs.value);
  }
}

async function onCreate(request: CreatePlatformClientRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    applyPlatformSelection(saved, catalogs.value);
    focusPanel("platform");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="platforms"
      mode="server"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :page-count="pageCount"
      :error="error"
      :loading="loading"
      :hint="$t('platforms.hint')"
      @update:page="setPage"
      @update:page-size="setPageSize"
    >
      <template #toolbar>
        <CreateButton :label="$t('platforms.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <PlatformTable
          :platforms="items"
          :selected-platform-client-id="workspace.selectedPlatformClientId"
          :mode="mode"
          scope="platforms"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('platforms.new')">
          <PlatformForm :catalogs="catalogs" @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
