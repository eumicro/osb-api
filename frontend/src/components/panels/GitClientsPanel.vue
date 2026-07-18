<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useGitClients } from "../../controllers/useGitClients";
import type { CreateGitClientInstanceRequest, GitClientInstance } from "../../models/gitClient";
import { focusPanel, openGitClientInNewTab } from "../../stores/workspaceLayout";
import { selectGitClient, workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import GitClientForm from "../organisms/GitClientForm.vue";
import GitClientTable from "../organisms/GitClientTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  gitClients,
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
} = useGitClients();
const { mode } = useCollectionViewMode("gitClients");
const createDialogOpen = ref(false);

function onSelect(client: GitClientInstance) {
  selectGitClient(client.id, client);
  focusPanel("gitClient");
}

function onOpenInTab(client: GitClientInstance) {
  openGitClientInNewTab(client);
}

async function onRemove(clientId: string) {
  const confirmed = await confirm({
    message: t("gitClients.deleteConfirm", { id: clientId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(clientId);
  if (workspace.selectedGitClientId === clientId) {
    selectGitClient(null);
  }
}

async function onCreate(request: CreateGitClientInstanceRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    selectGitClient(saved.id, saved);
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="gitClients"
      mode="server"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :page-count="pageCount"
      :error="error"
      :loading="loading"
      :hint="$t('gitClients.hint')"
      @update:page="setPage"
      @update:page-size="setPageSize"
    >
      <template #toolbar>
        <CreateButton :label="$t('gitClients.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <GitClientTable
          :git-clients="items"
          :selected-git-client-id="workspace.selectedGitClientId"
          :mode="mode"
          scope="gitClients"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('gitClients.new')">
          <GitClientForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
