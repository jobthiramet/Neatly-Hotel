<script setup lang="ts">
defineProps<{
  id: string
  title: string
  /** Figma frame / component the section mirrors. */
  figma?: string
}>()
</script>

<template>
  <section :id="id" class="scroll-mt-8 rounded-sm bg-white p-8 shadow-md">
    <header class="mb-6 flex flex-wrap items-baseline justify-between gap-2 border-b border-gray-300 pb-4">
      <h2 class="text-h4 text-gray-900">
        {{ title }}
      </h2>
      <p v-if="figma" class="text-body3 text-gray-600">
        Figma: {{ figma }}
      </p>
    </header>
    <slot />
  </section>
</template>
