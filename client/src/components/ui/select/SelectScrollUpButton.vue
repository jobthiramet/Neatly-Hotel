<script setup lang="ts">
import type { SelectScrollUpButtonProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { ChevronUpIcon } from '@lucide/vue'
import { reactiveOmit } from '@vueuse/core'
import { SelectScrollUpButton, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<SelectScrollUpButtonProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

const forwardedProps = useForwardProps(delegatedProps)
</script>

<template>
  <SelectScrollUpButton
    data-slot="select-scroll-up-button"
    v-bind="forwardedProps"
    :class="cn('z-10 flex cursor-default items-center justify-center bg-white py-1 text-gray-600 [&_svg]:size-4', props.class)"
  >
    <slot>
      <ChevronUpIcon />
    </slot>
  </SelectScrollUpButton>
</template>
