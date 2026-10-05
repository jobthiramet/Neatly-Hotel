<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'

const props = defineProps<{
  defaultValue?: string
  modelValue?: string
  class?: HTMLAttributes['class']
}>()

const emits = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

const modelValue = useVModel(props, 'modelValue', emits, {
  passive: true,
  defaultValue: props.defaultValue,
})
</script>

<!-- Figma: input style, multi-line (admin / hotel information). Same states as Input. -->
<template>
  <textarea
    v-model="modelValue"
    data-slot="textarea"
    :class="cn(
      'w-full min-w-0 rounded-sm border border-input bg-white py-2.75 pr-3.75 pl-2.75 text-body1 tracking-normal text-black transition-colors outline-none placeholder:text-gray-600 is-focus:border-orange-500 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-600 aria-invalid:border-red',
      props.class,
    )"
  />
</template>
