<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import { useGitClientDetail } from "../../controllers/useGitClientDetail";
import type { UpdateGitClientInstanceRequest } from "../../models/gitClient";
import type { GitClientPanelParams } from "../../stores/workspaceLayout";
import { selectGitClient, workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import GitClientDetailForm from "../organisms/GitClientDetailForm.vue";

interface DockviewComponentProps {
  params?: GitClientPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.gitClientId);

const boundGitClientId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.gitClientId ?? null)
    : workspace.selectedGitClientId;

const { gitClient, loading, error, save, remove } = useGitClientDetail(boundGitClientId, {
  syncWorkspace: !isBoundTab.value,
});

const meta = computed(() => {
  if (!gitClient.value) return undefined;
  return `${gitClient.value.authMethod} · ${gitClient.value.remoteUrl} · ${gitClient.value.defaultBranch}`;
});

const empty = computed(() =>
  !boundGitClientId() ? t("gitClient.selectGitClient") : undefined,
);

async function onSave(request: UpdateGitClientInstanceRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundGitClientId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("gitClients.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  selectGitClient(null);
}
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!gitClient"
      :loading="loading && !gitClient"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <GitClientDetailForm
        v-if="gitClient"
        ref="formRef"
        :git-client="gitClient"
        @save="onSave"
      />
      <template #actions>
        <DeleteButton :label="$t('common.delete')" @click="onDelete" />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
