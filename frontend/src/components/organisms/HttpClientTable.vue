<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { HttpClientInstance } from "../../models/httpClient";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  httpClients: HttpClientInstance[];
  selectedHttpClientId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [client: HttpClientInstance];
  remove: [clientId: string];
  openInTab: [client: HttpClientInstance];
}>();

const { t } = useI18n();

const columns = computed(() => [
  {
    header: t("common.name"),
    value: (c: HttpClientInstance) => c.name,
    primary: true,
  },
  { header: t("httpClients.baseUrl"), value: (c: HttpClientInstance) => c.baseUrl },
  {
    header: t("httpClients.authType"),
    value: (c: HttpClientInstance) => t(`httpClients.authTypes.${c.authType}`),
  },
  {
    header: t("common.status"),
    value: (c: HttpClientInstance) =>
      c.enabled ? t("httpClients.enabled") : t("httpClients.disabled"),
  },
]);
</script>

<template>
  <EntityCollection
    :items="httpClients"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(c) => c.id"
    :selected-key="selectedHttpClientId"
    :empty-label="t('httpClients.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
