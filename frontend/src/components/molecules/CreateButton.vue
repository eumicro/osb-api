<script setup lang="ts">
import BaseButton from "../atoms/BaseButton.vue";

defineProps<{ label: string; disabled?: boolean; type?: "button" | "submit" }>();
defineEmits<{ click: [] }>();
</script>

<template>
  <BaseButton
    icon="plus"
    :label="label"
    :type="type ?? 'button'"
    :disabled="disabled"
    @click="$emit('click')"
  />
</template>
