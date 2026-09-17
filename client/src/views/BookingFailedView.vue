<!-- Figma: user > payment failed -->
<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { IconErrorCircle } from '@/components/icons'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'

const route = useRoute()
const bookingId = computed(() => String(route.params.bookingId || route.query.bookingId || ''))
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />
    <main class="flex flex-1 flex-col items-center px-4 py-10 lg:py-20">
      <section
        aria-labelledby="payment-failed-title"
        class="flex w-full max-w-200 flex-col items-center bg-orange-100 px-6 py-16 text-center lg:px-10"
      >
        <IconErrorCircle class="size-16 text-orange-500" aria-hidden="true" />
        <h1 id="payment-failed-title" class="mt-6 font-serif text-h3 text-orange-500 lg:text-h2">
          Payment failed
        </h1>
        <p class="mt-3 max-w-140 text-body2 font-normal text-orange-500">
          There seems to be an issue with your card. Please check your card details and try again later, or use a different payment method.
        </p>
      </section>
      <div class="mt-10 flex flex-wrap items-center justify-center gap-6">
        <Button
          v-if="bookingId && bookingId !== 'unknown'"
          variant="ghost"
          as-child
        >
          <RouterLink :to="{ name: 'booking-pay', params: { bookingId } }">
            Retry
          </RouterLink>
        </Button>
        <Button as-child>
          <RouterLink :to="{ name: 'booking', query: { ...route.query, step: '3' } }">
            Back to Payment details
          </RouterLink>
        </Button>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
