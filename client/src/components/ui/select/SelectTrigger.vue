<script setup lang="ts">
import type { SelectTriggerProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { SelectIcon, SelectTrigger, useForwardProps } from 'reka-ui'
import { IconCaretDown } from '@/components/icons'
import { cn } from '@/lib/utils'

const props = defineProps<SelectTriggerProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
const forwardedProps = useForwardProps(delegatedProps)
</script>

<!-- Figma: input style / style=dropdown (12:349) -->
<template>
  <SelectTrigger
    data-slot="select-trigger"
    v-bind="forwardedProps"
    :class="cn(
      'flex w-full cursor-pointer items-center justify-between gap-2 rounded-sm border border-input bg-white py-2.75 pr-3.75 pl-2.75 text-left text-body1 tracking-normal whitespace-nowrap text-black transition-colors outline-none select-none is-focus:border-orange-500 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-600 aria-invalid:border-red data-placeholder:text-gray-600 *:data-[slot=select-value]:line-clamp-1',
      props.class,
    )"
  >
    <slot />
    <SelectIcon as-child>
      <IconCaretDown class="pointer-events-none size-5 shrink-0 text-gray-600" />
    </SelectIcon>
  </SelectTrigger>
</template>
