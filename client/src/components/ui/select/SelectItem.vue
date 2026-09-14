<script setup lang="ts">
import type { SelectItemProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { SelectItem, SelectItemText, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<SelectItemProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

const forwardedProps = useForwardProps(delegatedProps)
</script>

<!--
  Figma: dropdown list (101:2444).
  Highlighted (gray-100) and selected (gray-900) states are code-only — not drawn in Figma.
-->
<template>
  <SelectItem
    data-slot="select-item"
    v-bind="forwardedProps"
    :class="
      cn(
        'relative flex w-full cursor-pointer items-center bg-white px-4 py-2 text-body1 tracking-normal text-gray-700 outline-hidden select-none data-highlighted:bg-gray-100 data-[state=checked]:text-gray-900 data-disabled:pointer-events-none data-disabled:text-gray-500',
        props.class,
      )
    "
  >
    <SelectItemText>
      <slot />
    </SelectItemText>
  </SelectItem>
</template>
