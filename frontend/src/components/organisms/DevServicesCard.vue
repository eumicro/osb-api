<script setup lang="ts">
import KeyValueList from "../molecules/KeyValueList.vue";

defineProps<{
  services: Record<string, string>;
}>();
</script>

<template>
  <section class="card">
    <h2>{{ $t("dashboard.devServices") }}</h2>
    <KeyValueList :entries="services" />
  </section>
</template>
