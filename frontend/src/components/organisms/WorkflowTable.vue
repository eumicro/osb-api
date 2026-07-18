<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { WorkflowDefinition } from "../../models/workflow";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  workflows: WorkflowDefinition[];
  selectedWorkflowId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [workflow: WorkflowDefinition];
  remove: [workflowId: string];
  openInTab: [workflow: WorkflowDefinition];
}>();

const { t } = useI18n();

const columns = computed(() => [
  {
    header: t("common.name"),
    value: (w: WorkflowDefinition) => w.name,
    primary: true,
  },
  { header: t("workflows.kind"), value: (w: WorkflowDefinition) => w.kind },
  {
    header: t("workflows.webhook"),
    value: (w: WorkflowDefinition) => w.n8nWebhookPath,
  },
  {
    header: t("workflows.clients"),
    value: (w: WorkflowDefinition) => w.clients.join(", ") || "—",
  },
  {
    header: t("common.status"),
    value: (w: WorkflowDefinition) =>
      w.enabled ? t("workflows.enabled") : t("workflows.disabled"),
  },
]);
</script>

<template>
  <EntityCollection
    :items="workflows"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(w) => w.id"
    :selected-key="selectedWorkflowId"
    :empty-label="t('workflows.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
