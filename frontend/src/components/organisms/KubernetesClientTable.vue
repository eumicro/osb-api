<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { KubernetesClientInstance } from "../../models/kubernetesClient";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  kubernetesClients: KubernetesClientInstance[];
  selectedKubernetesClientId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [client: KubernetesClientInstance];
  remove: [clientId: string];
  openInTab: [client: KubernetesClientInstance];
}>();

const { t } = useI18n();

const columns = computed(() => [
  {
    header: t("common.name"),
    value: (c: KubernetesClientInstance) => c.name,
    primary: true,
  },
  {
    header: t("kubernetesClients.apiServerUrl"),
    value: (c: KubernetesClientInstance) => c.apiServerUrl,
  },
  {
    header: t("kubernetesClients.defaultNamespace"),
    value: (c: KubernetesClientInstance) => c.defaultNamespace,
  },
  {
    header: t("common.status"),
    value: (c: KubernetesClientInstance) =>
      c.enabled ? t("kubernetesClients.enabled") : t("kubernetesClients.disabled"),
  },
]);
</script>

<template>
  <EntityCollection
    :items="kubernetesClients"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(c) => c.id"
    :selected-key="selectedKubernetesClientId"
    :empty-label="t('kubernetesClients.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
