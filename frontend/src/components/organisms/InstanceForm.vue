<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { AdminCatalog, ServiceOffering, ServicePlan } from "../../models/catalog";
import type { PlatformClient } from "../../models/platformClient";
import type { ProvisionInstanceRequest } from "../../models/serviceInstance";
import BaseSelect from "../atoms/BaseSelect.vue";
import CreateButton from "../molecules/CreateButton.vue";
import FormField from "../molecules/FormField.vue";
import PlanParametersForm from "../molecules/PlanParametersForm.vue";

const props = defineProps<{
  catalogs: AdminCatalog[];
  platforms: PlatformClient[];
  preferredCatalogId?: string | null;
  preferredOfferingId?: string | null;
}>();
const emit = defineEmits<{ save: [request: ProvisionInstanceRequest] }>();

const catalogId = ref("");
const serviceId = ref("");
const planId = ref("");
const platformClientId = ref("");
const parameters = ref<Record<string, unknown>>({});
const parametersFormRef = ref<InstanceType<typeof PlanParametersForm>>();

const offerings = computed(() => {
  const catalog = props.catalogs.find((item) => item.id === catalogId.value);
  return catalog?.offerings ?? [];
});

const plans = computed(() => {
  const offering = offerings.value.find((item) => item.id === serviceId.value);
  return offering?.plans ?? [];
});

const selectedPlan = computed<ServicePlan | null>(() => {
  return plans.value.find((item) => item.id === planId.value) ?? null;
});

watch(
  () => [props.catalogs, props.preferredCatalogId] as const,
  () => {
    if (
      props.preferredCatalogId
      && props.catalogs.some((item) => item.id === props.preferredCatalogId)
    ) {
      catalogId.value = props.preferredCatalogId;
    } else if (!catalogId.value && props.catalogs.length > 0) {
      catalogId.value = props.catalogs[0].id;
    }
  },
  { immediate: true },
);

watch(
  () => [offerings.value, props.preferredOfferingId] as const,
  () => {
    if (
      props.preferredOfferingId
      && offerings.value.some((item: ServiceOffering) => item.id === props.preferredOfferingId)
    ) {
      serviceId.value = props.preferredOfferingId;
    } else if (!offerings.value.some((item) => item.id === serviceId.value)) {
      serviceId.value = offerings.value[0]?.id ?? "";
    }
  },
  { immediate: true },
);

watch(
  plans,
  (next) => {
    if (!next.some((item) => item.id === planId.value)) {
      planId.value = next[0]?.id ?? "";
    }
  },
  { immediate: true },
);

watch(selectedPlan, () => {
  parameters.value = {};
});

function submit() {
  if (parametersFormRef.value && !parametersFormRef.value.validate()) {
    return;
  }
  emit("save", {
    serviceId: serviceId.value,
    planId: planId.value,
    platformClientId: platformClientId.value || null,
    parameters: parameters.value,
  });
  platformClientId.value = "";
  parameters.value = {};
}
</script>

<template>
  <!--
    No outer <form>: PlanParametersForm/JsonSchemaForm already renders a <form>.
    Nested forms break the parameter controls in the browser.
  -->
  <div class="instance-form">
    <FormField :label="$t('instances.catalog')">
      <BaseSelect v-model="catalogId" required>
        <option v-for="catalog in catalogs" :key="catalog.id" :value="catalog.id">
          {{ catalog.name }} ({{ catalog.id }})
        </option>
      </BaseSelect>
    </FormField>
    <FormField :label="$t('instances.service')">
      <BaseSelect v-model="serviceId" required>
        <option v-for="offering in offerings" :key="offering.id" :value="offering.id">
          {{ offering.name }} ({{ offering.id }})
        </option>
      </BaseSelect>
    </FormField>
    <FormField :label="$t('instances.plan')">
      <BaseSelect v-model="planId" required>
        <option v-for="plan in plans" :key="plan.id" :value="plan.id">
          {{ plan.name }} ({{ plan.id }})
        </option>
      </BaseSelect>
    </FormField>
    <FormField :label="$t('instances.platform')">
      <BaseSelect v-model="platformClientId">
        <option value="">{{ $t("instances.noPlatform") }}</option>
        <option v-for="platform in platforms" :key="platform.id" :value="platform.id">
          {{ platform.displayName }} ({{ platform.id }})
        </option>
      </BaseSelect>
    </FormField>
    <p class="muted">{{ $t("instances.platformHint") }}</p>
    <!-- Not FormField/<label>: wrapping the JSE form in a label collapses controls. -->
    <div class="parameters-block">
      <span class="parameters-block__label">{{ $t("instances.parameters") }}</span>
      <PlanParametersForm
        ref="parametersFormRef"
        v-model="parameters"
        :form-key="selectedPlan?.id ?? ''"
        :schemas="selectedPlan?.schemas"
        :parameters-ui-schema="selectedPlan?.parametersUiSchema"
      />
    </div>
    <CreateButton type="button" :label="$t('common.create')" :disabled="!planId" @click="submit" />
  </div>
</template>

<style scoped>
.muted {
  margin: 0 0 0.75rem;
  color: var(--muted);
  font-size: 0.85rem;
}

.parameters-block {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  margin-bottom: 0.75rem;
}

.parameters-block__label {
  font-size: 0.85rem;
  color: var(--muted);
  font-weight: 600;
}
</style>
