<script setup lang="ts">
import type { SelectContentEmits, SelectContentProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import {
  SelectContent,
  SelectPortal,
  SelectViewport,
  useForwardPropsEmits,
} from 'reka-ui'
import { cn } from '@/lib/utils'
import { SelectScrollDownButton, SelectScrollUpButton } from '.'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<SelectContentProps & { class?: HTMLAttributes['class'] }>(),
  {
    position: 'popper',
    sideOffset: 6,
  },
)
const emits = defineEmits<SelectContentEmits>()

const delegatedProps = reactiveOmit(props, 'class')

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<!-- Figma: Drop Down - Service (101:2443) -->
<template>
  <SelectPortal>
    <SelectContent
      data-slot="select-content"
      v-bind="{ ...$attrs, ...forwarded }"
      :class="cn(
        'relative z-50 max-h-(--reka-select-content-available-height) min-w-(--reka-select-trigger-width) origin-(--reka-select-content-transform-origin) overflow-x-hidden overflow-y-auto rounded-sm bg-white py-2 shadow-md data-open:animate-in data-open:fade-in-0 data-closed:animate-out data-closed:fade-out-0',
        props.class,
      )
      "
    >
      <SelectScrollUpButton />
      <SelectViewport>
        <slot />
      </SelectViewport>
      <SelectScrollDownButton />
    </SelectContent>
  </SelectPortal>
</template>
