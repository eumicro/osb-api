<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { AdminCatalog } from "../../models/catalog";
import type { PlatformClient } from "../../models/platformClient";
import type { ServiceInstance, UpdateInstanceRequest } from "../../models/serviceInstance";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import InstanceStatusIcon from "../atoms/InstanceStatusIcon.vue";
import FormField from "../molecules/FormField.vue";
import PlanParametersForm from "../molecules/PlanParametersForm.vue";

const props = defineProps<{
  instance: ServiceInstance;
  platforms: PlatformClient[];
  catalogs?: AdminCatalog[];
}>();
const emit = defineEmits<{ save: [request: UpdateInstanceRequest] }>();

const platformClientId = ref(props.instance.platformClientId ?? "");
const dashboardUrl = ref(props.instance.dashboardUrl ?? "");
const parameters = ref<Record<string, unknown>>({ ...(props.instance.parameters ?? {}) });

const plan = computed(() => {
  for (const catalog of props.catalogs ?? []) {
    for (const offering of catalog.offerings) {
      if (offering.id !== props.instance.serviceId) continue;
      const found = offering.plans.find((item) => item.id === props.instance.planId);
      if (found) return found;
    }
  }
  return null;
});

watch(
  () => props.instance,
  (instance) => {
    platformClientId.value = instance.platformClientId ?? "";
    dashboardUrl.value = instance.dashboardUrl ?? "";
    parameters.value = { ...(instance.parameters ?? {}) };
  },
);

function submit() {
  emit("save", {
    platformClientId: platformClientId.value || null,
    dashboardUrl: dashboardUrl.value.trim() || null,
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="instance.id" disabled />
    </FormField>
    <FormField :label="$t('instances.service')">
      <BaseInput :model-value="instance.serviceId" disabled />
    </FormField>
    <FormField :label="$t('instances.plan')">
      <BaseInput :model-value="instance.planId" disabled />
    </FormField>
    <FormField :label="$t('common.status')">
      <InstanceStatusIcon :state="instance.state" show-label />
    </FormField>
    <FormField :label="$t('instances.lastOperation')">
      <BaseInput
        :model-value="
          instance.lastOperationKind
            ? `${instance.lastOperationKind}: ${instance.lastOperationState ?? '—'} — ${instance.lastOperationDescription ?? ''}`
            : '—'
        "
        disabled
      />
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
    <FormField :label="$t('instances.dashboardUrl')">
      <BaseInput v-model="dashboardUrl" />
      <a
        v-if="dashboardUrl && /^https?:\/\//i.test(dashboardUrl)"
        class="dashboard-link"
        :href="dashboardUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ $t("instances.openDashboard") }}
      </a>
    </FormField>
    <div class="parameters-block">
      <span class="parameters-block__label">{{ $t("instances.parameters") }}</span>
      <PlanParametersForm
        v-model="parameters"
        :form-key="instance.planId"
        :schemas="plan?.schemas"
        :parameters-ui-schema="plan?.parametersUiSchema"
        readonly
      />
    </div>
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

.dashboard-link {
  display: inline-block;
  margin-top: 0.35rem;
  font-size: 0.85rem;
  color: var(--accent, #1d4ed8);
}
</style>
