<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  KUBERNETES_CLIENT_AUTH_TYPES,
  type KubernetesClientAuthType,
  type KubernetesClientInstance,
  type UpdateKubernetesClientInstanceRequest,
} from "../../models/kubernetesClient";
import BaseInput from "../atoms/BaseInput.vue";
import AuthTypeSelect from "../molecules/AuthTypeSelect.vue";
import ClientCredentialsFields from "../molecules/ClientCredentialsFields.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ kubernetesClient: KubernetesClientInstance }>();
const emit = defineEmits<{ save: [request: UpdateKubernetesClientInstanceRequest] }>();

const name = ref(props.kubernetesClient.name);
const description = ref(props.kubernetesClient.description);
const apiServerUrl = ref(props.kubernetesClient.apiServerUrl);
const defaultNamespace = ref(props.kubernetesClient.defaultNamespace);
const authType = ref<KubernetesClientAuthType>(props.kubernetesClient.authType);
const username = ref(props.kubernetesClient.username ?? "");
const token = ref("");
const oauthClientId = ref(props.kubernetesClient.oauthClientId ?? "");
const oauthClientSecret = ref("");
const wellKnownUrl = ref(props.kubernetesClient.wellKnownUrl ?? "");
const insecureSkipTlsVerify = ref(props.kubernetesClient.insecureSkipTlsVerify);
const timeoutSeconds = ref(String(props.kubernetesClient.timeoutSeconds));
const enabled = ref(props.kubernetesClient.enabled);

const needsUsername = computed(() => authType.value === "BASIC");
const needsToken = computed(
  () => authType.value === "BASIC" || authType.value === "BEARER",
);
const needsClientCredentials = computed(() => authType.value === "CLIENT_CREDENTIALS");

watch(
  () => props.kubernetesClient,
  (client) => {
    name.value = client.name;
    description.value = client.description;
    apiServerUrl.value = client.apiServerUrl;
    defaultNamespace.value = client.defaultNamespace;
    authType.value = client.authType;
    username.value = client.username ?? "";
    token.value = "";
    oauthClientId.value = client.oauthClientId ?? "";
    oauthClientSecret.value = "";
    wellKnownUrl.value = client.wellKnownUrl ?? "";
    insecureSkipTlsVerify.value = client.insecureSkipTlsVerify;
    timeoutSeconds.value = String(client.timeoutSeconds);
    enabled.value = client.enabled;
  },
);

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    apiServerUrl: apiServerUrl.value.trim(),
    defaultNamespace: defaultNamespace.value.trim() || "default",
    authType: authType.value,
    username: username.value.trim(),
    token: needsToken.value ? token.value : "",
    oauthClientId: needsClientCredentials.value ? oauthClientId.value.trim() : "",
    oauthClientSecret: needsClientCredentials.value ? oauthClientSecret.value : "",
    wellKnownUrl: needsClientCredentials.value ? wellKnownUrl.value.trim() : "",
    insecureSkipTlsVerify: insecureSkipTlsVerify.value,
    timeoutSeconds: Number(timeoutSeconds.value) || 30,
    enabled: enabled.value,
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="kubernetesClient.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('kubernetesClients.apiServerUrl')">
      <BaseInput v-model="apiServerUrl" required />
    </FormField>
    <FormField :label="$t('kubernetesClients.defaultNamespace')">
      <BaseInput v-model="defaultNamespace" required />
    </FormField>
    <AuthTypeSelect
      v-model="authType"
      :options="KUBERNETES_CLIENT_AUTH_TYPES"
      label-key="kubernetesClients.authType"
      option-key-prefix="kubernetesClients.authTypes"
    />
    <FormField v-if="needsUsername" :label="$t('kubernetesClients.username')">
      <BaseInput v-model="username" required />
    </FormField>
    <FormField
      v-if="needsToken"
      :label="
        authType === 'BASIC' ? $t('kubernetesClients.password') : $t('kubernetesClients.token')
      "
    >
      <BaseInput
        v-model="token"
        type="password"
        autocomplete="new-password"
        :placeholder="
          kubernetesClient.tokenConfigured
            ? $t('kubernetesClients.tokenKeepHint')
            : authType === 'BASIC'
              ? $t('kubernetesClients.password')
              : $t('kubernetesClients.token')
        "
      />
    </FormField>
    <ClientCredentialsFields
      v-if="needsClientCredentials"
      v-model:oauth-client-id="oauthClientId"
      v-model:oauth-client-secret="oauthClientSecret"
      v-model:well-known-url="wellKnownUrl"
      :secret-configured="kubernetesClient.oauthClientSecretConfigured"
    />
    <FormField :label="$t('kubernetesClients.timeoutSeconds')">
      <BaseInput v-model="timeoutSeconds" type="number" min="1" max="300" required />
    </FormField>
    <label class="checkbox-row">
      <input v-model="insecureSkipTlsVerify" type="checkbox" />
      {{ $t("kubernetesClients.insecureSkipTlsVerify") }}
    </label>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("kubernetesClients.enabled") }}
    </label>
  </div>
</template>

<style scoped>
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.85rem;
  font-size: 0.9rem;
}
</style>
