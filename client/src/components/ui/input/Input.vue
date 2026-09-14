<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { useVModel } from '@vueuse/core'
import { computed, useAttrs } from 'vue'
import { IconErrorCircle } from '@/components/icons'
import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = defineProps<{
  defaultValue?: string | number
  modelValue?: string | number
  class?: HTMLAttributes['class']
}>()

const emits = defineEmits<{
  (e: 'update:modelValue', payload: string | number): void
}>()

const modelValue = useVModel(props, 'modelValue', emits, {
  passive: true,
  defaultValue: props.defaultValue,
})

const attrs = useAttrs()
const invalid = computed(() => attrs['aria-invalid'] === true || attrs['aria-invalid'] === 'true')
</script>

<!-- Figma: input style / style=input (12:343 · 12:390 · 12:396 · 12:402 · 12:409) -->
<template>
  <div data-slot="input-wrapper" class="relative w-full">
    <input
      v-bind="$attrs"
      v-model="modelValue"
      data-slot="input"
      :class="cn(
        'w-full min-w-0 rounded-sm border border-input bg-white py-3 pr-4 pl-3 text-body1 tracking-normal text-black transition-colors outline-none placeholder:text-gray-600 is-focus:border-orange-500 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-600 aria-invalid:border-red',
        invalid && 'pr-10',
        props.class,
      )"
    >
    <IconErrorCircle
      v-if="invalid"
      class="pointer-events-none absolute top-1/2 right-4 size-4 -translate-y-1/2 text-red"
    />
  </div>
</template>
