<script setup lang="ts">
import type { LabelProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { Label } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<LabelProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
</script>

<!-- Figma: input style / label -->
<template>
  <Label
    data-slot="label"
    v-bind="delegatedProps"
    :class="
      cn(
        'flex items-center gap-2 text-body1 tracking-normal text-gray-900 select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:text-gray-600 peer-disabled:cursor-not-allowed peer-disabled:text-gray-600',
        props.class,
      )
    "
  >
    <slot />
  </Label>
</template>
