<script setup lang="ts">
import type { CheckboxRootEmits, CheckboxRootProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { IconCheck } from '@/components/icons'
import { reactiveOmit } from '@vueuse/core'
import { CheckboxIndicator, CheckboxRoot, useForwardPropsEmits } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<CheckboxRootProps & { class?: HTMLAttributes['class'] }>()
const emits = defineEmits<CheckboxRootEmits>()

const delegatedProps = reactiveOmit(props, 'class')

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<!-- Figma: checkbox (51:853) — pair with <Label> in a `group/checkbox` wrapper for the label states. -->
<template>
  <CheckboxRoot
    v-slot="slotProps"
    data-slot="checkbox"
    v-bind="forwarded"
    :class="cn('peer relative flex size-6 shrink-0 cursor-pointer items-center justify-center rounded-sm border border-gray-400 bg-white text-white transition-colors outline-none is-hover:border-orange-500 is-focus:ring-2 is-focus:ring-ring is-focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-gray-100 data-checked:border-orange-300 data-checked:bg-orange-500', props.class)"
  >
    <CheckboxIndicator
      data-slot="checkbox-indicator"
      class="grid place-content-center text-current"
    >
      <slot v-bind="slotProps">
        <IconCheck class="size-6" />
      </slot>
    </CheckboxIndicator>
  </CheckboxRoot>
</template>
