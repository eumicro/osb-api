<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { IconName } from "../../models/iconName";
import Icon from "./Icon.vue";

/**
 * Atom: Instanz-Status als farbiges Symbol (Tooltip = Status-Text).
 */
const props = withDefaults(
  defineProps<{
    state: string;
    /** Textlabel neben dem Symbol anzeigen. */
    showLabel?: boolean;
  }>(),
  { showLabel: false },
);

const { t, te } = useI18n();

type Tone = "ok" | "danger" | "warn" | "muted" | "accent";

interface StatusVisual {
  icon: IconName;
  tone: Tone;
  spin?: boolean;
}

function normalizeState(state: string): string {
  return state.trim().toLowerCase();
}

const visual = computed<StatusVisual>(() => {
  switch (normalizeState(props.state)) {
    case "succeeded":
      return { icon: "check", tone: "ok" };
    case "failed":
      return { icon: "x", tone: "danger" };
    case "in progress":
      return { icon: "refresh", tone: "warn", spin: true };
    case "deleting":
      return { icon: "trash", tone: "warn", spin: false };
    case "deprovisioned":
      return { icon: "check", tone: "muted" };
    default:
      return { icon: "status", tone: "accent" };
  }
});

const label = computed(() => {
  const key = `instances.states.${normalizeState(props.state).replace(/\s+/g, "_")}`;
  if (te(key)) return String(t(key));
  return props.state || "—";
});
</script>

<template>
  <span
    class="instance-status"
    :class="[`instance-status--${visual.tone}`, { 'instance-status--spin': visual.spin }]"
    :title="label"
    :aria-label="label"
    role="img"
  >
    <Icon :name="visual.icon" class="instance-status__icon" />
    <span v-if="showLabel" class="instance-status__label">{{ label }}</span>
  </span>
</template>

<style scoped>
.instance-status {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  line-height: 1;
  vertical-align: middle;
}

.instance-status__icon {
  width: 1.15em;
  height: 1.15em;
}

.instance-status__label {
  font-size: 0.9rem;
}

.instance-status--ok {
  color: var(--ok);
}

.instance-status--danger {
  color: var(--danger);
}

.instance-status--warn {
  color: var(--warn);
}

.instance-status--muted {
  color: var(--muted);
}

.instance-status--accent {
  color: var(--accent);
}

.instance-status--spin .instance-status__icon {
  animation: instance-status-spin 1s linear infinite;
}

@keyframes instance-status-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
