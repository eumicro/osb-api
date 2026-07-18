<script setup lang="ts">
import type { Catalog } from "../../models/catalog";

defineProps<{ catalog: Catalog }>();
</script>

<template>
  <section class="card">
    <h2>
      {{ $t("dashboard.catalog") }}
      <small class="muted">{{ $t("dashboard.catalogHint") }}</small>
    </h2>

    <p v-if="catalog.services.length === 0" class="muted">
      {{ $t("dashboard.noOfferings") }}
    </p>

    <article
      v-for="service in catalog.services"
      :key="service.id"
      class="offering"
    >
      <h3>{{ service.name }}</h3>
      <p class="muted">{{ service.description }}</p>
      <div class="plans" :aria-label="$t('dashboard.plans')">
        <span v-for="plan in service.plans" :key="plan.id" class="plan">
          {{ plan.name }}{{ plan.free ? ` · ${$t("common.free")}` : "" }}
        </span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.offering {
  border: 1px solid var(--border);
  border-radius: 0.55rem;
  padding: 0.85rem 0.95rem;
  margin-top: 0.75rem;
}

.offering h3 {
  margin: 0 0 0.25rem;
  font-size: 1rem;
}

.offering p {
  margin: 0;
  font-size: 0.9rem;
}

.plans {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.7rem;
}

.plan {
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 0.25rem 0.7rem;
  font-size: 0.82rem;
  color: var(--muted);
}

small {
  font-weight: 400;
  margin-left: 0.4rem;
}
</style>
