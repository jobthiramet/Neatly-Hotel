<!-- Figma: change date (76:1749 desktop, 7423:4415 mobile) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone, parseDate } from '@internationalized/date'
import { computed, ref, shallowRef, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import roomImage from '@/assets/home/room-superior-garden-view.webp'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { FormField } from '@/components/ui/form-field'

interface MockBooking {
  id: string
  roomName: string
  roomImage: string
  roomImageAlt: string
  bookedAt: DateValue
  checkIn: DateValue
  checkOut: DateValue
}

const route = useRoute()
const formatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const booking: MockBooking = {
  id: String(route.params.bookingId),
  roomName: 'Superior Garden View',
  roomImage,
  roomImageAlt: 'Superior Garden View room overlooking the mountains',
  bookedAt: parseDate('2026-10-16'),
  checkIn: parseDate('2026-10-19'),
  checkOut: parseDate('2026-10-22'),
}

const checkIn = shallowRef<DateValue>(booking.checkIn)
const checkOut = shallowRef<DateValue>(booking.checkOut)
const confirmationOpen = ref(false)
const isSaving = ref(false)
const successMessage = ref('')

function formatDate(value: DateValue) {
  return formatter.format(value.toDate(getLocalTimeZone()))
}

function nightsBetween(start: DateValue, end: DateValue) {
  const millisecondsPerDay = 86_400_000
  return Math.round(
    (end.toDate('UTC').getTime() - start.toDate('UTC').getTime()) / millisecondsPerDay,
  )
}

const originalNights = nightsBetween(booking.checkIn, booking.checkOut)
const minimumCheckOut = computed(() => checkIn.value.add({ days: 1 }))
const maximumCheckOut = computed(() => checkIn.value.add({ days: originalNights }))

const dateError = computed(() => {
  if (checkOut.value.compare(checkIn.value) <= 0) {
    return 'Check-out must be after check-in.'
  }

  if (nightsBetween(checkIn.value, checkOut.value) > originalNights) {
    return `The new stay cannot exceed ${originalNights} nights.`
  }

  return ''
})

const hasChanged = computed(() =>
  checkIn.value.compare(booking.checkIn) !== 0
  || checkOut.value.compare(booking.checkOut) !== 0,
)
const canSubmit = computed(() => hasChanged.value && !dateError.value && !isSaving.value)

watch([checkIn, checkOut], () => {
  successMessage.value = ''
})

function requestConfirmation() {
  if (!canSubmit.value) {
    return
  }

  confirmationOpen.value = true
}

async function confirmChange() {
  isSaving.value = true
  successMessage.value = ''

  await new Promise(resolve => setTimeout(resolve, 600))

  confirmationOpen.value = false
  isSaving.value = false
  successMessage.value = `Booking ${booking.id} dates updated for this demo.`
}
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />

    <main class="flex-1">
      <section aria-labelledby="change-date-title" class="mx-auto max-w-280 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32">
      <h1
        id="change-date-title"
        class="max-w-142 font-serif text-h3 text-green-800 lg:text-h2 lg:text-green-700"
      >
        <span class="lg:hidden">
          Change<br>
          Check-in and<br>
          Check-out Date
        </span>
        <span class="hidden lg:inline">
          Change Check-in<br>
          and Check-out Date
        </span>
      </h1>

      <article class="-mx-4 mt-6 border-b border-gray-300 pb-6 lg:mx-0 lg:mt-14 lg:py-10">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:gap-12">
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

            <section aria-labelledby="original-date-title">
              <h3 id="original-date-title" class="text-body1 font-semibold text-gray-800">
                Original Date
              </h3>
              <p class="mt-1 text-body1 text-gray-700">
                {{ formatDate(booking.checkIn) }}&nbsp; - &nbsp;{{ formatDate(booking.checkOut) }}
              </p>
            </section>

            <form
              class="flex flex-col gap-4 rounded-sm bg-white p-4"
              aria-labelledby="date-form-title"
              @submit.prevent="requestConfirmation"
            >
              <h3 id="date-form-title" class="text-body1 font-semibold text-gray-800">
                Change Date
              </h3>

              <div class="flex flex-col gap-6 lg:flex-row lg:items-center">
                <FormField label="Check In" for="check-in">
                  <DatePicker
                    id="check-in"
                    v-model="checkIn"
                    placeholder="Select check-in date"
                  />
                </FormField>

                <span class="hidden text-body1 text-black lg:block" aria-hidden="true">-</span>

                <FormField
                  label="Check Out"
                  for="check-out"
                  :error="dateError"
                >
                  <DatePicker
                    id="check-out"
                    v-model="checkOut"
                    placeholder="Select check-out date"
                    :min-value="minimumCheckOut"
                    :max-value="maximumCheckOut"
                    :invalid="!!dateError"
                    aria-describedby="check-out-error"
                  />
                </FormField>
              </div>
            </form>
          </div>
        </div>

        <div class="flex flex-col gap-6 px-4 lg:flex-row-reverse lg:items-center lg:justify-between lg:px-0">
          <Button type="button" :disabled="!canSubmit" @click="requestConfirmation">
            Confirm Change Date
          </Button>
          <Button variant="ghost" as-child>
            <RouterLink to="/booking-history">Cancel</RouterLink>
          </Button>
        </div>

        <p
          aria-live="polite"
          class="mt-6 px-4 text-body1 font-medium text-green-700 lg:px-0"
        >
          {{ successMessage }}
        </p>
      </article>
    </section>

    <Dialog v-model:open="confirmationOpen">
      <DialogContent class="w-11/12 sm:w-full">
        <DialogHeader>
          <DialogTitle>Change Date</DialogTitle>
        </DialogHeader>
        <DialogDescription>
          Are you sure you want to change your check-in and check-out date?
        </DialogDescription>
        <DialogFooter>
          <DialogClose as-child>
            <Button variant="secondary" :disabled="isSaving">No, I don't</Button>
          </DialogClose>
          <Button :disabled="isSaving" @click="confirmChange">
            {{ isSaving ? 'Saving...' : 'Yes, I want to change' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
    </main>

    <SiteFooter />
  </div>
</template>
