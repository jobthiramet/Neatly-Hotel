<!-- Figma: change date (76:1749 desktop, 7423:4415 mobile) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { useAuth } from '@clerk/vue'
import { computed, ref, shallowRef, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
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
import { nightsBetween } from '@/data/booking'
import { apiErrorMessage } from '@/stores/hotel'
import { useBookingStore } from '@/stores/booking'

const route = useRoute()
const router = useRouter()
const { getToken, isLoaded } = useAuth()
const bookingStore = useBookingStore()
const formatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const bookingId = computed(() => String(route.params.bookingId || ''))
const booking = computed(() => bookingStore.getBooking(bookingId.value))

const loading = ref(true)
const error = ref('')
const checkIn = shallowRef<DateValue>()
const checkOut = shallowRef<DateValue>()
const originalCheckIn = shallowRef<DateValue>()
const originalCheckOut = shallowRef<DateValue>()
const confirmationOpen = ref(false)
const isSaving = ref(false)

function formatDate(value: DateValue | undefined) {
  if (!value)
    return ''
  return formatter.format(value.toDate(getLocalTimeZone()))
}

const originalNights = computed(() => {
  if (!originalCheckIn.value || !originalCheckOut.value)
    return 1
  return nightsBetween(originalCheckIn.value, originalCheckOut.value)
})
const requiredCheckOut = computed(() => checkIn.value?.add({ days: originalNights.value }))

const dateError = computed(() => {
  if (!checkIn.value || !checkOut.value)
    return ''
  if (nightsBetween(checkIn.value, checkOut.value) !== originalNights.value)
    return `The new stay must keep the same ${originalNights.value === 1 ? 'night' : `${originalNights.value} nights`}.`
  return ''
})

const hasChanged = computed(() =>
  !!checkIn.value
  && !!checkOut.value
  && !!originalCheckIn.value
  && !!originalCheckOut.value
  && (checkIn.value.compare(originalCheckIn.value) !== 0
    || checkOut.value.compare(originalCheckOut.value) !== 0),
)
const canSubmit = computed(() =>
  hasChanged.value
  && !dateError.value
  && !isSaving.value
  && booking.value?.status === 'within-24h',
)

watch([checkIn, checkOut], () => {
  error.value = ''
})

// The stay length is fixed, so check-out always follows check-in.
watch(checkIn, () => {
  if (requiredCheckOut.value)
    checkOut.value = requiredCheckOut.value
})

async function sessionToken() {
  const tokenFn = getToken.value
  return typeof tokenFn === 'function' ? await tokenFn() : null
}

async function loadBooking() {
  loading.value = true
  error.value = ''
  const token = await sessionToken()
  if (!token) {
    loading.value = false
    error.value = 'Please sign in to view this booking.'
    return
  }
  try {
    await bookingStore.loadOne(token, bookingId.value)
    const loaded = bookingStore.getBooking(bookingId.value)
    if (loaded) {
      originalCheckIn.value = loaded.checkIn.date
      originalCheckOut.value = loaded.checkOut.date
      checkIn.value = loaded.checkIn.date
      checkOut.value = loaded.checkOut.date
    }
  }
  catch (cause) {
    error.value = apiErrorMessage(cause, 'Could not load this booking.')
  }
  finally {
    loading.value = false
  }
}

function requestConfirmation() {
  if (!canSubmit.value)
    return
  confirmationOpen.value = true
}

async function confirmChange() {
  if (!checkIn.value || !checkOut.value || !booking.value)
    return
  isSaving.value = true
  error.value = ''
  const token = await sessionToken()
  if (!token) {
    isSaving.value = false
    confirmationOpen.value = false
    error.value = 'Please sign in to change this booking.'
    return
  }
  try {
    await bookingStore.changeDates(
      token,
      booking.value.id,
      checkIn.value.toString(),
      checkOut.value.toString(),
    )
    confirmationOpen.value = false
    await router.push({ name: 'booking-history', query: { bookingId: booking.value.id } })
  }
  catch (cause) {
    confirmationOpen.value = false
    error.value = apiErrorMessage(cause, 'Could not change these dates.')
  }
  finally {
    isSaving.value = false
  }
}

watch(isLoaded, (ready) => {
  if (ready)
    void loadBooking()
}, { immediate: true })
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />

    <main class="flex-1">
      <p
        v-if="loading"
        class="mx-auto max-w-280 px-4 pt-10 text-body1 text-gray-700 lg:pt-20"
        role="status"
      >
        Loading booking…
      </p>

      <p
        v-else-if="error && !booking"
        class="mx-auto max-w-280 px-4 pt-10 text-body1 text-red lg:pt-20"
        role="alert"
      >
        {{ error }}
      </p>

      <section
        v-else-if="booking"
        aria-labelledby="change-date-title"
        class="mx-auto max-w-280 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32"
      >
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

      <p
        v-if="error"
        class="mt-4 text-body1 text-red"
        role="alert"
      >
        {{ error }}
      </p>

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
                {{ formatDate(originalCheckIn) }}&nbsp; - &nbsp;{{ formatDate(originalCheckOut) }}
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
                    :min-value="requiredCheckOut"
                    :max-value="requiredCheckOut"
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
