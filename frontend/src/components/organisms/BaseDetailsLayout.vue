<script setup lang="ts">
import { useI18n } from "vue-i18n";
import BaseButton from "../atoms/BaseButton.vue";
import CollapsibleSection from "../molecules/CollapsibleSection.vue";
import ErrorMessage from "../molecules/ErrorMessage.vue";

/**
 * Organism: gemeinsames Detail-Layout (Meta, Body, Footer mit Aktionen unten rechts).
 * Optional: nur der Body (Konfiguration) ist einklappbar — die Button-Leiste bleibt sichtbar.
 */
withDefaults(
  defineProps<{
    meta?: string;
    error?: string;
    empty?: string;
    loading?: boolean;
    ready?: boolean;
    showSave?: boolean;
    saveLabel?: string;
    /** Body in CollapsibleSection; Footer bleibt außerhalb. */
    collapsible?: boolean;
    collapsibleTitle?: string;
  }>(),
  {
    ready: false,
    loading: false,
    showSave: true,
    collapsible: false,
  },
);

const bodyOpen = defineModel<boolean>("bodyOpen", { default: true });

const emit = defineEmits<{ submit: [] }>();
const { t } = useI18n();
</script>

<template>
  <div class="base-details" :class="{ 'base-details--collapsible': collapsible }">
    <p v-if="!ready && !loading && empty" class="muted">{{ empty }}</p>
    <p v-else-if="loading && !ready" class="muted">{{ $t("common.loading") }}</p>
    <form v-else-if="ready" class="base-details__form" @submit.prevent="emit('submit')">
      <CollapsibleSection
        v-if="collapsible"
        v-model="bodyOpen"
        :title="collapsibleTitle ?? t('common.configuration')"
      >
        <p v-if="meta" class="base-details__meta muted">{{ meta }}</p>
        <ErrorMessage :message="error ?? ''" />
        <div class="base-details__body">
          <slot />
        </div>
      </CollapsibleSection>
      <template v-else>
        <p v-if="meta" class="base-details__meta muted">{{ meta }}</p>
        <ErrorMessage :message="error ?? ''" />
        <div class="base-details__body">
          <slot />
        </div>
      </template>
      <footer class="base-details__footer">
        <div class="base-details__actions">
          <slot name="actions" />
          <BaseButton
            v-if="showSave"
            type="submit"
            icon="save"
            :label="saveLabel ?? t('common.save')"
          />
        </div>
      </footer>
    </form>
    <ErrorMessage v-else :message="error ?? ''" />
  </div>
</template>

<style scoped>
.base-details,
.base-details__form {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.base-details__meta {
  margin: 0 0 0.5rem;
  flex: 0 0 auto;
}

.base-details__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.base-details__footer {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 0.35rem;
  padding-top: 0.55rem;
  border-top: 1px solid var(--border);
}

.base-details__actions {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
