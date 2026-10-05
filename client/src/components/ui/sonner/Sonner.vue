<script lang="ts" setup>
import type { ToasterProps } from 'vue-sonner'
import { reactiveOmit } from '@vueuse/core'
import { Toaster as Sonner } from 'vue-sonner'
import { IconCheck, IconErrorCircle } from '@/components/icons'
import { cn } from '@/lib/utils'

const props = defineProps<ToasterProps>()
const delegatedProps = reactiveOmit(props, 'class', 'toastOptions')
</script>

<!-- shadcn-vue sonner, restyled with Neatly tokens. Mount once in App.vue; call `toast()` from vue-sonner. -->
<template>
  <Sonner
    :class="cn('group', props.class)"
    :style="{
      '--normal-bg': 'var(--color-white)',
      '--normal-text': 'var(--color-gray-900)',
      '--normal-border': 'var(--color-gray-300)',
      '--border-radius': 'var(--radius-sm)',
    }"
    :toast-options="props.toastOptions ?? {
      classes: {
        toast: 'font-sans text-body2 shadow-md',
        icon: 'text-green-600',
      },
    }"
    v-bind="delegatedProps"
  >
    <template #success-icon>
      <IconCheck class="size-4 text-green-600" />
    </template>
    <template #error-icon>
      <IconErrorCircle class="size-4 text-red" />
    </template>
  </Sonner>
</template>
