<script setup lang="ts">
import { computed } from "vue";
import type { UiTheme } from "../../theme";
import BaseButton from "../atoms/BaseButton.vue";

/** Molecule: light/dark appearance toggle (sun in dark, moon in light). */
const props = defineProps<{ theme: string }>();
const emit = defineEmits<{ change: [theme: UiTheme] }>();

const nextTheme = computed<UiTheme>(() => (props.theme === "dark" ? "light" : "dark"));
const themeIcon = computed(() => (props.theme === "dark" ? "sun" : "moon"));

function toggle() {
  emit("change", nextTheme.value);
}
</script>

<template>
  <span class="theme-toggle" :class="theme">
    <BaseButton
      :icon="themeIcon"
      :label="$t(`theme.${nextTheme}`)"
      variant="secondary"
      :aria-pressed="theme === 'dark'"
      @click="toggle"
    />
  </span>
</template>

<style scoped>
.theme-toggle.dark :deep(.base-button) {
  color: #f5c518;
}

.theme-toggle.light :deep(.base-button) {
  color: #3b82f6;
}
</style>
