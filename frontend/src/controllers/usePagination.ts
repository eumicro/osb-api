import { computed, ref, watch, type Ref } from "vue";

export const DEFAULT_PAGE_SIZE = 10;
export const PAGE_SIZE_OPTIONS = [10, 25, 50] as const;

/**
 * Controller: clientseitige Pagination ueber eine Item-Liste.
 */
export function usePagination<T>(
  items: () => readonly T[],
  options: { initialPageSize?: number } = {},
) {
  const page = ref(1);
  const pageSize = ref(options.initialPageSize ?? DEFAULT_PAGE_SIZE);

  const total = computed(() => items().length);
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value) || 1));

  const pageItems = computed(() => {
    const start = (page.value - 1) * pageSize.value;
    return items().slice(start, start + pageSize.value) as T[];
  });

  const rangeStart = computed(() =>
    total.value === 0 ? 0 : (page.value - 1) * pageSize.value + 1,
  );
  const rangeEnd = computed(() => Math.min(page.value * pageSize.value, total.value));

  const canPrev = computed(() => page.value > 1);
  const canNext = computed(() => page.value < pageCount.value);

  watch([total, pageSize], () => {
    if (page.value > pageCount.value) {
      page.value = pageCount.value;
    }
  });

  function prev() {
    if (canPrev.value) page.value -= 1;
  }

  function next() {
    if (canNext.value) page.value += 1;
  }

  function setPageSize(size: number) {
    pageSize.value = size;
    page.value = 1;
  }

  return {
    page: page as Ref<number>,
    pageSize: pageSize as Ref<number>,
    pageCount,
    total,
    pageItems,
    rangeStart,
    rangeEnd,
    canPrev,
    canNext,
    prev,
    next,
    setPageSize,
  };
}
