import { parseDate } from '@internationalized/date'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cancelBooking, changeBookingDates, getBooking, listBookings, type BookingResponse } from '@/api/bookings'
import type { BookingStatus, PriceBreakdownItem, UserBooking } from '@/data/booking'
import superiorGardenImage from '@/assets/home/room-superior-garden-view.webp'

/** Asia/Bangkok has no DST; matches server HOTEL_ZONE check-in at 14:00. */
const BANGKOK_OFFSET = '+07:00'
const HOUR_MS = 3_600_000
const DAY_MS = 24 * HOUR_MS

export function checkInAtMs(checkIn: string) {
  return Date.parse(`${checkIn}T14:00:00${BANGKOK_OFFSET}`)
}

export function refundDeadlineMs(checkIn: string) {
  return checkInAtMs(checkIn) - DAY_MS
}

export function uiStatus(booking: BookingResponse, now = Date.now()): BookingStatus {
  if (booking.status === 'CANCELLED')
    return 'cancelled'
  if (booking.status === 'CHECKED_IN' || booking.status === 'CHECKED_OUT' || booking.status === 'COMPLETED')
    return 'checked-in'
  const checkInMs = checkInAtMs(booking.checkIn)
  if (now >= checkInMs)
    return 'checked-in'
  if (checkInMs - now <= DAY_MS)
    return 'checkin-soon'
  if (now - Date.parse(booking.createdAt) <= DAY_MS)
    return 'within-24h'
  return 'after-24h'
}

function unitLabel(count: number, singular: string, plural: string) {
  return `${count} ${count === 1 ? singular : plural}`
}

export function refundCountdownText(status: BookingStatus, deadlineMs: number | undefined, now: number) {
  if (status === 'checked-in' || status === 'cancelled' || deadlineMs == null)
    return null
  if (status === 'checkin-soon' || now >= deadlineMs)
    return { text: 'Refund no longer available', urgent: false as const }
  const remaining = deadlineMs - now
  const days = Math.floor(remaining / DAY_MS)
  const hours = Math.floor((remaining % DAY_MS) / HOUR_MS)
  const minutes = Math.floor((remaining % HOUR_MS) / 60_000)
  let span: string
  if (days > 0)
    span = hours > 0
      ? `${unitLabel(days, 'day', 'days')} ${unitLabel(hours, 'hour', 'hours')}`
      : unitLabel(days, 'day', 'days')
  else if (hours > 0)
    span = unitLabel(hours, 'hour', 'hours')
  else
    span = unitLabel(Math.max(minutes, 1), 'minute', 'minutes')
  return { text: `Refund available for ${span}`, urgent: remaining < HOUR_MS }
}

export function toUserBooking(booking: BookingResponse, now = Date.now()): UserBooking {
  return {
    id: booking.id,
    roomName: booking.roomName,
    roomImage: booking.roomImageUrl || superiorGardenImage,
    roomImageAlt: booking.roomName,
    bookedAt: parseDate(booking.createdAt.slice(0, 10)),
    cancellationDate: booking.cancelledAt ? parseDate(booking.cancelledAt.slice(0, 10)) : undefined,
    checkIn: {
      date: parseDate(booking.checkIn),
      timeText: booking.checkInTimeText,
    },
    checkOut: {
      date: parseDate(booking.checkOut),
      timeText: booking.checkOutTimeText,
    },
    guestsText: booking.guests === 1 ? '1 Guest' : `${booking.guests} Guests`,
    nightsText: booking.nights === 1 ? '1 Night' : `${booking.nights} Nights`,
    paymentMethodText: booking.paymentMethodText,
    paymentMethod: booking.paymentMethod,
    breakdown: booking.items.map((item): PriceBreakdownItem => ({
      label: item.label,
      amount: item.amount,
      isDiscount: item.kind === 'DISCOUNT',
    })),
    totalPrice: booking.grandTotal,
    additionalRequest: booking.additionalRequest || undefined,
    status: uiStatus(booking, now),
    refundDeadlineMs: refundDeadlineMs(booking.checkIn),
  }
}

export const useBookingStore = defineStore('booking', () => {
  const records = ref<BookingResponse[]>([])
  const loading = ref(false)
  const error = ref('')
  const nowMs = ref(Date.now())

  const bookings = computed(() => records.value.map(booking => toUserBooking(booking, nowMs.value)))

  function tickClock(at = Date.now()) {
    nowMs.value = at
  }

  async function loadMine(token: string) {
    loading.value = true
    error.value = ''
    try {
      const page = await listBookings(token, 0, 50)
      records.value = page.content
    }
    catch (cause) {
      error.value = cause instanceof Error ? cause.message : 'Could not load bookings.'
      records.value = []
    }
    finally {
      loading.value = false
    }
  }

  async function loadOne(token: string, id: string) {
    const booking = await getBooking(token, id)
    upsert(booking)
    return booking
  }

  async function cancelMine(token: string, id: string) {
    const booking = await cancelBooking(token, id)
    upsert(booking)
    return booking
  }

  async function changeDates(token: string, id: string, checkIn: string, checkOut: string) {
    const booking = await changeBookingDates(token, id, { checkIn, checkOut })
    upsert(booking)
    return booking
  }

  function upsert(booking: BookingResponse) {
    const index = records.value.findIndex(item => item.id === booking.id)
    if (index >= 0)
      records.value[index] = booking
    else
      records.value = [booking, ...records.value]
  }

  function getBookingRecord(id: string) {
    return records.value.find(item => item.id === id)
  }

  function getBookingView(id: string): UserBooking | undefined {
    const record = getBookingRecord(id)
    return record ? toUserBooking(record, nowMs.value) : undefined
  }

  return {
    records,
    bookings,
    nowMs,
    loading,
    error,
    tickClock,
    loadMine,
    loadOne,
    getBooking: getBookingView,
    getBookingRecord,
    cancelBooking: cancelMine,
    changeDates,
  }
})
