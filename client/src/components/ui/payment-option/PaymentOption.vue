<script setup lang="ts">
import type { RadioGroupItemProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { RadioGroupItem, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<RadioGroupItemProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
const forwardedProps = useForwardProps(delegatedProps)
</script>

<!--
  Figma: payment option card (32:1222) — default · hover · selected.
  Use inside <RadioGroup>. Put the icon in the default slot before the label.
-->
<template>
  <RadioGroupItem
    data-slot="payment-option"
    v-bind="forwardedProps"
    :class="cn(
      'flex h-20 w-53 cursor-pointer items-center justify-center gap-2 overflow-hidden rounded-sm border border-gray-300 bg-white text-h5 text-gray-600 shadow-md transition-colors outline-none is-hover:border-orange-500 is-focus:ring-2 is-focus:ring-ring is-focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-40 data-[state=checked]:border-orange-500 data-[state=checked]:text-orange-500 [&_svg]:size-8 [&_svg]:shrink-0',
      props.class,
    )"
  >
    <slot />
  </RadioGroupItem>
</template>
