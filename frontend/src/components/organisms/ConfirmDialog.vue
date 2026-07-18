<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import DialogFormActions from "../molecules/DialogFormActions.vue";
import AppDialog from "./AppDialog.vue";

/** Organism: Bestaetigungsdialog fuer destruktive oder irreversible Aktionen. */
const { open, options, onConfirm, onCancel } = useConfirm();
const { t } = useI18n();

const title = computed(() => options.value?.title ?? t("dialog.confirmTitle"));
const message = computed(() => options.value?.message ?? "");
const confirmLabel = computed(() => options.value?.confirmLabel ?? t("dialog.confirm"));
const cancelLabel = computed(() => options.value?.cancelLabel ?? t("common.cancel"));
const confirmVariant = computed(() =>
  options.value?.confirmVariant === "danger" ? "danger" : "primary",
);
</script>

<template>
  <AppDialog :open="open" :title="title" size="sm" @update:open="(value) => !value && onCancel()">
    <p class="dialog-message">{{ message }}</p>
    <template #footer>
      <DialogFormActions
        :cancel-label="cancelLabel"
        :submit-label="confirmLabel"
        :submit-icon="confirmVariant === 'danger' ? 'trash' : 'check'"
        :submit-variant="confirmVariant"
        @cancel="onCancel"
        @submit="onConfirm"
      />
    </template>
  </AppDialog>
</template>
