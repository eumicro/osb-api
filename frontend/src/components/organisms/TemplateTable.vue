<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { Template } from "../../models/template";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  templates: Template[];
  selectedTemplateId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [template: Template];
  remove: [templateId: string];
  openInTab: [template: Template];
}>();

const { t } = useI18n();

const columns = computed(() => [
  { header: t("common.name"), value: (item: Template) => item.name, primary: true },
  {
    header: t("templates.kind"),
    value: (item: Template) => t(`templates.kinds.${item.kind}`),
  },
  {
    header: t("common.status"),
    value: (item: Template) =>
      item.enabled ? t("templates.enabled") : t("templates.disabled"),
  },
]);
</script>

<template>
  <EntityCollection
    :items="templates"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(item) => item.id"
    :selected-key="selectedTemplateId"
    :empty-label="t('templates.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
