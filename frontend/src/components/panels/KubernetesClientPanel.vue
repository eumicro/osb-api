<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import { useKubernetesClientDetail } from "../../controllers/useKubernetesClientDetail";
import type { UpdateKubernetesClientInstanceRequest } from "../../models/kubernetesClient";
import type { KubernetesClientPanelParams } from "../../stores/workspaceLayout";
import { selectKubernetesClient, workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import KubernetesClientDetailForm from "../organisms/KubernetesClientDetailForm.vue";

interface DockviewComponentProps {
  params?: KubernetesClientPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.kubernetesClientId);

const boundKubernetesClientId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.kubernetesClientId ?? null)
    : workspace.selectedKubernetesClientId;

const { kubernetesClient, loading, error, save, remove } = useKubernetesClientDetail(
  boundKubernetesClientId,
  {
    syncWorkspace: !isBoundTab.value,
  },
);

const meta = computed(() => {
  if (!kubernetesClient.value) return undefined;
  return `${kubernetesClient.value.apiServerUrl} · ${kubernetesClient.value.defaultNamespace} · ${kubernetesClient.value.authType}`;
});

const empty = computed(() =>
  !boundKubernetesClientId() ? t("kubernetesClient.selectKubernetesClient") : undefined,
);

async function onSave(request: UpdateKubernetesClientInstanceRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundKubernetesClientId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("kubernetesClients.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  selectKubernetesClient(null);
}
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!kubernetesClient"
      :loading="loading && !kubernetesClient"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <KubernetesClientDetailForm
        v-if="kubernetesClient"
        ref="formRef"
        :kubernetes-client="kubernetesClient"
        @save="onSave"
      />
      <template #actions>
        <DeleteButton :label="$t('common.delete')" @click="onDelete" />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
