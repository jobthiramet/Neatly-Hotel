<script setup lang="ts">
import type { AdminBookingDetail } from '@/api/adminBookings'
import { getAdminBooking } from '@/api/adminBookings'
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { apiErrorMessage } from '@/stores/hotel'
import { BED_TYPE_LABELS } from '@/stores/rooms'

const props = defineProps<{
  id: string
}>()

const booking = ref<AdminBookingDetail | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const stayDateFormat = new Intl.DateTimeFormat('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const money = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function formatStayDate(iso: string) {
  const [year, month, day] = iso.split('-').map(Number)
  if (!year || !month || !day)
    return iso
  return stayDateFormat.format(new Date(Date.UTC(year, month - 1, day)))
}

function formatBookingDate(iso: string) {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime()))
    return iso
  return stayDateFormat.format(date)
}

function formatMoney(amount: number) {
  return money.format(amount)
}

function amountLabel(count: number) {
  return `${count} ${count === 1 ? 'room' : 'rooms'}`
}

function nightsLabel(nights: number) {
  return `${nights} ${nights === 1 ? 'night' : 'nights'}`
}

function bedLabel(detail: AdminBookingDetail) {
  return detail.bedType ? BED_TYPE_LABELS[detail.bedType] : '—'
}

function lineAmountClass(kind: string) {
  return kind === 'DISCOUNT' ? 'text-gray-600' : 'text-gray-800'
}

async function load() {
  loading.value = true
  error.value = null
  booking.value = null
  try {
    booking.value = await getAdminBooking(props.id)
  }
  catch (e) {
    const message = apiErrorMessage(e, 'Could not load booking detail.')
    error.value = message
    toast.error(message)
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.id, load)
</script>

<template>
  <Teleport defer to="#admin-header-title">
    <div class="flex min-w-0 items-center gap-4">
      <Button
        variant="ghost"
        size="icon"
        class="shrink-0 text-gray-700"
        as-child
      >
        <RouterLink :to="{ name: 'admin-customer-bookings' }" aria-label="Back to Customer Booking">
          <svg class="size-6" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M15 6 9 12l6 6"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </RouterLink>
      </Button>
      <div v-if="booking" class="min-w-0 truncate text-h5 text-black">
        <span class="font-medium">{{ booking.customerName }}</span>
        <span class="mx-2 text-gray-500"> </span>
        <span class="font-normal text-gray-800">{{ booking.roomType }}</span>
      </div>
    </div>
  </Teleport>

  <div
    v-if="loading"
    role="status"
    class="rounded-sm bg-white px-6 py-10 text-body1 text-gray-700 lg:px-20"
  >
    Loading booking…
  </div>

  <div
    v-else-if="error"
    role="alert"
    class="flex flex-col items-start gap-4 rounded-sm bg-white px-6 py-10 lg:px-20"
  >
    <p class="text-body1 text-red">
      {{ error }}
    </p>
    <Button variant="secondary" @click="load">
      Try again
    </Button>
  </div>

  <div
    v-else-if="booking"
    class="rounded-sm bg-white px-6 py-8 shadow-md lg:px-20 lg:py-10"
  >
    <dl class="flex flex-col gap-6 text-body1">
      <div>
        <dt class="text-gray-600">
          Customer name
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ booking.customerName }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Guest(s)
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ booking.guests }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Room type
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ booking.roomType }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Amount
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ amountLabel(booking.roomsCount) }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Bed type
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ bedLabel(booking) }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Check-in
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ formatStayDate(booking.checkIn) }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Check-out
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ formatStayDate(booking.checkOut) }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Stay (total)
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ nightsLabel(booking.nights) }}
        </dd>
      </div>
      <div>
        <dt class="text-gray-600">
          Booking date
        </dt>
        <dd class="mt-1 text-gray-800">
          {{ formatBookingDate(booking.bookingDate) }}
        </dd>
      </div>
    </dl>

    <div class="mt-8 rounded-sm bg-gray-100 px-4 py-5">
      <p class="text-right text-body1 text-gray-700">
        {{ booking.paymentMethodText }}
      </p>
      <ul class="mt-4 flex flex-col gap-3">
        <li
          v-for="(item, index) in booking.items"
          :key="`${item.kind}-${item.code}-${index}`"
          class="flex items-center justify-between gap-4 text-body1"
          :class="lineAmountClass(item.kind)"
        >
          <span>{{ item.label }}</span>
          <span class="shrink-0 tabular-nums">
            {{ item.kind === 'DISCOUNT' ? `-${formatMoney(Math.abs(item.amount))}` : formatMoney(item.amount) }}
          </span>
        </li>
      </ul>
      <div class="mt-4 flex items-center justify-between gap-4 border-t border-gray-300 pt-4 text-body1 font-semibold text-black">
        <span>Total</span>
        <span class="shrink-0 tabular-nums">
          {{ booking.currency }} {{ formatMoney(booking.grandTotal) }}
        </span>
      </div>
    </div>

    <div class="mt-6 rounded-sm bg-gray-200 px-4 py-4 text-body1">
      <p class="font-semibold text-gray-800">
        Additional Request
      </p>
      <p class="mt-1 text-gray-700">
        {{ booking.additionalRequest || '—' }}
      </p>
    </div>
  </div>
</template>
