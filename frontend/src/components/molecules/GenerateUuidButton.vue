<script setup lang="ts">
import { useI18n } from "vue-i18n";
import BaseButton from "../atoms/BaseButton.vue";

/** Molecule: icon button that signals UUID generation. */
defineEmits<{ click: [] }>();

const { t } = useI18n();
</script>

<template>
  <BaseButton
    icon="uuid"
    :label="t('common.generateUuid')"
    variant="secondary"
    type="button"
    @click="$emit('click')"
  />
</template>
