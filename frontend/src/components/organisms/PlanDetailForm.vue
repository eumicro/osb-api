<script setup lang="ts">
import { ref, watch } from "vue";
import type { ServiceOffering } from "../../models/catalog";
import type { SavePlanRequest } from "../../models/catalogAdmin";
import type { PlanDetail } from "../../models/plan";
import { withCreateParametersSchema } from "../../models/planSchemas";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";
import PlanParametersSchemaEditor from "../molecules/PlanParametersSchemaEditor.vue";

const props = defineProps<{
  plan: PlanDetail;
  offerings: ServiceOffering[];
}>();
const emit = defineEmits<{
  save: [request: SavePlanRequest, targetOfferingId: string];
}>();

const name = ref(props.plan.name);
const description = ref(props.plan.description);
const free = ref(props.plan.free);
const bindable = ref(props.plan.bindable);
const offeringId = ref(props.plan.serviceId);
const schemas = ref<Record<string, unknown>>(
  (props.plan.schemas as Record<string, unknown>)
    ?? (withCreateParametersSchema({}, { type: "object", properties: {} }) as Record<
      string,
      unknown
    >),
);
const parametersUiSchema = ref<Record<string, unknown>>(
  (props.plan.parametersUiSchema as Record<string, unknown>) ?? {},
);

watch(
  () => props.plan,
  (plan) => {
    name.value = plan.name;
    description.value = plan.description;
    free.value = plan.free;
    bindable.value = plan.bindable;
    offeringId.value = plan.serviceId;
    schemas.value = (plan.schemas as Record<string, unknown>)
      ?? (withCreateParametersSchema({}, { type: "object", properties: {} }) as Record<
        string,
        unknown
      >);
    parametersUiSchema.value = (plan.parametersUiSchema as Record<string, unknown>) ?? {};
  },
);

function submit() {
  emit(
    "save",
    {
      id: props.plan.id,
      name: name.value.trim(),
      description: description.value,
      free: free.value,
      bindable: bindable.value,
      schemas: schemas.value,
      parametersUiSchema: parametersUiSchema.value,
    },
    offeringId.value,
  );
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('plans.planId')">
      <BaseInput :model-value="plan.id" disabled />
    </FormField>
    <FormField :label="$t('plans.offering')">
      <BaseSelect v-model="offeringId" required>
        <option v-for="offering in offerings" :key="offering.id" :value="offering.id">
          {{ offering.name }} ({{ offering.id }})
        </option>
      </BaseSelect>
    </FormField>
    <p class="muted">{{ $t("plans.offeringHint") }}</p>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <label class="checkbox-row">
      <input v-model="free" type="checkbox" />
      {{ $t("common.free") }}
    </label>
    <label class="checkbox-row">
      <input v-model="bindable" type="checkbox" />
      {{ $t("offerings.bindable") }}
    </label>
    <FormField :label="$t('plans.parametersSchema')">
      <p class="muted">{{ $t("plans.parametersSchemaHint") }}</p>
      <PlanParametersSchemaEditor
        v-model:schemas="schemas"
        v-model:parameters-ui-schema="parametersUiSchema"
      />
    </FormField>
  </div>
</template>

<style scoped>
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
}
.muted {
  margin: 0 0 0.45rem;
  color: var(--muted);
  font-size: 0.85rem;
}
</style>
