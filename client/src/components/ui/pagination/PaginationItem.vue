<script setup lang="ts">
import type { PaginationListItemProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { PaginationListItem } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<PaginationListItemProps & { class?: HTMLAttributes['class'] }>()
const delegatedProps = reactiveOmit(props, 'class')
</script>

<template>
  <PaginationListItem
    data-slot="pagination-item"
    v-bind="delegatedProps"
    :class="cn(
      'flex size-8 cursor-pointer items-center justify-center rounded-sm text-body2 text-gray-700 outline-none is-hover:text-black is-focus:ring-2 is-focus:ring-ring data-selected:border data-selected:border-orange-500 data-selected:font-semibold data-selected:text-orange-500',
      props.class,
    )"
  >
    <slot>{{ value }}</slot>
  </PaginationListItem>
</template>
