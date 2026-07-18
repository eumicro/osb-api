<script setup lang="ts">
import BaseInput from "../atoms/BaseInput.vue";
import FormField from "./FormField.vue";

const oauthClientId = defineModel<string>("oauthClientId", { required: true });
const oauthClientSecret = defineModel<string>("oauthClientSecret", { required: true });
const wellKnownUrl = defineModel<string>("wellKnownUrl", { required: true });

defineProps<{
  /** When true, secret input is required (create). When false, empty keeps existing. */
  requireSecret?: boolean;
  secretConfigured?: boolean;
}>();
</script>

<template>
  <div class="client-credentials-fields">
    <FormField :label="$t('clientAuth.oauthClientId')">
      <BaseInput
        v-model="oauthClientId"
        required
        autocomplete="off"
        placeholder="my-service-client"
      />
    </FormField>
    <FormField :label="$t('clientAuth.oauthClientSecret')">
      <BaseInput
        v-model="oauthClientSecret"
        type="password"
        autocomplete="new-password"
        :required="requireSecret && !secretConfigured"
        :placeholder="
          secretConfigured
            ? $t('clientAuth.oauthClientSecretKeepHint')
            : $t('clientAuth.oauthClientSecret')
        "
      />
    </FormField>
    <FormField :label="$t('clientAuth.wellKnownUrl')">
      <BaseInput
        v-model="wellKnownUrl"
        required
        type="url"
        placeholder="https://idp.example/.well-known/openid-configuration"
      />
    </FormField>
  </div>
</template>
