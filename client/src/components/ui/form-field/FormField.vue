<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

const props = defineProps<{
  label: string
  /** id of the control inside the default slot */
  for: string
  /** Error message — shown below the control in `utility/red`. */
  error?: string
  disabled?: boolean
  class?: HTMLAttributes['class']
}>()
</script>

<!-- Figma: input style (label + input field + supporting text) -->
<template>
  <div
    data-slot="form-field"
    :data-disabled="disabled || undefined"
    :class="cn('group flex w-full flex-col gap-1', props.class)"
  >
    <Label :for="props.for">
      {{ label }}
    </Label>
    <slot />
    <p v-if="error" :id="`${props.for}-error`" class="text-body2 font-normal tracking-normal text-red">
      {{ error }}
    </p>
  </div>
</template>
