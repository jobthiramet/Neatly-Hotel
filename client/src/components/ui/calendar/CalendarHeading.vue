<script lang="ts" setup>
import type { CalendarHeadingProps } from 'reka-ui'
import type { HTMLAttributes, VNode } from 'vue'
import { computed } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CalendarHeading as RekaCalendarHeading, injectCalendarRootContext, useForwardProps } from 'reka-ui'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'

const props = defineProps<CalendarHeadingProps & { class?: HTMLAttributes['class'] }>()

defineSlots<{
  default: (props: { headingValue: string }) => VNode[]
}>()

const delegatedProps = reactiveOmit(props, 'class')

const forwardedProps = useForwardProps(delegatedProps)
const calendar = injectCalendarRootContext()
const months = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
]
const selectedMonth = computed({
  get: () => String(calendar.placeholder.value.month),
  set: (value: string) => calendar.onPlaceholderChange(calendar.placeholder.value.set({ month: Number(value) })),
})

const selectedYear = computed({
  get: () => String(calendar.placeholder.value.year),
  set: (value: string) => calendar.onPlaceholderChange(calendar.placeholder.value.set({ year: Number(value) })),
})

const years = computed(() => {
  const currentYear = new Date().getFullYear()
  const minYear = calendar.minValue.value?.year ?? 1900
  const maxYear = calendar.maxValue.value?.year ?? currentYear + 10

  return Array.from(
    { length: Math.max(0, maxYear - minYear + 1) },
    (_, index) => maxYear - index,
  )
})
</script>

<template>
  <RekaCalendarHeading
    v-slot="{ headingValue }"
    data-slot="calendar-heading"
    :class="cn('text-body2 tracking-normal text-gray-800', props.class)"
    v-bind="forwardedProps"
  >
    <slot :heading-value>
      <div class="flex items-center gap-1">
        <Select v-model="selectedMonth">
          <SelectTrigger class="w-auto border-0 bg-transparent px-1 py-1 text-body2 text-gray-800">
            <SelectValue :placeholder="headingValue" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="(month, index) in months" :key="month" :value="String(index + 1)">
              {{ month }}
            </SelectItem>
          </SelectContent>
        </Select>
        <Select v-model="selectedYear">
          <SelectTrigger class="w-auto border-0 bg-transparent px-1 py-1 text-body2 text-gray-800">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="year in years" :key="year" :value="String(year)">
              {{ year }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>
    </slot>
  </RekaCalendarHeading>
</template>
