<!-- Figma: user > cancel and refund & cancel booking (desktop & mobile) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone, parseDate } from '@internationalized/date'
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { useBookingStore } from '@/stores/booking'

const props = defineProps<{
  refund?: boolean
}>()

const route = useRoute()
const router = useRouter()
const bookingStore = useBookingStore()

const bookingId = String(route.params.bookingId || 'b-101')
const booking = computed(() => bookingStore.getBooking(bookingId) || bookingStore.bookings[0]!)

// If prop isn't passed, deduce from route or booking status
const isRefund = computed(() => {
  if (props.refund !== undefined) return props.refund
  if (route.name === 'booking-cancel' || booking.value.status === 'checkin-soon') {
    return false
  }
  return true
})

const isSubmitted = ref(false)
const isSubmitting = ref(false)

const formatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

interface FormattableDate {
  toDate: (timeZone: string) => Date
}

function formatDate(value: FormattableDate | DateValue | undefined | null) {
  if (!value) return ''
  return formatter.format(value.toDate(getLocalTimeZone()))
}

function formatMoney(amount: number) {
  return amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

const cancellationDate = parseDate('2022-10-16')

async function handleConfirmCancellation() {
  isSubmitting.value = true
  await new Promise(resolve => setTimeout(resolve, 400))
  bookingStore.cancelBooking(booking.value.id, cancellationDate)
  isSubmitting.value = false
  isSubmitted.value = true
}

function goBackToHome() {
  router.push('/')
}
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />

    <main class="flex-1">
      <!-- Cancellation Request Form (Before Confirmation) -->
      <section
        v-if="!isSubmitted"
        aria-labelledby="cancel-title"
        class="mx-auto max-w-280 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32"
      >
        <h1
          id="cancel-title"
          class="font-serif text-h3 text-green-800 lg:text-h2 lg:text-green-700"
        >
          {{ isRefund ? 'Request a Refund' : 'Cancel Booking' }}
        </h1>

        <article class="-mx-4 mt-6 border-b border-gray-300 pb-6 lg:mx-0 lg:mt-14 lg:py-10">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:gap-12">
            <!-- Room Image -->
            <img
              :src="booking.roomImage"
              :alt="booking.roomImageAlt"
              width="1600"
              height="1067"
              class="h-55.25 w-full rounded-sm object-cover lg:h-52.5 lg:w-89.25 lg:shrink-0"
            >

            <!-- Room & Stay Details -->
            <div class="flex min-w-0 flex-1 flex-col gap-6 px-4 lg:px-0">
              <header class="flex flex-col gap-1 lg:flex-row lg:items-center lg:justify-between">
                <h2 class="text-h4 text-black font-semibold">
                  {{ booking.roomName }}
                </h2>
                <p class="text-body1 text-gray-600">
                  Booking date: {{ formatDate(booking.bookedAt) }}
                </p>
              </header>

              <div class="flex flex-col gap-2">
                <p class="text-body1 text-gray-700">
                  {{ formatDate(booking.checkIn.date) }} &nbsp;-&nbsp; {{ formatDate(booking.checkOut.date) }}
                </p>
                <p class="text-body1 text-gray-700">
                  {{ booking.guestsText }}
                </p>
              </div>

              <!-- Non-refundable warning -->
              <p
                v-if="!isRefund"
                class="text-body2 text-red"
              >
                *Cancellation of the booking now will not be able to request a refund.
              </p>

              <!-- Total Refund Display (if refundable) -->
              <div
                v-if="isRefund"
                class="mt-2 flex items-center justify-between border-t border-gray-200 pt-4"
              >
                <span class="text-body1 text-gray-700">Total Refund</span>
                <span class="text-h4 font-semibold text-gray-900">
                  THB {{ formatMoney(booking.totalPrice) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Desktop Actions -->
          <div class="mt-8 hidden items-center justify-between px-4 lg:flex lg:px-0">
            <Button variant="ghost" as-child>
              <RouterLink to="/booking-history">Cancel</RouterLink>
            </Button>

            <Button
              :disabled="isSubmitting"
              @click="handleConfirmCancellation"
            >
              {{ isRefund ? 'Cancel and Refund this Booking' : 'Cancel this Booking' }}
            </Button>
          </div>

          <!-- Mobile Actions (Stacked) -->
          <div class="mt-6 flex flex-col gap-4 px-4 lg:hidden">
            <Button
              class="w-full"
              :disabled="isSubmitting"
              @click="handleConfirmCancellation"
            >
              {{ isRefund ? 'Cancel and Refund this Booking' : 'Cancel this Booking' }}
            </Button>

            <Button variant="ghost" class="w-full" as-child>
              <RouterLink to="/booking-history">Cancel</RouterLink>
            </Button>
          </div>
        </article>
      </section>

      <!-- Success / Submitted Screen (After Confirmation) -->
      <section
        v-else
        aria-labelledby="success-title"
        class="mx-auto max-w-185 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32"
      >
        <div class="rounded-sm bg-green-800 px-6 py-10 text-white lg:p-12">
          <header class="text-center">
            <h1
              id="success-title"
              class="font-serif text-h4 text-white sm:text-h3 lg:text-h2"
            >
              {{ isRefund ? 'Your Request has been Submitted' : 'The Cancellation is Complete' }}
            </h1>
            <p class="mx-auto mt-3 max-w-120 text-body1 text-green-200">
              {{
                isRefund
                  ? 'The cancellation is complete. You will recieve an email with a detail and refund within 48 hours.'
                  : 'The cancellation is complete. You will recieve an email with a detail of cancellation within 24 hours.'
              }}
            </p>
          </header>

          <!-- Inner Summary Card -->
          <div class="mt-8 rounded-sm bg-green-700/90 p-6 lg:p-8">
            <h2 class="text-h4 font-semibold text-white">
              {{ booking.roomName }}
            </h2>

            <p class="mt-4 text-body1 text-green-100">
              {{ formatDate(booking.checkIn.date) }} &nbsp;-&nbsp; {{ formatDate(booking.checkOut.date) }}
            </p>
            <p class="mt-1 text-body1 text-green-100">
              {{ booking.guestsText }}
            </p>

            <div class="mt-6 border-t border-green-600/80 pt-4 text-body1 text-green-200">
              <p>Booking date: {{ formatDate(booking.bookedAt) }}</p>
              <p class="mt-1">Cancellation date: {{ formatDate(cancellationDate) }}</p>
            </div>
          </div>

          <!-- Total Refund Row (if refund) -->
          <div
            v-if="isRefund"
            class="mt-6 flex items-center justify-between border-t border-green-700 pt-6"
          >
            <span class="text-body1 text-green-200">Total Refund</span>
            <span class="text-h4 font-semibold text-white">
              THB {{ formatMoney(booking.totalPrice) }}
            </span>
          </div>

          <!-- Back to Home Button -->
          <div class="mt-10 flex justify-center lg:justify-end">
            <Button
              class="w-full lg:w-auto"
              @click="goBackToHome"
            >
              Back to Home
            </Button>
          </div>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>
