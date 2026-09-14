<!-- Figma: cancel and refund (64:2116 desktop, 7423:4746 mobile) -->
<!-- Figma: refund success (66:1306 desktop, 7423:4989 mobile) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone, today } from '@internationalized/date'
import { ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import roomImage from '@/assets/home/room-superior-garden-view.webp'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface MockRefundableBooking {
  id: string
  roomName: string
  roomImage: string
  roomImageAlt: string
  bookedAt: DateValue
  checkIn: DateValue
  checkOut: DateValue
  guests: number
  totalRefund: number
}

const route = useRoute()
const currentDate = today(getLocalTimeZone())
const dateFormatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})
const moneyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'THB',
  currencyDisplay: 'code',
})

const booking: MockRefundableBooking = {
  id: String(route.params.bookingId),
  roomName: 'Superior Garden View',
  roomImage,
  roomImageAlt: 'Superior Garden View room overlooking the mountains',
  bookedAt: currentDate.subtract({ days: 1 }),
  checkIn: currentDate.add({ days: 14 }),
  checkOut: currentDate.add({ days: 17 }),
  guests: 2,
  totalRefund: 2300,
}

const confirmationOpen = ref(false)
const isSubmitting = ref(false)
const isComplete = ref(false)

function formatDate(value: DateValue) {
  return dateFormatter.format(value.toDate(getLocalTimeZone()))
}

function formatMoney(value: number) {
  return moneyFormatter.format(value)
}

async function confirmCancellation() {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true

  await new Promise(resolve => setTimeout(resolve, 600))

  confirmationOpen.value = false
  isSubmitting.value = false
  isComplete.value = true
}
</script>

<template>
  <main class="min-h-screen bg-bg">
    <section
      v-if="!isComplete"
      aria-labelledby="refund-title"
      class="mx-auto max-w-280 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32"
    >
      <h1
        id="refund-title"
        class="font-serif text-h3 text-green-800 lg:text-h2 lg:text-green-700"
      >
        Request a Refund
      </h1>

      <article class="-mx-4 mt-6 border-b border-gray-300 pb-6 lg:mx-0 lg:mt-12">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:gap-12 lg:py-10">
          <img
            :src="booking.roomImage"
            :alt="booking.roomImageAlt"
            width="1600"
            height="1067"
            class="h-55.25 w-full rounded-sm object-cover lg:h-52.5 lg:w-89.25"
          >

          <div class="flex min-w-0 flex-1 flex-col gap-8 px-4 pb-6 lg:px-0 lg:pb-6">
            <header class="flex flex-col gap-1 lg:flex-row lg:items-center lg:justify-between">
              <h2 class="text-h4 text-black">{{ booking.roomName }}</h2>
              <p class="text-body1 text-gray-600">
                Booking date: {{ formatDate(booking.bookedAt) }}
              </p>
            </header>

            <div class="flex flex-col gap-6 lg:flex-row lg:items-end">
              <section aria-label="Booking stay details" class="flex flex-1 flex-col text-body1 text-gray-700">
                <p>
                  {{ formatDate(booking.checkIn) }}&nbsp; - &nbsp;{{ formatDate(booking.checkOut) }}
                </p>
                <p class="mt-1">{{ booking.guests }} Guests</p>
              </section>

              <section aria-label="Refund amount" class="lg:w-55.5 lg:text-right">
                <p class="text-body1 text-gray-900">Total Refund</p>
                <p class="mt-1 text-h5 text-gray-900">{{ formatMoney(booking.totalRefund) }}</p>
              </section>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-6 px-4 lg:flex-row-reverse lg:items-center lg:justify-between lg:border-t lg:border-gray-300 lg:px-0 lg:py-10">
          <Button type="button" @click="confirmationOpen = true">
            Cancel and Refund this Booking
          </Button>
          <Button variant="ghost" as-child>
            <RouterLink to="/">Cancel</RouterLink>
          </Button>
        </div>
      </article>
    </section>

    <section
      v-else
      aria-labelledby="refund-success-title"
      aria-live="polite"
      class="mx-auto flex max-w-184.5 flex-col items-center gap-10 lg:gap-15 lg:pt-20"
    >
      <article class="w-full overflow-hidden bg-green-700 shadow-md lg:rounded-sm">
        <header class="flex flex-col items-center gap-3 bg-green-800 px-4 py-10 text-center lg:px-6">
          <h1 id="refund-success-title" class="font-serif text-h3 text-white">
            Your Request has been Submitted
          </h1>
          <p class="text-body2 text-green-400">
            The cancellation is complete.<br>
            You will receive an email with details of your refund within 48 hours.
          </p>
        </header>

        <div class="flex flex-col gap-10 px-4 pt-6 pb-10 lg:px-10">
          <section
            aria-labelledby="refund-booking-title"
            class="flex flex-col gap-10 rounded-sm bg-green-600 p-4 lg:p-6"
          >
            <div>
              <h2 id="refund-booking-title" class="text-h5 text-white">
                {{ booking.roomName }}
              </h2>
              <p class="mt-4 text-body1 font-semibold text-white">
                {{ formatDate(booking.checkIn) }}&nbsp; - &nbsp;{{ formatDate(booking.checkOut) }}
              </p>
              <p class="mt-1 text-body1 text-white">{{ booking.guests }} Guests</p>
            </div>

            <div class="flex flex-col gap-2 text-body1 text-green-300">
              <p>Booking date: {{ formatDate(booking.bookedAt) }}</p>
              <p>Cancellation date: {{ formatDate(currentDate) }}</p>
            </div>
          </section>

          <section
            aria-label="Refund total"
            class="flex items-baseline justify-between border-t border-green-600 pt-6"
          >
            <p class="text-body1 text-green-300">Total Refund</p>
            <p class="text-h5 text-white">{{ formatMoney(booking.totalRefund) }}</p>
          </section>
        </div>
      </article>

      <Button as-child>
        <RouterLink to="/">Back to Home</RouterLink>
      </Button>
    </section>

    <Dialog v-model:open="confirmationOpen">
      <DialogContent class="w-11/12 sm:w-full">
        <DialogHeader>
          <DialogTitle>Cancel Booking</DialogTitle>
        </DialogHeader>
        <DialogDescription>
          Are you sure you would like to cancel this booking?
        </DialogDescription>
        <DialogFooter>
          <Button
            variant="secondary"
            class="w-full sm:w-auto"
            :disabled="isSubmitting"
            @click="confirmCancellation"
          >
            {{ isSubmitting ? 'Submitting...' : 'Yes, I want to cancel and request refund' }}
          </Button>
          <DialogClose as-child>
            <Button class="w-full sm:w-auto" :disabled="isSubmitting">
              No, Don't Cancel
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </main>
</template>
