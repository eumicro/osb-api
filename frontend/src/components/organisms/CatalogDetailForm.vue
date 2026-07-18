<script setup lang="ts">
import { ref, watch } from "vue";
import type { AdminCatalog } from "../../models/catalog";
import type { CatalogSavePayload } from "../../models/catalogAdmin";
import type { PlatformClient } from "../../models/platformClient";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

export type { CatalogSavePayload };

const props = defineProps<{
  catalog: AdminCatalog;
  platforms: PlatformClient[];
  catalogs: AdminCatalog[];
}>();
const emit = defineEmits<{ save: [payload: CatalogSavePayload] }>();

const name = ref(props.catalog.name);
const description = ref(props.catalog.description);
const assignedIds = ref<string[]>([]);
const fallbackCatalogId = ref("");

function syncPlatformSelection() {
  assignedIds.value = props.platforms
    .filter((platform) => platform.catalogId === props.catalog.id)
    .map((platform) => platform.id);
  const other = props.catalogs.find((item) => item.id !== props.catalog.id);
  fallbackCatalogId.value = other?.id ?? "";
}

watch(
  () => props.catalog,
  (catalog) => {
    name.value = catalog.name;
    description.value = catalog.description;
  },
);

watch(
  () => [props.platforms, props.catalog.id, props.catalogs] as const,
  () => {
    syncPlatformSelection();
  },
  { immediate: true, deep: true },
);

function togglePlatform(platformId: string, checked: boolean) {
  if (checked) {
    if (!assignedIds.value.includes(platformId)) {
      assignedIds.value = [...assignedIds.value, platformId];
    }
    return;
  }
  assignedIds.value = assignedIds.value.filter((id) => id !== platformId);
}

function submit() {
  emit("save", {
    catalog: {
      name: name.value.trim(),
      description: description.value,
    },
    assignedPlatformIds: [...assignedIds.value],
    fallbackCatalogId: fallbackCatalogId.value || null,
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="catalog.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />

    <section class="relation-section">
      <h3>{{ $t("catalogs.assignedPlatforms") }}</h3>
      <p class="muted">{{ $t("catalogs.assignHint") }}</p>
      <div v-if="platforms.length === 0" class="muted">{{ $t("catalogs.noPlatforms") }}</div>
      <ul v-else class="checkbox-list">
        <li v-for="platform in platforms" :key="platform.id">
          <label class="checkbox-row">
            <input
              type="checkbox"
              :checked="assignedIds.includes(platform.id)"
              @change="
                togglePlatform(
                  platform.id,
                  ($event.target as HTMLInputElement).checked,
                )
              "
            />
            {{ platform.displayName }}
            <span class="muted">({{ platform.username }})</span>
          </label>
        </li>
      </ul>
      <FormField
        v-if="catalogs.some((item) => item.id !== catalog.id)"
        :label="$t('catalogs.unassignFallback')"
      >
        <BaseSelect v-model="fallbackCatalogId" required>
          <option
            v-for="item in catalogs.filter((entry) => entry.id !== catalog.id)"
            :key="item.id"
            :value="item.id"
          >
            {{ item.name }} ({{ item.id }})
          </option>
        </BaseSelect>
      </FormField>
      <p class="muted">{{ $t("catalogs.unassignHint") }}</p>
    </section>
  </div>
</template>

<style scoped>
.relation-section {
  margin: 1.25rem 0 0;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
}
.relation-section h3 {
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
}
.checkbox-list {
  list-style: none;
  margin: 0 0 0.85rem;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.9rem;
}
</style>
