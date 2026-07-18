<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import { useHttpClientDetail } from "../../controllers/useHttpClientDetail";
import type { UpdateHttpClientInstanceRequest } from "../../models/httpClient";
import type { HttpClientPanelParams } from "../../stores/workspaceLayout";
import { selectHttpClient, workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import HttpClientDetailForm from "../organisms/HttpClientDetailForm.vue";

interface DockviewComponentProps {
  params?: HttpClientPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.httpClientId);

const boundHttpClientId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.httpClientId ?? null)
    : workspace.selectedHttpClientId;

const { httpClient, loading, error, save, remove } = useHttpClientDetail(boundHttpClientId, {
  syncWorkspace: !isBoundTab.value,
});

const meta = computed(() => {
  if (!httpClient.value) return undefined;
  return `${httpClient.value.baseUrl} · ${httpClient.value.authType}`;
});

const empty = computed(() =>
  !boundHttpClientId() ? t("httpClient.selectHttpClient") : undefined,
);

async function onSave(request: UpdateHttpClientInstanceRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundHttpClientId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("httpClients.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  selectHttpClient(null);
}
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!httpClient"
      :loading="loading && !httpClient"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <HttpClientDetailForm
        v-if="httpClient"
        ref="formRef"
        :http-client="httpClient"
        @save="onSave"
      />
      <template #actions>
        <DeleteButton :label="$t('common.delete')" @click="onDelete" />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
