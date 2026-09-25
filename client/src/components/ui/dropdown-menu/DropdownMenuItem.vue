<script setup lang="ts">
import type { DropdownMenuItemProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { DropdownMenuItem, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<DropdownMenuItemProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
const forwardedProps = useForwardProps(delegatedProps)
</script>

<!-- Figma: drop down - user (28:2286). Pass `as-child` to wrap a <RouterLink>. -->
<template>
  <DropdownMenuItem
    data-slot="dropdown-menu-item"
    v-bind="forwardedProps"
    :class="cn(
      'flex w-full cursor-pointer items-center gap-3 px-4 py-3 text-body1 tracking-normal text-gray-700 outline-hidden select-none data-highlighted:bg-gray-100 data-highlighted:text-gray-900 data-disabled:pointer-events-none data-disabled:text-gray-500 [&_svg]:size-5 [&_svg]:shrink-0 [&_svg]:text-gray-600',
      props.class,
    )"
  >
    <slot />
  </DropdownMenuItem>
</template>
