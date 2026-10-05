<!-- Figma: user > payment failed -->
<script setup lang="ts">
import { useAuth } from '@clerk/vue'
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getBooking } from '@/api/bookings'
import { IconErrorCircle } from '@/components/icons'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import {
  confirmOpenSession,
  confirmParkedCardCheckout,
  isCardCheckoutParked,
  releaseCardCheckout,
} from '@/lib/cardCheckout'
import { apiErrorMessage } from '@/stores/hotel'

const route = useRoute()
const router = useRouter()
const { getToken } = useAuth()
const submitting = ref(false)
const notice = ref('')
const bookingId = computed(() => String(route.params.bookingId || route.query.bookingId || ''))

onBeforeUnmount(releaseCardCheckout)

function blockWhilePaying(event: MouseEvent) {
  if (submitting.value)
    event.preventDefault()
}

async function retryPayment() {
  if (submitting.value || !bookingId.value || bookingId.value === 'unknown')
    return
  submitting.value = true
  notice.value = ''
  try {
    const result = isCardCheckoutParked()
      ? await confirmParkedCardCheckout()
      : await confirmSavedSession()
    if (!result)
      return
    if (result.ok) {
      releaseCardCheckout()
      await router.push({ name: 'booking-success', params: { bookingId: bookingId.value } })
      return
    }
    notice.value = result.message
  }
  finally {
    submitting.value = false
  }
}

async function confirmSavedSession() {
  const token = await getToken.value?.()
  if (!token) {
    notice.value = 'Please sign in to retry payment.'
    return null
  }
  try {
    const booking = await getBooking(token, bookingId.value)
    if (booking.status === 'CONFIRMED')
      return { ok: true as const }
    if (!booking.clientSecret) {
      notice.value = 'Card details are no longer available. Go back to payment details.'
      return null
    }
    return await confirmOpenSession(booking.clientSecret)
  }
  catch (error) {
    notice.value = apiErrorMessage(error, 'Could not retry this payment.')
    return null
  }
}
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
          type="button"
          variant="ghost"
          :disabled="submitting"
          :aria-busy="submitting"
          @click="retryPayment"
        >
          {{ submitting ? 'Paying…' : 'Retry' }}
        </Button>
        <Button as-child>
          <RouterLink
            :to="{ name: 'booking', query: { ...route.query, step: '3' } }"
            :aria-disabled="submitting || undefined"
            @click="blockWhilePaying"
          >
            Back to Payment details
          </RouterLink>
        </Button>
      </div>
      <p v-if="notice" class="mt-4 text-center text-body2 font-normal text-red" role="alert">
        {{ notice }}
      </p>
    </main>
    <SiteFooter />
  </div>
</template>
