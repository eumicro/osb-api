<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { GitClientInstance } from "../../models/gitClient";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  gitClients: GitClientInstance[];
  selectedGitClientId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [client: GitClientInstance];
  remove: [clientId: string];
  openInTab: [client: GitClientInstance];
}>();

const { t } = useI18n();

const columns = computed(() => [
  {
    header: t("common.name"),
    value: (c: GitClientInstance) => c.name,
    primary: true,
  },
  {
    header: t("gitClients.authMethod"),
    value: (c: GitClientInstance) => t(`gitClients.authMethods.${c.authMethod}`),
  },
  { header: t("gitClients.remoteUrl"), value: (c: GitClientInstance) => c.remoteUrl },
  {
    header: t("common.status"),
    value: (c: GitClientInstance) =>
      c.enabled ? t("gitClients.enabled") : t("gitClients.disabled"),
  },
]);
</script>

<template>
  <EntityCollection
    :items="gitClients"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(c) => c.id"
    :selected-key="selectedGitClientId"
    :empty-label="t('gitClients.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
