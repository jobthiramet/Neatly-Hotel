import type { DateValue } from '@internationalized/date'
import { parseDate } from '@internationalized/date'
import deluxeImage from '@/assets/home/room-deluxe.webp'
import premierImage from '@/assets/home/room-premier-sea-view.webp'
import superiorGardenImage from '@/assets/home/room-superior-garden-view.webp'

export type BookingStatus =
  | 'within-24h' // within 24 hours of booking: able to change date, able to cancel and refund
  | 'after-24h' // after 24 hours of booking: cannot change date, can cancel and refund
  | 'checkin-soon' // within 24 hours of check-in date: cancel booking will not able to refund
  | 'checked-in' // after user already checked in: all CTA disappear, only view detail
  | 'cancelled' // cancelled booking: shows cancellation date, no CTA

export interface PriceBreakdownItem {
  label: string
  amount: number
  isDiscount?: boolean
}

export interface UserBooking {
  id: string
  roomName: string
  roomImage: string
  roomImageAlt: string
  bookedAt: DateValue
  cancellationDate?: DateValue
  checkIn: {
    date: DateValue
    timeText: string
  }
  checkOut: {
    date: DateValue
    timeText: string
  }
  guestsText: string
  nightsText: string
  paymentMethodText: string
  breakdown: PriceBreakdownItem[]
  totalPrice: number
  additionalRequest?: string
  status: BookingStatus
  expandedInitially?: boolean
}

export const mockUserBookings: UserBooking[] = [
  {
    id: 'b-101',
    roomName: 'Superior Garden View',
    roomImage: superiorGardenImage,
    roomImageAlt: 'Superior Garden View room overlooking the mountains',
    bookedAt: parseDate('2022-10-16'),
    checkIn: {
      date: parseDate('2022-10-19'),
      timeText: 'After 2:00 PM',
    },
    checkOut: {
      date: parseDate('2022-10-20'),
      timeText: 'Before 12:00 PM',
    },
    guestsText: '2 Guests',
    nightsText: '1 Night',
    paymentMethodText: 'Payment success via Credit Card - *888',
    breakdown: [
      { label: 'Superior Garden View Room', amount: 2500 },
      { label: 'Airport transfer', amount: 200 },
      { label: 'Promotion Code', amount: -400, isDiscount: true },
    ],
    totalPrice: 2300,
    additionalRequest: 'Can i have some chocolate?',
    status: 'within-24h',
    expandedInitially: false,
  },
  {
    id: 'b-102',
    roomName: 'Superior Garden View',
    roomImage: superiorGardenImage,
    roomImageAlt: 'Superior Garden View room overlooking the mountains',
    bookedAt: parseDate('2022-10-16'),
    checkIn: {
      date: parseDate('2022-10-19'),
      timeText: 'After 2:00 PM',
    },
    checkOut: {
      date: parseDate('2022-10-20'),
      timeText: 'Before 12:00 PM',
    },
    guestsText: '2 Guests',
    nightsText: '1 Night',
    paymentMethodText: 'Payment success via Credit Card - *888',
    breakdown: [
      { label: 'Superior Garden View Room', amount: 2500 },
      { label: 'Airport transfer', amount: 200 },
      { label: 'Promotion Code', amount: -400, isDiscount: true },
    ],
    totalPrice: 2300,
    additionalRequest: 'Can i have some chocolate?',
    status: 'within-24h',
    expandedInitially: true,
  },
  {
    id: 'b-103',
    roomName: 'Deluxe',
    roomImage: deluxeImage,
    roomImageAlt: 'Deluxe bathroom and luxury interior',
    bookedAt: parseDate('2022-08-01'),
    checkIn: {
      date: parseDate('2022-08-06'),
      timeText: 'After 2:00 PM',
    },
    checkOut: {
      date: parseDate('2022-08-08'),
      timeText: 'Before 12:00 PM',
    },
    guestsText: '2 Guests',
    nightsText: '2 Nights',
    paymentMethodText: 'Payment success via Credit Card - *888',
    breakdown: [
      { label: 'Deluxe Room', amount: 4800 },
      { label: 'Breakfast', amount: 300 },
    ],
    totalPrice: 5100,
    additionalRequest: 'Early check-in if possible please.',
    status: 'after-24h',
    expandedInitially: false,
  },
  {
    id: 'b-104',
    roomName: 'Premier Sea View',
    roomImage: premierImage,
    roomImageAlt: 'Premier Sea View balcony overlooking the ocean',
    bookedAt: parseDate('2022-08-01'),
    checkIn: {
      date: parseDate('2022-08-06'),
      timeText: 'After 2:00 PM',
    },
    checkOut: {
      date: parseDate('2022-08-08'),
      timeText: 'Before 12:00 PM',
    },
    guestsText: '2 Guests',
    nightsText: '2 Nights',
    paymentMethodText: 'Payment success via Credit Card - *888',
    breakdown: [
      { label: 'Premier Sea View Room', amount: 6200 },
      { label: 'Airport transfer', amount: 200 },
    ],
    totalPrice: 6400,
    additionalRequest: 'Late check-out request.',
    status: 'checkin-soon',
    expandedInitially: false,
  },
  {
    id: 'b-105',
    roomName: 'Deluxe',
    roomImage: deluxeImage,
    roomImageAlt: 'Deluxe room',
    bookedAt: parseDate('2022-08-01'),
    cancellationDate: parseDate('2022-10-16'),
    checkIn: {
      date: parseDate('2022-08-06'),
      timeText: 'After 2:00 PM',
    },
    checkOut: {
      date: parseDate('2022-08-08'),
      timeText: 'Before 12:00 PM',
    },
    guestsText: '2 Guests',
    nightsText: '2 Nights',
    paymentMethodText: 'Payment success via Credit Card - *888',
    breakdown: [
      { label: 'Deluxe Room', amount: 4800 },
    ],
    totalPrice: 4800,
    status: 'cancelled',
    expandedInitially: false,
  },
]
