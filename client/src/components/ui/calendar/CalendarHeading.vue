<script lang="ts" setup>
import type { CalendarHeadingProps } from 'reka-ui'
import type { HTMLAttributes, VNode } from 'vue'
import { computed } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CalendarHeading as RekaCalendarHeading, injectCalendarRootContext, useForwardProps } from 'reka-ui'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'

const props = defineProps<CalendarHeadingProps & {
  /** Years offered when `minValue`/`maxValue` aren't set. Defaults to this year → +2. */
  yearRange?: [number, number]
  class?: HTMLAttributes['class']
}>()

defineSlots<{
  default: (props: { headingValue: string }) => VNode[]
}>()

const delegatedProps = reactiveOmit(props, 'class', 'yearRange')

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
  const minValueYear = calendar.minValue.value?.year
  const maxValueYear = calendar.maxValue.value?.year
  // minValue/maxValue win. Otherwise `yearRange`, else this year → +2 (bookings),
  // or 100 years back when only maxValue is set (e.g. date of birth).
  const fromYear = props.yearRange?.[0] ?? (maxValueYear === undefined ? currentYear : maxValueYear - 100)
  const toYear = props.yearRange?.[1] ?? currentYear + 2
  // Always include the visible year so the select never shows a blank value.
  const visibleYear = calendar.placeholder.value.year
  const minYear = minValueYear ?? Math.min(fromYear, visibleYear)
  const maxYear = maxValueYear ?? Math.max(toYear, visibleYear)

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
