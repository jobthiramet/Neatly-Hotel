<script setup lang="ts">
import type { TabsTriggerProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { TabsTrigger, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<TabsTriggerProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
const forwardedProps = useForwardProps(delegatedProps)
</script>

<!-- Put an icon in the default slot before the label. -->
<template>
  <TabsTrigger
    data-slot="tabs-trigger"
    v-bind="forwardedProps"
    :class="cn(
      'flex shrink-0 cursor-pointer flex-col items-center gap-3 rounded-sm border-b-2 border-transparent px-2 pt-2 pb-3 text-center text-body1 text-green-300 transition-colors outline-none is-hover:text-white is-focus:ring-2 is-focus:ring-ring data-[state=active]:border-orange-500 data-[state=active]:text-white [&_svg]:shrink-0',
      props.class,
    )"
  >
    <slot />
  </TabsTrigger>
</template>
