<script lang="ts" setup>
import type { CalendarCellTriggerProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CalendarCellTrigger, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<CalendarCellTriggerProps & { class?: HTMLAttributes['class'] }>(), {
  as: 'button',
})

const delegatedProps = reactiveOmit(props, 'class')

const forwardedProps = useForwardProps(delegatedProps)
</script>

<!-- Figma: Desktop Date Picker cell — default · today (outline) · selected (fill) · hover (gray-200) · outside (40%) -->
<template>
  <CalendarCellTrigger
    data-slot="calendar-cell-trigger"
    :class="cn(
      'flex size-8 cursor-pointer items-center justify-center border border-transparent text-body2 font-normal tracking-normal text-gray-800 outline-none is-hover:bg-gray-200 is-focus:border-orange-500',
      // Today
      'data-today:border-orange-500',
      // Selected
      'data-selected:border-transparent data-selected:bg-orange-500 data-selected:text-white is-hover:data-selected:bg-orange-500',
      // Outside month / disabled / unavailable
      'data-outside-view:opacity-40 data-disabled:cursor-not-allowed data-disabled:text-gray-500 data-unavailable:text-gray-500 data-unavailable:line-through',
      props.class,
    )"
    v-bind="forwardedProps"
  >
    <slot />
  </CalendarCellTrigger>
</template>
