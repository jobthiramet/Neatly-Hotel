<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { nextTick, ref } from 'vue'
import { ImageUpload } from '@/components/ui/image-upload'
import { cn } from '@/lib/utils'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  /** Image URLs and picked files, in display order. */
  modelValue: (string | File)[]
  id: string
  accept?: string
  /** Hides the upload tile once reached. */
  max?: number
  class?: HTMLAttributes['class']
}>(), {
  accept: 'image/*',
  max: Infinity,
})

const emit = defineEmits<{
  (e: 'update:modelValue', payload: (string | File)[]): void
}>()

const dragIndex = ref<number | null>(null)

// Stable keys so tiles keep their DOM (and object-URL previews) while reordering.
const fileKeys = new WeakMap<File, number>()
let nextKey = 0
function keyOf(item: string | File) {
  if (typeof item === 'string')
    return item
  if (!fileKeys.has(item))
    fileKeys.set(item, nextKey++)
  return `file-${fileKeys.get(item)}`
}

function move(from: number, to: number) {
  if (to < 0 || to >= props.modelValue.length || from === to)
    return
  const next = [...props.modelValue]
  next.splice(to, 0, ...next.splice(from, 1))
  emit('update:modelValue', next)
}

function remove(index: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== index))
}

function add(file: string | File | null) {
  if (file)
    emit('update:modelValue', [...props.modelValue, file])
}

function onDrop(index: number) {
  if (dragIndex.value !== null)
    move(dragIndex.value, index)
  dragIndex.value = null
}

// Keyboard alternative to dragging: Alt + ←/→ on a focused tile.
function onKeydown(event: KeyboardEvent, index: number) {
  if (!event.altKey || (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight'))
    return
  event.preventDefault()
  const to = index + (event.key === 'ArrowLeft' ? -1 : 1)
  move(index, to)
  const tiles = (event.currentTarget as HTMLElement).parentElement?.children
  nextTick(() => (tiles?.[to] as HTMLElement | undefined)?.focus())
}
</script>

<!--
  Figma: admin / room & property / image gallery — ImageUpload tiles that can be dragged to reorder, plus an upload tile.
  File type, size and count checks are left to the parent form.
-->
<template>
  <ul data-slot="image-gallery-upload" :class="cn('flex flex-wrap gap-6', props.class)">
    <li
      v-for="(item, index) in modelValue"
      :key="keyOf(item)"
      draggable="true"
      tabindex="0"
      :aria-label="`Image ${index + 1} of ${modelValue.length}. Drag or press Alt and arrow keys to reorder.`"
      :class="cn('cursor-grab rounded-sm outline-none is-focus:ring-2 is-focus:ring-orange-500', dragIndex === index && 'opacity-50')"
      @dragstart="dragIndex = index"
      @dragend="dragIndex = null"
      @dragover.prevent
      @drop.prevent="onDrop(index)"
      @keydown="onKeydown($event, index)"
    >
      <ImageUpload
        :id="`${id}-${index}`"
        class="size-36"
        :model-value="item"
        @update:model-value="value => value === null && remove(index)"
      />
    </li>
    <li v-if="modelValue.length < max">
      <ImageUpload :id="id" v-bind="$attrs" class="size-36" :accept="accept" :model-value="null" @update:model-value="add" />
    </li>
  </ul>
</template>
