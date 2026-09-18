import { parseDate } from '@internationalized/date'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cancelBooking, changeBookingDates, getBooking, listBookings, type BookingResponse } from '@/api/bookings'
import type { BookingStatus, PriceBreakdownItem, UserBooking } from '@/data/booking'
import superiorGardenImage from '@/assets/home/room-superior-garden-view.webp'

function hoursBetween(fromMs: number, toMs: number) {
  return (toMs - fromMs) / 3_600_000
}

export function uiStatus(booking: BookingResponse, now = Date.now()): BookingStatus {
  if (booking.status === 'CANCELLED')
    return 'cancelled'
  if (booking.status === 'CHECKED_IN' || booking.status === 'COMPLETED')
    return 'checked-in'
  const checkInMs = Date.parse(`${booking.checkIn}T14:00:00`)
  if (hoursBetween(now, checkInMs) <= 24)
    return 'checkin-soon'
  if (hoursBetween(Date.parse(booking.createdAt), now) <= 24)
    return 'within-24h'
  return 'after-24h'
}

export function toUserBooking(booking: BookingResponse): UserBooking {
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
    breakdown: booking.items.map((item): PriceBreakdownItem => ({
      label: item.label,
      amount: item.amount,
      isDiscount: item.kind === 'DISCOUNT',
    })),
    totalPrice: booking.grandTotal,
    additionalRequest: booking.additionalRequest || undefined,
    status: uiStatus(booking),
  }
}

export const useBookingStore = defineStore('booking', () => {
  const records = ref<BookingResponse[]>([])
  const loading = ref(false)
  const error = ref('')

  const bookings = computed(() => records.value.map(toUserBooking))

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
    return record ? toUserBooking(record) : undefined
  }

  return {
    records,
    bookings,
    loading,
    error,
    loadMine,
    loadOne,
    getBooking: getBookingView,
    getBookingRecord,
    cancelBooking: cancelMine,
    changeDates,
  }
})
