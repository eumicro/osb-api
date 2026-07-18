<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useWorkflows } from "../../controllers/useWorkflows";
import type { CreateWorkflowRequest, WorkflowDefinition } from "../../models/workflow";
import { notifyWorkflowPanelRefresh } from "../../stores/catalogSelection";
import { focusPanel, openWorkflowInNewTab } from "../../stores/workspaceLayout";
import { selectWorkflow, workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import WorkflowForm from "../organisms/WorkflowForm.vue";
import WorkflowTable from "../organisms/WorkflowTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  workflows,
  page,
  pageSize,
  total,
  pageCount,
  selectedWorkflowId,
  loading,
  error,
  load,
  setPage,
  setPageSize,
  select,
  create,
  remove,
} = useWorkflows();
const { mode } = useCollectionViewMode("workflows");
const createDialogOpen = ref(false);

function onSelect(workflow: WorkflowDefinition) {
  select(workflow);
  notifyWorkflowPanelRefresh(workflow);
  focusPanel("workflow");
}

function onOpenInTab(workflow: WorkflowDefinition) {
  openWorkflowInNewTab(workflow);
}

async function onRemove(workflowId: string) {
  const confirmed = await confirm({
    message: t("workflows.deleteConfirm", { id: workflowId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(workflowId);
  if (workspace.selectedWorkflowId === workflowId) {
    selectWorkflow(null);
    notifyWorkflowPanelRefresh(null);
  }
}

async function onCreate(request: CreateWorkflowRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    notifyWorkflowPanelRefresh(saved);
    focusPanel("workflow");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="workflows"
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
        <CreateButton :label="$t('workflows.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <WorkflowTable
          :workflows="items"
          :selected-workflow-id="selectedWorkflowId"
          :mode="mode"
          scope="workflows"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('workflows.new')" size="lg">
          <WorkflowForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
