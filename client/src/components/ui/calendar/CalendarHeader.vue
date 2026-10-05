<script lang="ts" setup>
import type { CalendarHeaderProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CalendarHeader, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<CalendarHeaderProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

const forwardedProps = useForwardProps(delegatedProps)
</script>

<template>
  <CalendarHeader
    data-slot="calendar-header"
    :class="cn('flex h-13 w-full items-center justify-between px-6 pt-4 pb-3', props.class)"
    v-bind="forwardedProps"
  >
    <slot />
  </CalendarHeader>
</template>
