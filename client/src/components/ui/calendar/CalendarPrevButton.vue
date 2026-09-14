<script lang="ts" setup>
import type { CalendarPrevProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CalendarPrev, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'
import { IconChevronRight } from '@/components/icons'

const props = defineProps<CalendarPrevProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

const forwardedProps = useForwardProps(delegatedProps)
</script>

<template>
  <CalendarPrev
    data-slot="calendar-prev-button"
    :class="cn('flex size-4 cursor-pointer items-center justify-center rounded-sm text-gray-500 outline-none is-hover:text-gray-700 is-focus:ring-2 is-focus:ring-ring disabled:cursor-not-allowed disabled:opacity-40', props.class)"
    v-bind="forwardedProps"
  >
    <slot>
      <IconChevronRight class="size-4 rotate-180" />
    </slot>
  </CalendarPrev>
</template>
