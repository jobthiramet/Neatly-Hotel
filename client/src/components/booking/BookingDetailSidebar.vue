<!-- Figma: user > booking > booking detail card -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { IconBooking } from '@/components/icons'
import {
  BOOKING_POLICIES,
  CHECK_IN_TIME_TEXT,
  CHECK_OUT_TIME_TEXT,
  formatThb,
} from '@/data/booking'

const props = defineProps<{
  remainingSeconds: number
  checkIn: DateValue
  checkOut: DateValue
  guests: number
  lineItems: { label: string, amount: number }[]
  total: number
}>()

const formatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

function formatDate(value: DateValue) {
  return formatter.format(value.toDate(getLocalTimeZone()))
}

function formatTimer(total: number) {
  const clamped = Math.max(0, total)
  const minutes = Math.floor(clamped / 60)
  const seconds = clamped % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const guestsLabel = () => (props.guests === 1 ? '1 Guest' : `${props.guests} Guests`)
</script>

<template>
  <div class="flex flex-col gap-4">
    <aside class="rounded-sm bg-green-800 p-6 text-white" aria-labelledby="booking-detail-title">
      <header class="flex items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <IconBooking class="size-6 shrink-0" />
          <h2 id="booking-detail-title" class="text-h5">
            Booking Detail
          </h2>
        </div>
        <p
          class="rounded-sm bg-orange-100 px-2 py-0.5 text-body3 font-medium tabular-nums text-orange-500"
          aria-live="polite"
          :aria-label="`Booking hold time remaining ${formatTimer(remainingSeconds)}`"
        >
          {{ formatTimer(remainingSeconds) }}
        </p>
      </header>

      <div class="mt-6 grid grid-cols-2 gap-4">
        <div>
          <p class="text-body2 text-green-400">
            Check-in
          </p>
          <p class="mt-1 text-body1">
            {{ CHECK_IN_TIME_TEXT }}
          </p>
        </div>
        <div>
          <p class="text-body2 text-green-400">
            Check-out
          </p>
          <p class="mt-1 text-body1">
            {{ CHECK_OUT_TIME_TEXT }}
          </p>
        </div>
      </div>

      <p class="mt-4 text-body2 text-green-300">
        {{ formatDate(checkIn) }} - {{ formatDate(checkOut) }}
      </p>
      <p class="mt-1 text-body2 text-green-300">
        {{ guestsLabel() }}
      </p>

      <ul class="mt-6 flex flex-col gap-3 border-t border-green-600 pt-6">
        <li
          v-for="item in lineItems"
          :key="item.label"
          class="flex items-start justify-between gap-4 text-body1 text-green-300"
        >
          <span>{{ item.label }}</span>
          <span class="shrink-0 tabular-nums text-white">{{ formatThb(item.amount) }}</span>
        </li>
      </ul>

      <div class="mt-6 flex items-center justify-between gap-4 border-t border-green-600 pt-6">
        <span class="text-body1">Total</span>
        <span class="text-h5 tabular-nums">THB {{ formatThb(total) }}</span>
      </div>
    </aside>

    <div class="rounded-sm bg-gray-200 p-4">
      <ul class="flex flex-col gap-3 text-body3 font-normal text-gray-700">
        <li v-for="policy in BOOKING_POLICIES" :key="policy" class="flex gap-2">
          <span class="mt-1.5 size-1.5 shrink-0 rounded-full bg-orange-500" aria-hidden="true" />
          <span>{{ policy }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>
