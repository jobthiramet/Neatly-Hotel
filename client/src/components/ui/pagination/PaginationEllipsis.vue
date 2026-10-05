<script setup lang="ts">
import type { PaginationEllipsisProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { PaginationEllipsis } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<PaginationEllipsisProps & { class?: HTMLAttributes['class'] }>()
const delegatedProps = reactiveOmit(props, 'class')
</script>

<template>
  <PaginationEllipsis
    data-slot="pagination-ellipsis"
    v-bind="delegatedProps"
    :class="cn('flex size-8 items-center justify-center text-body2 text-gray-700', props.class)"
  >
    <slot>…</slot>
  </PaginationEllipsis>
</template>
