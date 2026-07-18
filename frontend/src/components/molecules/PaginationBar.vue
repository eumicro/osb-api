<script setup lang="ts">
import { computed } from "vue";
import { PAGE_SIZE_OPTIONS } from "../../controllers/usePagination";
import BaseButton from "../atoms/BaseButton.vue";
import BaseSelect from "../atoms/BaseSelect.vue";

const props = defineProps<{
  page: number;
  pageCount: number;
  pageSize: number;
  total: number;
  rangeStart: number;
  rangeEnd: number;
  canPrev: boolean;
  canNext: boolean;
}>();

const emit = defineEmits<{
  prev: [];
  next: [];
  "update:pageSize": [size: number];
}>();

const pageSizeModel = computed({
  get: () => String(props.pageSize),
  set: (value: string) => emit("update:pageSize", Number(value)),
});
</script>

<template>
  <div class="pagination-bar" role="navigation" :aria-label="$t('pagination.label')">
    <label class="pagination-bar__size">
      <span class="muted">{{ $t("pagination.pageSize") }}</span>
      <BaseSelect v-model="pageSizeModel">
        <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="String(size)">
          {{ size }}
        </option>
      </BaseSelect>
    </label>

    <span class="pagination-bar__summary muted">
      {{ $t("pagination.summary", { from: rangeStart, to: rangeEnd, total }) }}
    </span>

    <div class="pagination-bar__nav">
      <BaseButton
        icon="chevronLeft"
        variant="secondary"
        :label="$t('pagination.previous')"
        :disabled="!canPrev"
        @click="emit('prev')"
      />
      <span class="pagination-bar__page muted">
        {{ $t("pagination.page", { page, pageCount }) }}
      </span>
      <BaseButton
        icon="chevronRight"
        variant="secondary"
        :label="$t('pagination.next')"
        :disabled="!canNext"
        @click="emit('next')"
      />
    </div>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem 1rem;
}

.pagination-bar__size {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.85rem;
}

.pagination-bar__summary,
.pagination-bar__page {
  font-size: 0.85rem;
  white-space: nowrap;
}

.pagination-bar__nav {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
</style>
