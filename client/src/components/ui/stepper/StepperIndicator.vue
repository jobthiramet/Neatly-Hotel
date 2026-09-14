<script lang="ts" setup>
import type { StepperIndicatorProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { StepperIndicator, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<StepperIndicatorProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

const forwarded = useForwardProps(delegatedProps)
</script>

<!-- Figma: step (35:1029) — none · current · finish -->
<template>
  <StepperIndicator
    v-slot="slotProps"
    v-bind="forwarded"
    :class="cn('inline-flex w-16.5 items-center justify-center rounded-sm bg-gray-200 py-3 text-h4 text-gray-600 group-data-[state=active]:bg-orange-500 group-data-[state=active]:text-white group-data-[state=completed]:bg-orange-100 group-data-[state=completed]:text-orange-500', props.class)"
  >
    <slot v-bind="slotProps" />
  </StepperIndicator>
</template>
