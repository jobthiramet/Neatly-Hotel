<script setup lang="ts">
import { AuthenticateWithRedirectCallback } from '@clerk/vue'
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-bg px-6">
    <p class="text-body1 text-gray-700">Signing you in...</p>
    <AuthenticateWithRedirectCallback
      sign-in-url="/sign-in"
      sign-up-url="/sign-up"
      continue-sign-up-url="/sign-up?oauth=1"
      sign-in-fallback-redirect-url="/"
      sign-up-fallback-redirect-url="/sign-up?oauth=1"
    />
  </main>
</template>
