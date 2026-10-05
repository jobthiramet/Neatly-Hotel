<script lang="ts" setup>
import type { CalendarRootEmits, CalendarRootProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CalendarRoot, useForwardPropsEmits } from 'reka-ui'
import { cn } from '@/lib/utils'
import { CalendarCell, CalendarCellTrigger, CalendarGrid, CalendarGridBody, CalendarGridHead, CalendarGridRow, CalendarHeadCell, CalendarHeader, CalendarHeading, CalendarNextButton, CalendarPrevButton } from '.'

const props = withDefaults(defineProps<CalendarRootProps & {
  /** Header year select range when `minValue`/`maxValue` aren't set. */
  yearRange?: [number, number]
  class?: HTMLAttributes['class']
}>(), {
  modelValue: undefined,
  weekdayFormat: 'narrow',
  weekStartsOn: 1,
  fixedWeeks: true,
  locale: 'en',
})
const emits = defineEmits<CalendarRootEmits>()

const delegatedProps = reactiveOmit(props, 'class', 'yearRange')

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<!-- Figma: Date Picker (106:4311) -->
<template>
  <CalendarRoot
    v-slot="{ grid, weekDays }"
    v-bind="forwarded"
    data-slot="calendar"
    :class="cn('w-64 bg-white', props.class)"
  >
    <CalendarHeader>
      <CalendarHeading :year-range="yearRange" />
      <nav class="flex items-center gap-2">
        <CalendarPrevButton />
        <CalendarNextButton />
      </nav>
    </CalendarHeader>

    <div class="flex flex-col gap-4 sm:flex-row">
      <CalendarGrid v-for="month in grid" :key="month.value.toString()">
        <CalendarGridHead>
          <CalendarGridRow class="px-4">
            <CalendarHeadCell v-for="day in weekDays" :key="day">
              {{ day }}
            </CalendarHeadCell>
          </CalendarGridRow>
        </CalendarGridHead>
        <CalendarGridBody class="block px-4 pb-2">
          <CalendarGridRow v-for="(weekDates, index) in month.rows" :key="`weekDate-${index}`">
            <CalendarCell
              v-for="weekDate in weekDates"
              :key="weekDate.toString()"
              :date="weekDate"
            >
              <CalendarCellTrigger
                :day="weekDate"
                :month="month.value"
              />
            </CalendarCell>
          </CalendarGridRow>
        </CalendarGridBody>
      </CalendarGrid>
    </div>
  </CalendarRoot>
</template>
