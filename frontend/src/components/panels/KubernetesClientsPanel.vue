<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useKubernetesClients } from "../../controllers/useKubernetesClients";
import type {
  CreateKubernetesClientInstanceRequest,
  KubernetesClientInstance,
} from "../../models/kubernetesClient";
import { focusPanel, openKubernetesClientInNewTab } from "../../stores/workspaceLayout";
import { selectKubernetesClient, workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import KubernetesClientForm from "../organisms/KubernetesClientForm.vue";
import KubernetesClientTable from "../organisms/KubernetesClientTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  kubernetesClients,
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
} = useKubernetesClients();
const { mode } = useCollectionViewMode("kubernetesClients");
const createDialogOpen = ref(false);

function onSelect(client: KubernetesClientInstance) {
  selectKubernetesClient(client.id, client);
  focusPanel("kubernetesClient");
}

function onOpenInTab(client: KubernetesClientInstance) {
  openKubernetesClientInNewTab(client);
}

async function onRemove(clientId: string) {
  const confirmed = await confirm({
    message: t("kubernetesClients.deleteConfirm", { id: clientId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(clientId);
  if (workspace.selectedKubernetesClientId === clientId) {
    selectKubernetesClient(null);
  }
}

async function onCreate(request: CreateKubernetesClientInstanceRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    selectKubernetesClient(saved.id, saved);
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="kubernetesClients"
      mode="server"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :page-count="pageCount"
      :error="error"
      :loading="loading"
      :hint="$t('kubernetesClients.hint')"
      @update:page="setPage"
      @update:page-size="setPageSize"
    >
      <template #toolbar>
        <CreateButton :label="$t('kubernetesClients.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <KubernetesClientTable
          :kubernetes-clients="items"
          :selected-kubernetes-client-id="workspace.selectedKubernetesClientId"
          :mode="mode"
          scope="kubernetesClients"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('kubernetesClients.new')">
          <KubernetesClientForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
