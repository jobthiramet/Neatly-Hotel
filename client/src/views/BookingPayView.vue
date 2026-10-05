<script setup lang="ts">
import type { Stripe, StripeCheckoutElementsSdk, StripePaymentElement } from '@stripe/stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import { useAuth } from '@clerk/vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { retryBookingPayment } from '@/api/bookings'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'

const route = useRoute()
const router = useRouter()
const { getToken } = useAuth()
const host = ref<HTMLElement | null>(null)
const error = ref('')
const submitting = ref(false)
let checkout: StripeCheckoutElementsSdk | null = null
let paymentElement: StripePaymentElement | null = null

function tokenColor(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

onMounted(async () => {
  const bookingId = String(route.params.bookingId || '')
  const token = await getToken.value?.()
  if (!token || !bookingId) {
    error.value = 'Please sign in to retry payment.'
    return
  }
  try {
    const booking = await retryBookingPayment(token, bookingId)
    if (booking.status === 'CONFIRMED') {
      await router.replace({ name: 'booking-success', params: { bookingId } })
      return
    }
    if (!booking.clientSecret) {
      error.value = 'Card payments are not configured.'
      return
    }
    const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY
    const stripe: Stripe | null = key ? await loadStripe(key) : null
    if (!stripe || !host.value) {
      error.value = 'Could not load Stripe.'
      return
    }
    checkout = stripe.initCheckoutElementsSdk({
      clientSecret: booking.clientSecret,
      elementsOptions: {
        appearance: {
          theme: 'stripe',
          variables: {
            colorPrimary: tokenColor('--color-orange-600'),
            colorBackground: tokenColor('--color-white'),
            colorText: tokenColor('--color-gray-800'),
            colorDanger: tokenColor('--color-red'),
            fontFamily: tokenColor('--font-sans'),
          },
        },
      },
    })
    paymentElement = checkout.createPaymentElement()
    paymentElement.mount(host.value)
  }
  catch {
    error.value = 'Could not restart this payment.'
  }
})

onBeforeUnmount(() => {
  paymentElement?.unmount()
})

async function confirm() {
  if (!checkout)
    return
  submitting.value = true
  error.value = ''
  const loaded = await checkout.loadActions()
  if (loaded.type === 'error') {
    error.value = loaded.error.message || 'Payment failed.'
    submitting.value = false
    return
  }
  const validated = await loaded.actions.validateElements()
  if (validated.type === 'error') {
    error.value = validated.error.validation_errors[0]?.message
      || validated.error.message
      || 'Check your card details.'
    submitting.value = false
    return
  }
  const result = await loaded.actions.confirm({ redirect: 'if_required' })
  submitting.value = false
  if (result.type === 'error') {
    if (result.error.code === 'paymentFailed') {
      await router.push({ name: 'booking-failed', params: { bookingId: String(route.params.bookingId) } })
      return
    }
    error.value = result.error.message || 'Check your card details.'
    return
  }
  await router.push({ name: 'booking-success', params: { bookingId: String(route.params.bookingId) } })
}
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />
    <main class="mx-auto w-full max-w-200 flex-1 px-4 py-10">
      <h1 class="font-serif text-h3 text-green-800">
        Retry payment
      </h1>
      <div ref="host" class="mt-8 min-h-20 rounded-sm bg-white p-6 shadow-md" />
      <p v-if="error" class="mt-4 text-body2 text-red" role="alert">
        {{ error }}
      </p>
      <Button class="mt-8" :disabled="submitting" @click="confirm">
        {{ submitting ? 'Paying…' : 'Confirm Booking' }}
      </Button>
    </main>
    <SiteFooter />
  </div>
</template>
