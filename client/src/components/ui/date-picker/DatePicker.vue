<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import type { HTMLAttributes } from 'vue'
import { DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { useVModel } from '@vueuse/core'
import { computed } from 'vue'
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
  class?: HTMLAttributes['class']
}>(), {
  placeholder: 'Select date',
})

const emits = defineEmits<{
  (e: 'update:modelValue', payload: DateValue | undefined): void
}>()

const value = useVModel(props, 'modelValue', emits, { passive: true })

const formatter = new DateFormatter('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
const label = computed(() => value.value ? formatter.format(value.value.toDate(getLocalTimeZone())) : undefined)
</script>

<!-- Figma: input style / style=date picker (12:365) + Date Picker (106:4311) -->
<template>
  <Popover>
    <PopoverTrigger
      :id="id"
      :disabled="disabled"
      :aria-invalid="invalid || undefined"
      data-slot="date-picker-trigger"
      :class="cn(
        'flex w-full cursor-pointer items-center justify-between gap-2 rounded-sm border border-input bg-white py-3 pr-4 pl-3 text-left text-body1 tracking-normal text-black transition-colors outline-none is-focus:border-orange-500 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-600 aria-invalid:border-red data-[state=open]:border-orange-500',
        !label && 'text-gray-600',
        props.class,
      )"
    >
      <span>{{ label ?? placeholder }}</span>
      <IconCalendar class="size-6 shrink-0 text-gray-600" />
    </PopoverTrigger>
    <PopoverContent>
      <Calendar v-model="value" initial-focus />
    </PopoverContent>
  </Popover>
</template>
