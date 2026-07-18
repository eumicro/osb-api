<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCatalogs } from "../../controllers/useCatalogs";
import { useConfirm } from "../../controllers/useConfirm";
import { useInstanceDetail } from "../../controllers/useInstanceDetail";
import { usePlatforms } from "../../controllers/usePlatforms";
import type { UpdateInstanceRequest } from "../../models/serviceInstance";
import { isInstanceBusy } from "../../models/serviceInstance";
import { INSTANCE_SELECTED_EVENT } from "../../stores/catalogSelection";
import { applyInstanceSelection } from "../../stores/selectionSync";
import type { InstancePanelParams } from "../../stores/workspaceLayout";
import { workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import InstanceDetailForm from "../organisms/InstanceDetailForm.vue";

interface DockviewComponentProps {
  params?: InstancePanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const { platforms, load: loadPlatforms } = usePlatforms();
const { catalogs, load: loadCatalogs } = useCatalogs();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.instanceId);

const boundInstanceId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.instanceId ?? null)
    : workspace.selectedInstanceId;

const { instance, loading, error, load, save, remove } = useInstanceDetail(boundInstanceId, {
  syncWorkspace: !isBoundTab.value,
});

const meta = computed(() => {
  if (!instance.value) return undefined;
  return `${instance.value.serviceId} / ${instance.value.planId} · ${instance.value.state}`;
});

const empty = computed(() =>
  !boundInstanceId() ? t("instance.selectInstance") : undefined,
);

const deleteDisabled = computed(
  () => !instance.value || isInstanceBusy(instance.value),
);

onMounted(() => {
  void loadPlatforms();
  void loadCatalogs();
  window.addEventListener(INSTANCE_SELECTED_EVENT, onExternalSelect);
});
onUnmounted(() => {
  window.removeEventListener(INSTANCE_SELECTED_EVENT, onExternalSelect);
});

async function onSave(request: UpdateInstanceRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundInstanceId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("instances.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  const ok = await remove();
  if (!ok) return;
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  applyInstanceSelection(null, catalogs.value, platforms.value);
}

function onExternalSelect() {
  if (isBoundTab.value) return;
  void load();
}
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!instance"
      :loading="loading && !instance"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <InstanceDetailForm
        v-if="instance"
        ref="formRef"
        :instance="instance"
        :platforms="platforms"
        :catalogs="catalogs"
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
