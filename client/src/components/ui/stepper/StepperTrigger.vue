<script lang="ts" setup>
import type { StepperTriggerProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { StepperTrigger, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<StepperTriggerProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

const forwarded = useForwardProps(delegatedProps)
</script>

<template>
  <StepperTrigger
    v-bind="forwarded"
    :class="cn('flex cursor-pointer items-center gap-4 rounded-sm text-left outline-none is-focus:ring-2 is-focus:ring-ring is-focus:ring-offset-2', props.class)"
  >
    <slot />
  </StepperTrigger>
</template>
