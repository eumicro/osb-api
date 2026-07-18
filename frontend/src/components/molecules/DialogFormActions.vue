<script setup lang="ts">
import type { IconName } from "../../models/iconName";
import BaseButton from "../atoms/BaseButton.vue";

/** Molecule: Standard-Aktionsleiste fuer Dialoge (Abbrechen + Primaeraktion). */
withDefaults(
  defineProps<{
    cancelLabel: string;
    submitLabel: string;
    submitIcon?: IconName;
    submitDisabled?: boolean;
    submitVariant?: "primary" | "danger";
  }>(),
  { submitIcon: "check", submitVariant: "primary" },
);

defineEmits<{ cancel: []; submit: [] }>();
</script>

<template>
  <div class="dialog-actions">
    <BaseButton icon="x" :label="cancelLabel" variant="secondary" @click="$emit('cancel')" />
    <BaseButton
      :icon="submitIcon"
      :label="submitLabel"
      :variant="submitVariant ?? 'primary'"
      :disabled="submitDisabled"
      @click="$emit('submit')"
    />
  </div>
</template>
