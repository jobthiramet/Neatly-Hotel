<script setup lang="ts">
import { AuthenticateWithRedirectCallback } from '@clerk/vue'
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-bg px-6">
    <p class="text-body1 text-gray-700">Signing you in...</p>
    <AuthenticateWithRedirectCallback />
  </main>
</template>
