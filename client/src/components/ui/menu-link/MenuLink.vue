<script setup lang="ts">
import type { PrimitiveProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { Primitive } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<PrimitiveProps & {
  /** Current page / selected item. */
  active?: boolean
  class?: HTMLAttributes['class']
}>(), {
  as: 'a',
})
</script>

<!--
  Figma: menu link (80:1381) — default · hover · selected. Admin sidebar item.
  Pass `as-child` to wrap a <RouterLink>. Put the icon in the default slot before the label.
-->
<template>
  <Primitive
    data-slot="menu-link"
    :as="as"
    :as-child="asChild"
    :data-active="active || undefined"
    :aria-current="active ? 'page' : undefined"
    :class="cn(
      'flex w-60 cursor-pointer items-center gap-4 bg-green-800 p-6 text-body1 font-medium text-green-300 transition-colors outline-none is-hover:bg-green-700 is-focus:bg-green-700 data-active:bg-green-600 data-active:text-green-100 [&_svg]:size-6 [&_svg]:shrink-0 [&_svg]:text-green-500 data-active:[&_svg]:text-green-400',
      props.class,
    )"
  >
    <slot />
  </Primitive>
</template>
