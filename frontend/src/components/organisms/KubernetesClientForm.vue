<script setup lang="ts">
import { computed, ref } from "vue";
import {
  KUBERNETES_CLIENT_AUTH_TYPES,
  type CreateKubernetesClientInstanceRequest,
  type KubernetesClientAuthType,
} from "../../models/kubernetesClient";
import BaseInput from "../atoms/BaseInput.vue";
import AuthTypeSelect from "../molecules/AuthTypeSelect.vue";
import ClientCredentialsFields from "../molecules/ClientCredentialsFields.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const emit = defineEmits<{ save: [request: CreateKubernetesClientInstanceRequest] }>();

const name = ref("");
const description = ref("");
const apiServerUrl = ref("https://");
const defaultNamespace = ref("default");
const authType = ref<KubernetesClientAuthType>("NONE");
const username = ref("");
const token = ref("");
const oauthClientId = ref("");
const oauthClientSecret = ref("");
const wellKnownUrl = ref("");
const insecureSkipTlsVerify = ref(false);
const timeoutSeconds = ref("30");
const enabled = ref(true);

const needsUsername = computed(() => authType.value === "BASIC");
const needsToken = computed(
  () => authType.value === "BASIC" || authType.value === "BEARER",
);
const needsClientCredentials = computed(() => authType.value === "CLIENT_CREDENTIALS");

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
  name.value = "";
  description.value = "";
  apiServerUrl.value = "https://";
  defaultNamespace.value = "default";
  authType.value = "NONE";
  username.value = "";
  token.value = "";
  oauthClientId.value = "";
  oauthClientSecret.value = "";
  wellKnownUrl.value = "";
  insecureSkipTlsVerify.value = false;
  timeoutSeconds.value = "30";
  enabled.value = true;
}
</script>

<template>
  <form @submit.prevent="submit">
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('kubernetesClients.apiServerUrl')">
      <BaseInput
        v-model="apiServerUrl"
        required
        placeholder="https://kubernetes.default.svc"
      />
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
      <BaseInput v-model="token" type="password" required autocomplete="new-password" />
    </FormField>
    <ClientCredentialsFields
      v-if="needsClientCredentials"
      v-model:oauth-client-id="oauthClientId"
      v-model:oauth-client-secret="oauthClientSecret"
      v-model:well-known-url="wellKnownUrl"
      require-secret
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
    <CreateButton type="submit" :label="$t('common.create')" />
  </form>
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
