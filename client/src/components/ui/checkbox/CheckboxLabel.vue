<script setup lang="ts">
import type { LabelProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { Label } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<LabelProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
</script>

<!-- Figma: checkbox label — place directly after <Checkbox> (uses the `peer` state). -->
<template>
  <Label
    data-slot="checkbox-label"
    v-bind="delegatedProps"
    :class="cn(
      'cursor-pointer text-body1 tracking-normal text-gray-700 select-none peer-is-hover:text-orange-500 peer-data-checked:font-medium peer-data-checked:text-gray-900 peer-disabled:cursor-not-allowed peer-disabled:text-gray-500',
      props.class,
    )"
  >
    <slot />
  </Label>
</template>
