<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { computed, ref, useTemplateRef } from 'vue'
import { IconCaretDown } from '@/components/icons'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

export interface MultiSelectOption {
  value: string
  label: string
}

const props = withDefaults(defineProps<{
  options: MultiSelectOption[]
  modelValue: string[]
  /** Trigger text when nothing is selected; selecting nothing means "everything". */
  allLabel?: string
  /** Trigger text for two or more selections. */
  countLabel?: (count: number) => string
  id?: string
  disabled?: boolean
  class?: HTMLAttributes['class']
}>(), {
  allLabel: 'All options',
  countLabel: (count: number) => `${count} selected`,
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const open = ref(false)
const list = useTemplateRef<HTMLElement>('list')

const label = computed(() => {
  const selected = props.options.filter(option => props.modelValue.includes(option.value))
  if (selected.length === 0)
    return props.modelValue.length ? props.countLabel(props.modelValue.length) : props.allLabel
  if (selected.length === 1)
    return selected[0]!.label
  return props.countLabel(selected.length)
})

function toggle(value: string, checked: boolean) {
  emit('update:modelValue', checked
    ? [...props.modelValue, value]
    : props.modelValue.filter(item => item !== value))
}

// Arrow keys walk the checkboxes; Space and Escape are handled by the checkbox and the popover.
function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp')
    return
  const items = [...(list.value?.querySelectorAll<HTMLElement>('[data-slot=checkbox]') ?? [])]
  if (!items.length)
    return
  event.preventDefault()
  const current = items.indexOf(document.activeElement as HTMLElement)
  const step = event.key === 'ArrowDown' ? 1 : -1
  items[(current + step + items.length) % items.length]!.focus()
}
</script>

<!-- No Figma frame: styled after input style / style=dropdown (12:349) to match the other search fields. -->
<template>
  <Popover v-model:open="open">
    <!-- as-child keeps our own id, so the FormField label's `for` still points here. -->
    <PopoverTrigger as-child>
      <button
        :id="id"
        type="button"
        :disabled="disabled"
        :class="cn(
          'flex w-full cursor-pointer items-center justify-between gap-2 rounded-sm border border-input bg-white py-2.75 pr-3.75 pl-2.75 text-left text-body1 tracking-normal whitespace-nowrap text-black transition-colors outline-none is-focus:border-orange-500 is-focus:ring-2 is-focus:ring-ring is-focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-600',
          props.class,
        )"
      >
        <span class="line-clamp-1">{{ label }}</span>
        <IconCaretDown class="pointer-events-none size-5 shrink-0 text-gray-600" />
      </button>
    </PopoverTrigger>

    <PopoverContent align="start" class="w-(--reka-popover-trigger-width) min-w-64 p-4">
      <div
        ref="list"
        role="listbox"
        aria-multiselectable="true"
        :aria-labelledby="id"
        class="flex max-h-72 flex-col gap-3 overflow-y-auto"
        @keydown="onKeydown"
      >
        <div
          v-for="option in options"
          :key="option.value"
          role="option"
          :aria-selected="modelValue.includes(option.value)"
          class="group/checkbox flex items-center gap-3"
        >
          <Checkbox
            :id="`${id}-${option.value}`"
            :model-value="modelValue.includes(option.value)"
            @update:model-value="checked => toggle(option.value, checked === true)"
          />
          <Label :for="`${id}-${option.value}`" class="cursor-pointer font-normal">
            {{ option.label }}
          </Label>
        </div>
      </div>

      <button
        type="button"
        class="mt-4 cursor-pointer rounded-sm font-button text-button text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring disabled:text-gray-500"
        :disabled="modelValue.length === 0"
        @click="emit('update:modelValue', [])"
      >
        Clear
      </button>
    </PopoverContent>
  </Popover>
</template>
