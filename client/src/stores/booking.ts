import type { DateValue } from '@internationalized/date'
import { parseDate } from '@internationalized/date'
import { defineStore } from 'pinia'
import { shallowRef } from 'vue'
import type { UserBooking } from '@/data/booking'
import { mockUserBookings } from '@/data/booking'

export const useBookingStore = defineStore('booking', () => {
  const bookings = shallowRef<UserBooking[]>([...mockUserBookings])

  function getBooking(id: string): UserBooking | undefined {
    return bookings.value.find(b => b.id === id) || bookings.value[0]
  }

  function cancelBooking(id: string, cancellationDate: DateValue = parseDate('2022-10-16')) {
    bookings.value = bookings.value.map(b => {
      if (b.id === id) {
        return {
          ...b,
          status: 'cancelled',
          cancellationDate,
        }
      }
      return b
    })
  }

  function updateBookingDates(id: string, checkIn: DateValue, checkOut: DateValue) {
    bookings.value = bookings.value.map(b => {
      if (b.id === id) {
        return {
          ...b,
          checkIn: { ...b.checkIn, date: checkIn },
          checkOut: { ...b.checkOut, date: checkOut },
        }
      }
      return b
    })
  }

  return {
    bookings,
    getBooking,
    cancelBooking,
    updateBookingDates,
  }
})
