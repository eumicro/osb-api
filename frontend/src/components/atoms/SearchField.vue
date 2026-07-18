<script setup lang="ts">
import Icon from "./Icon.vue";
import BaseInput from "./BaseInput.vue";

/** Atom: kompaktes Suchfeld (BaseInput + Icon) mit v-model. */
withDefaults(
  defineProps<{
    modelValue: string;
    label?: string;
    placeholder?: string;
  }>(),
  {
    label: undefined,
    placeholder: undefined,
  },
);

defineEmits<{ "update:modelValue": [value: string] }>();
</script>

<template>
  <label class="search-field">
    <span class="search-field__label">{{ label ?? $t("common.search") }}</span>
    <span class="search-field__control">
      <Icon name="search" class="search-field__icon" />
      <BaseInput
        type="search"
        :model-value="modelValue"
        :placeholder="placeholder ?? $t('common.searchPlaceholder')"
        :aria-label="label ?? $t('common.search')"
        autocomplete="off"
        @update:model-value="$emit('update:modelValue', $event)"
      />
    </span>
  </label>
</template>

<style scoped>
.search-field {
  display: inline-flex;
  flex-direction: column;
  min-width: 12rem;
  max-width: 18rem;
  flex: 1 1 12rem;
}

.search-field__label {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.search-field__control {
  position: relative;
  display: block;
}

.search-field__icon {
  position: absolute;
  left: 0.55rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--muted);
  pointer-events: none;
  z-index: 1;
}

.search-field__control :deep(.base-input) {
  padding-left: 2rem;
}
</style>
