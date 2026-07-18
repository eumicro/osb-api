<script setup lang="ts">
import type { SystemInfo } from "../../models/systemInfo";
import StatusBadge from "../atoms/StatusBadge.vue";
import KeyValueList from "../molecules/KeyValueList.vue";

defineProps<{ info: SystemInfo }>();
</script>

<template>
  <section class="card">
    <h2>{{ $t("dashboard.system") }}</h2>
    <StatusBadge :label="`${info.status} · ${info.version}`" />
    <h3 class="section-title">{{ $t("dashboard.adapters") }}</h3>
    <KeyValueList :entries="info.adapters" />
  </section>
</template>
