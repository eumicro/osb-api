<script setup lang="ts">
defineProps<{ message: string }>();
</script>

<template>
  <p v-if="message" class="error">{{ $t("common.error") }}: {{ message }}</p>
</template>
