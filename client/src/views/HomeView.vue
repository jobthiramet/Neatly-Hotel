<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="mx-auto flex min-h-screen max-w-5xl flex-col items-start justify-center gap-4 p-8">
    <h1 class="font-serif text-h3 text-green-800">Neatly</h1>
    <RouterLink to="/design-system" class="text-button font-button text-orange-500 is-hover:text-orange-400">
      Design system →
    </RouterLink>
  </main>
</template>
