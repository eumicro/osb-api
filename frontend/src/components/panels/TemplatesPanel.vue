<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollectionViewMode } from "../../controllers/useCollectionViewMode";
import { useConfirm } from "../../controllers/useConfirm";
import { useTemplates } from "../../controllers/useTemplates";
import type { CreateTemplateRequest, Template } from "../../models/template";
import { focusPanel, openTemplateInNewTab } from "../../stores/workspaceLayout";
import { selectTemplate, workspace } from "../../stores/workspace";
import CollectionViewToggle from "../molecules/CollectionViewToggle.vue";
import CreateButton from "../molecules/CreateButton.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import AppDialog from "../organisms/AppDialog.vue";
import BaseListLayout from "../organisms/BaseListLayout.vue";
import TemplateForm from "../organisms/TemplateForm.vue";
import TemplateTable from "../organisms/TemplateTable.vue";

const { t } = useI18n();
const { confirm } = useConfirm();
const {
  templates,
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
} = useTemplates();
const { mode } = useCollectionViewMode("templates");
const createDialogOpen = ref(false);

function onSelect(template: Template) {
  selectTemplate(template.id, template);
  focusPanel("template");
}

function onOpenInTab(template: Template) {
  openTemplateInNewTab(template);
}

async function onRemove(templateId: string) {
  const confirmed = await confirm({
    message: t("templates.deleteConfirm", { id: templateId }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove(templateId);
  if (workspace.selectedTemplateId === templateId) {
    selectTemplate(null);
  }
}

async function onCreate(request: CreateTemplateRequest) {
  const saved = await create(request);
  if (saved) {
    createDialogOpen.value = false;
    selectTemplate(saved.id, saved);
    focusPanel("template");
  }
}
</script>

<template>
  <div class="panel-body">
    <BaseListLayout
      :items="templates"
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
        <CreateButton :label="$t('templates.new')" @click="createDialogOpen = true" />
      </template>
      <template #toolbar-end>
        <CollectionViewToggle v-model="mode" />
        <ReloadButton :loading="loading" @click="load" />
      </template>
      <template #default="{ items }">
        <TemplateTable
          :templates="items"
          :selected-template-id="workspace.selectedTemplateId"
          :mode="mode"
          scope="templates"
          @select="onSelect"
          @remove="onRemove"
          @open-in-tab="onOpenInTab"
        />
      </template>
      <template #dialog>
        <AppDialog v-model:open="createDialogOpen" :title="$t('templates.new')" size="lg">
          <TemplateForm @save="onCreate" />
        </AppDialog>
      </template>
    </BaseListLayout>
  </div>
</template>
