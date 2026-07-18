<script setup lang="ts">
import { ref, watch } from "vue";
import type { ServiceOffering, ServicePlan } from "../../models/catalog";
import type { UpdateOfferingRequest } from "../../models/catalogAdmin";
import BaseButton from "../atoms/BaseButton.vue";
import BaseInput from "../atoms/BaseInput.vue";
import DeleteButton from "../molecules/DeleteButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ offering: ServiceOffering }>();
const emit = defineEmits<{
  save: [request: UpdateOfferingRequest];
  selectPlan: [plan: ServicePlan];
  removePlan: [planId: string];
}>();

const name = ref(props.offering.name);
const description = ref(props.offering.description);
const bindable = ref(props.offering.bindable);

watch(
  () => props.offering,
  (offering) => {
    name.value = offering.name;
    description.value = offering.description;
    bindable.value = offering.bindable;
  },
);

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value,
    bindable: bindable.value,
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="offering-detail">
      <FormField :label="$t('common.id')">
        <BaseInput :model-value="offering.id" disabled />
      </FormField>
      <FormField :label="$t('common.name')">
        <BaseInput v-model="name" required />
      </FormField>
      <DescriptionField v-model="description" />
      <label class="checkbox-row">
        <input v-model="bindable" type="checkbox" />
        {{ $t("offerings.bindable") }}
      </label>

      <section class="plans-section">
        <h3>{{ $t("panels.plans") }}</h3>
        <p class="muted">{{ $t("offerings.plansHint") }}</p>
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ $t("common.name") }}</th>
              <th>{{ $t("common.description") }}</th>
              <th>{{ $t("common.free") }}</th>
              <th>{{ $t("common.actions") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="plan in offering.plans"
              :key="plan.id"
              class="selectable"
              @click="$emit('selectPlan', plan)"
            >
              <td>{{ plan.name }}</td>
              <td>{{ plan.description }}</td>
              <td>{{ plan.free ? $t("common.yes") : $t("common.no") }}</td>
              <td class="actions-cell" @click.stop>
                <BaseButton
                  icon="edit"
                  :label="$t('common.edit')"
                  variant="secondary"
                  @click="$emit('selectPlan', plan)"
                />
                <DeleteButton
                  :label="$t('common.delete')"
                  :disabled="offering.plans.length <= 1"
                  @click="$emit('removePlan', plan.id)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </section>
  </div>
</template>

<style scoped>
.plans-section {
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
}
.plans-section h3 {
  margin: 0 0 0.35rem;
  font-size: 0.95rem;
}
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
}
.actions-cell {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}
</style>
