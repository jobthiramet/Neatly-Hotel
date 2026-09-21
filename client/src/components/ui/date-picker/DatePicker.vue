<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import type { HTMLAttributes, Ref } from 'vue'
import { DateFormatter, getLocalTimeZone, today } from '@internationalized/date'
import { useVModel } from '@vueuse/core'
import { computed, ref, shallowRef } from 'vue'
import { IconCalendar } from '@/components/icons'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  modelValue?: DateValue
  id?: string
  placeholder?: string
  disabled?: boolean
  invalid?: boolean
  minValue?: DateValue
  maxValue?: DateValue
  /** `compact` drops the weekday ("21 Sept 2026"), for rows that are tight on width. */
  format?: 'weekday' | 'compact'
  /** Header year select range when `minValue`/`maxValue` aren't set. */
  yearRange?: [number, number]
  ariaDescribedby?: string
  class?: HTMLAttributes['class']
}>(), {
  placeholder: 'Select date',
})

const emits = defineEmits<{
  (e: 'update:modelValue', payload: DateValue | undefined): void
}>()

const value = useVModel(props, 'modelValue', emits, { passive: true }) as Ref<DateValue | undefined>

// The month shown in the calendar. Owned here (two-way) so the header's month/year
// selects and the arrows can move it; a one-way binding reverted every change.
const open = ref(false)
const visibleMonth = shallowRef<DateValue>(today(getLocalTimeZone()))

function onOpenChange(isOpen: boolean) {
  if (isOpen)
    visibleMonth.value = value.value ?? props.maxValue ?? today(getLocalTimeZone())
  open.value = isOpen
}

function onSelect(date: DateValue | undefined) {
  value.value = date
  open.value = false
}

const formatters = {
  weekday: new DateFormatter('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }),
  compact: new DateFormatter('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
}
const label = computed(() => value.value
  ? formatters[props.format ?? 'weekday'].format(value.value.toDate(getLocalTimeZone()))
  : undefined)
</script>

<!-- Figma: input style / style=date picker (12:365) + Date Picker (106:4311) -->
<template>
  <Popover :open="open" @update:open="onOpenChange">
    <PopoverTrigger as-child>
      <button
        type="button"
        :id="id"
        :disabled="disabled"
        :aria-invalid="invalid || undefined"
        :aria-describedby="ariaDescribedby"
        data-slot="date-picker-trigger"
        :class="cn(
          'flex w-full cursor-pointer items-center justify-between gap-2 rounded-sm border border-input bg-white py-2.75 pr-3.75 pl-2.75 text-left text-body1 tracking-normal whitespace-nowrap text-black transition-colors outline-none is-focus:border-orange-500 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-600 aria-invalid:border-red data-[state=open]:border-orange-500',
          !label && 'text-gray-600',
          props.class,
        )"
      >
        <span>{{ label ?? placeholder }}</span>
        <IconCalendar class="size-6 shrink-0 text-gray-600" />
      </button>
    </PopoverTrigger>
    <PopoverContent>
      <Calendar
        v-model:placeholder="visibleMonth"
        :model-value="value"
        :min-value="minValue"
        :max-value="maxValue"
        :year-range="yearRange"
        initial-focus
        @update:model-value="onSelect"
      />
    </PopoverContent>
  </Popover>
</template>
