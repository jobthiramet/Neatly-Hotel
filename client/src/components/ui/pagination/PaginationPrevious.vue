<script setup lang="ts">
import type { PaginationPrevProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { PaginationPrev } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<PaginationPrevProps & { class?: HTMLAttributes['class'] }>()
const delegatedProps = reactiveOmit(props, 'class')
</script>

<template>
  <PaginationPrev
    data-slot="pagination-previous"
    aria-label="Previous page"
    v-bind="delegatedProps"
    :class="cn('flex size-8 cursor-pointer items-center justify-center rounded-sm text-body2 text-gray-700 outline-none is-hover:text-black is-focus:ring-2 is-focus:ring-ring disabled:cursor-not-allowed disabled:text-gray-400', props.class)"
  >
    <slot>&lt;</slot>
  </PaginationPrev>
</template>
