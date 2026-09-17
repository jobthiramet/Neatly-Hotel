<!-- Figma: user > thank you for booking -->
<script setup lang="ts">
import { DateFormatter, getLocalTimeZone, parseDate } from '@internationalized/date'
import { useAuth } from '@clerk/vue'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getBooking, type BookingResponse } from '@/api/bookings'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { formatThb } from '@/data/booking'

const route = useRoute()
const { getToken } = useAuth()
const booking = ref<BookingResponse>()
const error = ref('')
const formatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const bookingId = computed(() => String(route.params.bookingId || ''))

function formatDate(value: string) {
  return formatter.format(parseDate(value.slice(0, 10)).toDate(getLocalTimeZone()))
}

const stayLabel = computed(() => {
  if (!booking.value)
    return ''
  const guests = booking.value.guests === 1 ? '1 Guest' : `${booking.value.guests} Guests`
  return `${formatDate(booking.value.checkIn)} - ${formatDate(booking.value.checkOut)}\n${guests}`
})

onMounted(async () => {
  const token = await getToken.value?.()
  if (!token) {
    error.value = 'Please sign in to view this booking.'
    return
  }
  try {
    booking.value = await getBooking(token, bookingId.value)
  }
  catch {
    error.value = 'Could not load this booking.'
  }
})
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />
    <main class="flex flex-1 justify-center px-4 py-10 lg:py-20">
      <section
        v-if="booking"
        aria-labelledby="booking-success-title"
        class="w-full max-w-200 bg-green-800 px-6 py-10 text-white lg:px-10 lg:py-12"
      >
        <h1 id="booking-success-title" class="text-center font-serif text-h3 lg:text-h2">
          Thank you for booking
        </h1>
        <p class="mt-3 text-center text-body2 font-normal text-green-300">
          We are looking forward to hosting you at our place.
          We will send you more information about check-in and staying at our Neatly closer to your date of reservation
        </p>

        <div class="mt-10 grid gap-6 bg-green-700 p-6 text-body1 sm:grid-cols-3">
          <p class="whitespace-pre-line text-green-200">
            {{ stayLabel }}
          </p>
          <div>
            <p class="text-green-300">Check-in</p>
            <p>{{ booking.checkInTimeText }}</p>
          </div>
          <div>
            <p class="text-green-300">Check-out</p>
            <p>{{ booking.checkOutTimeText }}</p>
          </div>
        </div>

        <p class="mt-8 text-center text-body1">
          <template v-if="booking.paymentMethod === 'CASH'">
            {{ booking.paymentMethodText }}
          </template>
          <template v-else>
            Payment success via
            <span class="font-semibold">{{ booking.paymentMethodText.replace('Payment success via ', '') }}</span>
          </template>
        </p>

        <dl class="mt-10 flex flex-col gap-3 text-body1">
          <div
            v-for="item in booking.items"
            :key="item.code + item.label"
            class="flex justify-between gap-4"
            :class="item.kind === 'DISCOUNT' ? 'text-green-300' : ''"
          >
            <dt>{{ item.label }}</dt>
            <dd>{{ formatThb(item.amount) }}</dd>
          </div>
          <div class="mt-2 flex justify-between border-t border-green-600 pt-4 text-h5">
            <dt>Total</dt>
            <dd>THB {{ formatThb(booking.grandTotal) }}</dd>
          </div>
        </dl>
      </section>
      <p v-else-if="error" class="text-body1 text-red" role="alert">
        {{ error }}
      </p>
      <p v-else class="text-body1 text-gray-700" role="status">
        Loading booking…
      </p>
    </main>

    <div
      v-if="booking"
      class="flex flex-wrap items-center justify-center gap-6 px-4 pb-16"
    >
      <Button variant="ghost" as-child>
        <RouterLink :to="{ name: 'booking-history', query: { bookingId: booking.id } }">
          Check Booking Detail
        </RouterLink>
      </Button>
      <Button as-child>
        <RouterLink to="/">Back to Home</RouterLink>
      </Button>
    </div>
    <SiteFooter />
  </div>
</template>
