<script setup lang="ts">
import type { DropdownMenuTriggerProps } from 'reka-ui'
import { DropdownMenuTrigger } from 'reka-ui'

const props = defineProps<DropdownMenuTriggerProps>()
</script>

<!-- Reka sets aria-haspopup="menu" and aria-expanded on the trigger. -->
<template>
  <DropdownMenuTrigger
    data-slot="dropdown-menu-trigger"
    v-bind="props"
  >
    <slot />
  </DropdownMenuTrigger>
</template>
