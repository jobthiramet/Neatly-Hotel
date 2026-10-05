<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<!-- Figma: modal / top -->
<template>
  <div
    data-slot="dialog-header"
    :class="cn('flex min-h-14 items-center border-b border-gray-300 py-2 pr-18 pl-6', props.class)"
  >
    <slot />
  </div>
</template>
