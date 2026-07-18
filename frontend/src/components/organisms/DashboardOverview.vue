<script setup lang="ts">
import type { Catalog } from "../../models/catalog";
import type { SystemInfo } from "../../models/systemInfo";
import CatalogCard from "./CatalogCard.vue";
import DevServicesCard from "./DevServicesCard.vue";
import SystemInfoCard from "./SystemInfoCard.vue";

defineProps<{
  info: SystemInfo | null;
  catalog: Catalog | null;
  pluginCount: number;
}>();
</script>

<template>
  <div class="dashboard-overview">
    <p class="muted">{{ $t("plugins.loaded", { count: pluginCount }) }}</p>
    <div class="grid">
      <SystemInfoCard v-if="info" :info="info" />
      <DevServicesCard v-if="info" :services="info.devServices" />
      <CatalogCard v-if="catalog" :catalog="catalog" />
    </div>
  </div>
</template>

<style scoped>
.dashboard-overview {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.grid {
  display: grid;
  gap: 1rem;
}
</style>
