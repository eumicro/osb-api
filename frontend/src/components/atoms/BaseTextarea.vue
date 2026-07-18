<script setup lang="ts">
/** Atom: mehrzeiliges Textfeld mit v-model. */
withDefaults(
  defineProps<{
    modelValue: string;
    rows?: number;
  }>(),
  { rows: 3 },
);
defineEmits<{ "update:modelValue": [value: string] }>();
</script>

<template>
  <textarea
    class="base-textarea"
    :value="modelValue"
    :rows="rows"
    v-bind="$attrs"
    @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
</template>

<style scoped>
.base-textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 4.5rem;
  resize: vertical;
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0.45rem 0.6rem;
  font: inherit;
  line-height: 1.4;
}
</style>
