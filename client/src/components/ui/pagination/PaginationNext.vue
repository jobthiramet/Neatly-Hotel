<script setup lang="ts">
import type { PaginationNextProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { PaginationNext } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<PaginationNextProps & { class?: HTMLAttributes['class'] }>()
const delegatedProps = reactiveOmit(props, 'class')
</script>

<template>
  <PaginationNext
    data-slot="pagination-next"
    aria-label="Next page"
    v-bind="delegatedProps"
    :class="cn('flex size-8 cursor-pointer items-center justify-center rounded-sm text-body2 text-gray-700 outline-none is-hover:text-black is-focus:ring-2 is-focus:ring-ring disabled:cursor-not-allowed disabled:text-gray-400', props.class)"
  >
    <slot>&gt;</slot>
  </PaginationNext>
</template>
