<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { ServiceInstance } from "../../models/serviceInstance";
import { isInstanceBusy } from "../../models/serviceInstance";
import InstanceStatusIcon from "../atoms/InstanceStatusIcon.vue";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  instances: ServiceInstance[];
  selectedInstanceId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [instance: ServiceInstance];
  remove: [instanceId: string];
  openInTab: [instance: ServiceInstance];
}>();

const { t } = useI18n();

const columns = computed(() => [
  { header: t("instances.id"), value: (i: ServiceInstance) => i.id, primary: true },
  { header: t("instances.service"), value: (i: ServiceInstance) => i.serviceId },
  { header: t("instances.plan"), value: (i: ServiceInstance) => i.planId },
  {
    header: t("instances.platform"),
    value: (i: ServiceInstance) => i.platformClientId ?? "—",
  },
  {
    header: t("common.status"),
    value: (i: ServiceInstance) => i.state,
    cell: {
      component: InstanceStatusIcon,
      props: (i: ServiceInstance) => ({ state: i.state }),
    },
  },
  {
    header: t("instances.lastOperation"),
    value: (i: ServiceInstance) => {
      if (!i.lastOperationKind && !i.lastOperationState) return "—";
      const kind = i.lastOperationKind ?? "—";
      const state = i.lastOperationState ?? "—";
      return `${kind}: ${state}`;
    },
  },
]);

function canRemove(instance: ServiceInstance) {
  return !isInstanceBusy(instance);
}
</script>

<template>
  <EntityCollection
    :items="instances"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(i) => i.id"
    :selected-key="selectedInstanceId"
    :empty-label="t('instances.none')"
    :can-remove="canRemove"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
