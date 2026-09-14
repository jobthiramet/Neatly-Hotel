<script setup lang="ts">
import type { PrimitiveProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import type { BadgeVariants, RoomStatus } from '.'
import { reactiveOmit } from '@vueuse/core'
import { Primitive } from 'reka-ui'
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { badgeVariants, roomStatusTone } from '.'

const props = defineProps<PrimitiveProps & {
  tone?: BadgeVariants['tone']
  /** Shortcut: pick the tone from a room status and use it as the label. */
  status?: RoomStatus
  class?: HTMLAttributes['class']
}>()

const delegatedProps = reactiveOmit(props, 'class', 'tone', 'status')
const resolvedTone = computed(() => props.tone ?? (props.status ? roomStatusTone[props.status] : undefined))
</script>

<template>
  <Primitive
    data-slot="badge"
    :data-tone="resolvedTone"
    :class="cn(badgeVariants({ tone: resolvedTone }), props.class)"
    v-bind="delegatedProps"
  >
    <slot>{{ status }}</slot>
  </Primitive>
</template>
