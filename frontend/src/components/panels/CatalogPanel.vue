<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCatalogDetail } from "../../controllers/useCatalogDetail";
import { useCatalogRelations } from "../../controllers/useCatalogRelations";
import { useConfirm } from "../../controllers/useConfirm";
import type { CatalogSavePayload } from "../../models/catalogAdmin";
import { CATALOG_SELECTED_EVENT } from "../../stores/catalogSelection";
import { applyCatalogSelection } from "../../stores/selectionSync";
import type { CatalogPanelParams } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import CatalogDetailForm from "../organisms/CatalogDetailForm.vue";

interface DockviewComponentProps {
  params?: CatalogPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.catalogId);

const boundCatalogId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.catalogId ?? null)
    : workspace.selectedCatalogId;

const { catalog, loading, error, save, remove } = useCatalogDetail(boundCatalogId, {
  syncWorkspace: !isBoundTab.value,
});

const { platforms, catalogs, relationError, load: loadRelations, saveWithAssignments } =
  useCatalogRelations(boundCatalogId, {
    saveCatalog: save,
    unassignNeedsFallbackMessage: () => t("catalogs.unassignNeedsFallback"),
    genericErrorMessage: () => t("common.error"),
  });

const meta = computed(() => {
  if (!catalog.value) return undefined;
  return `${catalog.value.id} · ${catalog.value.offerings.length} ${t("catalogs.offerings")}`;
});

const empty = computed(() =>
  !boundCatalogId() ? t("catalog.selectCatalog") : undefined,
);

async function onSave(payload: CatalogSavePayload) {
  await saveWithAssignments(payload);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundCatalogId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("catalogs.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  applyCatalogSelection(null);
}

function onExternalSelect() {
  if (isBoundTab.value) return;
  void loadRelations();
}

onMounted(() => {
  window.addEventListener(CATALOG_SELECTED_EVENT, onExternalSelect);
});
onUnmounted(() => {
  window.removeEventListener(CATALOG_SELECTED_EVENT, onExternalSelect);
});
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!catalog"
      :loading="loading && !catalog"
      :empty="empty"
      :meta="meta"
      :error="error || relationError"
      @submit="onSubmit"
    >
      <CatalogDetailForm
        v-if="catalog"
        ref="formRef"
        :catalog="catalog"
        :platforms="platforms"
        :catalogs="catalogs"
        @save="onSave"
      />
      <template #actions>
        <DeleteButton :label="$t('common.delete')" @click="onDelete" />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
