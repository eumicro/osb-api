<script setup lang="ts">
import { ref, watch } from "vue";
import type { ServiceOffering } from "../../models/catalog";
import type { CreatePlanRequest } from "../../models/catalogAdmin";
import { withCreateParametersSchema } from "../../models/planSchemas";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";
import IdInput from "../molecules/IdInput.vue";
import PlanParametersSchemaEditor from "../molecules/PlanParametersSchemaEditor.vue";

const props = defineProps<{
  offerings: ServiceOffering[];
  preferredOfferingId?: string | null;
}>();
const emit = defineEmits<{ save: [request: CreatePlanRequest] }>();

const offeringId = ref(props.preferredOfferingId || props.offerings[0]?.id || "");
const id = ref("");
const name = ref("");
const description = ref("");
const free = ref(true);
const bindable = ref(true);
const schemas = ref<Record<string, unknown>>(
  withCreateParametersSchema({}, { type: "object", properties: {} }) as Record<string, unknown>,
);
const parametersUiSchema = ref<Record<string, unknown>>({});

watch(
  () => [props.offerings, props.preferredOfferingId] as const,
  () => {
    if (
      props.preferredOfferingId
      && props.offerings.some((item) => item.id === props.preferredOfferingId)
    ) {
      offeringId.value = props.preferredOfferingId;
      return;
    }
    if (!props.offerings.some((item) => item.id === offeringId.value)) {
      offeringId.value = props.offerings[0]?.id || "";
    }
  },
  { immediate: true },
);

function submit() {
  emit("save", {
    offeringId: offeringId.value,
    id: id.value.trim(),
    name: name.value.trim(),
    description: description.value,
    free: free.value,
    bindable: bindable.value,
    schemas: schemas.value,
    parametersUiSchema: parametersUiSchema.value,
  });
  id.value = "";
  name.value = "";
  description.value = "";
  free.value = true;
  bindable.value = true;
  schemas.value = withCreateParametersSchema({}, { type: "object", properties: {} }) as Record<
    string,
    unknown
  >;
  parametersUiSchema.value = {};
}
</script>

<template>
  <form @submit.prevent="submit">
    <FormField :label="$t('plans.offering')">
      <BaseSelect v-model="offeringId" required>
        <option v-for="offering in offerings" :key="offering.id" :value="offering.id">
          {{ offering.name }} ({{ offering.id }})
        </option>
      </BaseSelect>
    </FormField>
    <FormField :label="$t('plans.planId')">
      <IdInput v-model="id" required pattern="[a-z0-9-]+" />
    </FormField>
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
    <CreateButton type="submit" :label="$t('common.create')" />
  </form>
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
