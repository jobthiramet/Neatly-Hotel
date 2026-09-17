<script setup lang="ts">
import type { TabsContentProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { TabsContent } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<TabsContentProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
</script>

<template>
  <TabsContent
    data-slot="tabs-content"
    v-bind="delegatedProps"
    :class="cn('rounded-sm outline-none is-focus:ring-2 is-focus:ring-ring', props.class)"
  >
    <slot />
  </TabsContent>
</template>
