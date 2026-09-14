<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { computed, onBeforeUnmount, watch } from 'vue'
import { IconClose } from '@/components/icons'
import { cn } from '@/lib/utils'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  /** Image URL or a picked file. */
  modelValue: string | File | null
  id: string
  accept?: string
  class?: HTMLAttributes['class']
}>(), {
  accept: 'image/*',
})

const emit = defineEmits<{
  (e: 'update:modelValue', payload: string | File | null): void
}>()

const previewUrl = computed(() => {
  const value = props.modelValue
  return value instanceof File ? URL.createObjectURL(value) : value
})

// Revoke object URLs when the file is removed, replaced or the component unmounts.
watch(previewUrl, (_, old) => {
  if (old?.startsWith('blob:'))
    URL.revokeObjectURL(old)
})
onBeforeUnmount(() => {
  if (previewUrl.value?.startsWith('blob:'))
    URL.revokeObjectURL(previewUrl.value)
})

function onChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // allow re-picking the same file
  if (file)
    emit('update:modelValue', file)
}
</script>

<!--
  Figma: admin / hotel information / hotel logo — preview with remove button, or upload box.
  File type/size validation is left to the parent form.
-->
<template>
  <div data-slot="image-upload" :class="cn('relative size-41.75', props.class)">
    <template v-if="previewUrl">
      <img :src="previewUrl" alt="" class="size-full bg-gray-200 object-contain">
      <button
        type="button"
        aria-label="Remove image"
        class="absolute -top-2 -right-2 flex size-6 cursor-pointer items-center justify-center rounded-full bg-red text-white outline-none is-focus:ring-2 is-focus:ring-orange-500"
        @click="emit('update:modelValue', null)"
      >
        <IconClose class="size-6" />
      </button>
    </template>
    <label
      v-else
      :for="id"
      class="group flex size-full cursor-pointer flex-col items-center justify-center gap-2 rounded-sm bg-gray-200 text-body2 font-medium text-orange-500 transition-colors has-focus-visible:ring-2 has-focus-visible:ring-orange-500 hover:bg-gray-300 has-aria-invalid:ring-1 has-aria-invalid:ring-red"
    >
      <span aria-hidden="true" class="text-h5">+</span>
      Upload photo
      <input v-bind="$attrs" :id="id" type="file" :accept="accept" class="sr-only" @change="onChange">
    </label>
  </div>
</template>
