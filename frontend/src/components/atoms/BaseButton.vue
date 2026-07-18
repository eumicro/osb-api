<script setup lang="ts">
import type { IconName } from "../../models/iconName";
import Icon from "./Icon.vue";

/** Atom: icon button; `label` is accessible name (aria-label + title). */
withDefaults(
  defineProps<{
    label: string;
    icon: IconName;
    variant?: "primary" | "secondary" | "danger";
    type?: "button" | "submit";
    disabled?: boolean;
  }>(),
  { variant: "primary", type: "button", disabled: false },
);
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    class="base-button"
    :class="variant"
    :aria-label="label"
    :title="label"
    v-bind="$attrs"
  >
    <Icon :name="icon" />
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  width: 2.15rem;
  height: 2.15rem;
  padding: 0;
  font: inherit;
  cursor: pointer;
  border: 1px solid transparent;
}

.base-button.primary {
  background: var(--accent);
  color: #fff;
}

.base-button.secondary {
  background: var(--surface);
  color: var(--text);
  border-color: var(--border);
}

.base-button.danger {
  background: var(--danger);
  color: #fff;
}

.base-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
