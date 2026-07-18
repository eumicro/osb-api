<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useHttpClients } from "../../controllers/useHttpClients";
import type {
  CreateHttpClientInstanceRequest,
  HttpClientInstance,
} from "../../models/httpClient";
import { focusPanel, openHttpClientInNewTab } from "../../stores/workspaceLayout";
import { selectHttpClient, workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import HttpClientForm from "../organisms/HttpClientForm.vue";
import HttpClientTable from "../organisms/HttpClientTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  httpClients,
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
} = useHttpClients();
const { mode } = useCollectionViewMode("httpClients");
const createDialogOpen = ref(false);

function onSelect(client: HttpClientInstance) {
  selectHttpClient(client.id, client);
  focusPanel("httpClient");
}

function onOpenInTab(client: HttpClientInstance) {
  openHttpClientInNewTab(client);
}

async function onRemove(clientId: string) {
  const confirmed = await confirm({
    message: t("httpClients.deleteConfirm", { id: clientId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(clientId);
  if (workspace.selectedHttpClientId === clientId) {
    selectHttpClient(null);
  }
}

async function onCreate(request: CreateHttpClientInstanceRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    selectHttpClient(saved.id, saved);
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="httpClients"
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
        <CreateButton :label="$t('httpClients.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <HttpClientTable
          :http-clients="items"
          :selected-http-client-id="workspace.selectedHttpClientId"
          :mode="mode"
          scope="httpClients"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('httpClients.new')">
          <HttpClientForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
