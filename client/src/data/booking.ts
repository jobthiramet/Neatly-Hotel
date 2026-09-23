import type { DateValue } from '@internationalized/date'
import { parseDate } from '@internationalized/date'
import deluxeImage from '@/assets/home/room-deluxe.webp'
import premierImage from '@/assets/home/room-premier-sea-view.webp'
import superiorGardenImage from '@/assets/home/room-superior-garden-view.webp'

export type BookingStatus =
  | 'within-24h' // within 24 hours of booking: able to change date, able to cancel and refund
  | 'after-24h' // after 24 hours of booking: cannot change date, can cancel and refund
  | 'checkin-soon' // within 24 hours of check-in date: cancel booking will not able to refund
  | 'checked-in' // after check-in (14:00 Bangkok or API CHECKED_IN): all CTAs disappear, only view detail
  | 'cancelled' // cancelled booking: shows cancellation date, Room Detail stays

export interface PriceBreakdownItem {
  label: string
  amount: number
  isDiscount?: boolean
}

export interface GuestDetails {
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  country: string
}

export type BasicInfoField = keyof GuestDetails | 'dateOfBirth'

export type CheckoutPaymentMethod = 'credit' | 'cash'
export type PaymentField = 'method' | 'cardNumber' | 'expiry' | 'cvc' | 'cardOwner'

export interface CheckoutPayment {
  method: CheckoutPaymentMethod
  cardNumber: string
  expiry: string
  cvc: string
  cardOwner: string
  promotionCode: string
}

export interface RequestOption {
  id: string
  label: string
}

export interface SpecialRequestOption extends RequestOption {
  price: number
}

/** Unconfirmed room preferences. Not added to the booking total. */
export const standardRequests: RequestOption[] = [
  { id: 'early-check-in', label: 'Early check-in' },
  { id: 'late-check-out', label: 'Late check-out' },
  { id: 'non-smoking', label: 'Non-smoking room' },
  { id: 'high-floor', label: 'A room on the high floor' },
  { id: 'quiet-room', label: 'A quiet room' },
]

/** Paid extras. Added to the booking total when selected. */
export const specialRequests: SpecialRequestOption[] = [
  { id: 'baby-cot', label: 'Baby cot', price: 400 },
  { id: 'airport-transfer', label: 'Airport transfer', price: 200 },
  { id: 'extra-bed', label: 'Extra bed', price: 500 },
  { id: 'extra-pillows', label: 'Extra pillows', price: 100 },
  { id: 'phone-chargers', label: 'Phone chargers and adapters', price: 100 },
  { id: 'breakfast', label: 'Breakfast', price: 150 },
]

export const CHECK_IN_TIME_TEXT = 'After 2:00 PM'
export const CHECK_OUT_TIME_TEXT = 'Before 12:00 PM'
export const BOOKING_HOLD_SECONDS = 5 * 60

/** Mock checkout codes. Amounts match the Figma payment breakdown. */
export const BOOKING_PROMOTION_CODES: Record<string, number> = {
  NEATLYNEW400: 400,
}

export function promotionDiscount(code: string) {
  return BOOKING_PROMOTION_CODES[code.trim().toUpperCase()] ?? 0
}

export const BOOKING_POLICIES = [
  'Cancel booking will get full refund if the cancellation occurs before 24 hours of the check-in date.',
  'Able to change check-in or check-out date booking within 24 hours of the booking date',
]

export const CHECKOUT_STEPS = ['Basic Information', 'Special Request', 'Payment Method'] as const

export function formatThb(amount: number) {
  return amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

export function nightsBetween(start: DateValue, end: DateValue) {
  const millisecondsPerDay = 86_400_000
  return Math.round(
    (end.toDate('UTC').getTime() - start.toDate('UTC').getTime()) / millisecondsPerDay,
  )
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
  /** Card bookings can be refunded to the original card. Cash is cancel-only. */
  paymentMethod?: 'STRIPE' | 'CASH'
  breakdown: PriceBreakdownItem[]
  totalPrice: number
  additionalRequest?: string
  status: BookingStatus
  /** Instant when the refund window closes (check-in 14:00 Bangkok minus 24 hours). */
  refundDeadlineMs?: number
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
