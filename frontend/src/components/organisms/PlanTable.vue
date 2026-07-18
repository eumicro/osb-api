<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { PlanRow } from "../../models/plan";
import EntityCollection from "./EntityCollection.vue";

const props = defineProps<{
  plans: PlanRow[];
  selectedPlanId: string | null;
  selectedPlanOfferingId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [plan: PlanRow];
  remove: [plan: PlanRow];
  openInTab: [plan: PlanRow];
}>();

const { t } = useI18n();

const columns = computed(() => [
  { header: t("plans.offering"), value: (p: PlanRow) => p.serviceName },
  { header: t("common.name"), value: (p: PlanRow) => p.name, primary: true },
  { header: t("common.description"), value: (p: PlanRow) => p.description },
  {
    header: t("common.free"),
    value: (p: PlanRow) => (p.free ? t("common.yes") : t("common.no")),
  },
]);

const selectedKey = computed(() => {
  if (!props.selectedPlanId || !props.selectedPlanOfferingId) return null;
  return `${props.selectedPlanOfferingId}:${props.selectedPlanId}`;
});
</script>

<template>
  <EntityCollection
    :items="plans"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(p) => `${p.serviceId}:${p.id}`"
    :selected-key="selectedKey"
    :empty-label="t('plans.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
