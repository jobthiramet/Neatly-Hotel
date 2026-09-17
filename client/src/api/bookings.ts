import { api } from '@/api/client'

export type BookingStatus =
  | 'PENDING_PAYMENT'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'EXPIRED'

export type BookingPaymentMethod = 'STRIPE' | 'CASH'
export type BookingItemKind = 'ROOM' | 'ADDON' | 'DISCOUNT'

export interface BookingLineItem {
  kind: BookingItemKind
  code: string
  label: string
  quantity: number
  unitPrice: number
  amount: number
}

export interface NamedRequest {
  code: string
  label: string
}

export interface BookingResponse {
  id: string
  bookingNumber: string
  roomTypeId: string
  roomName: string
  roomImageUrl: string | null
  checkIn: string
  checkOut: string
  checkInTimeText: string
  checkOutTimeText: string
  guests: number
  nights: number
  roomsCount: number
  status: BookingStatus
  paymentMethod: BookingPaymentMethod
  guestFirstName: string
  guestLastName: string
  guestEmail: string
  guestPhone: string
  guestCountry: string
  guestDateOfBirth: string
  standardRequests: NamedRequest[]
  additionalRequest: string | null
  promotionCode: string | null
  currency: string
  items: BookingLineItem[]
  roomSubtotal: number
  extrasTotal: number
  discountTotal: number
  grandTotal: number
  paymentMethodText: string
  clientSecret: string | null
  holdExpiresAt: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateBookingRequest {
  roomTypeId: string
  checkIn: string
  checkOut: string
  guests: number
  roomsCount?: number
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  country: string
  dateOfBirth: string
  standardRequestCodes: string[]
  specialRequestCodes: string[]
  additionalRequest: string
  promotionCode: string
  paymentMethod: BookingPaymentMethod
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function createBooking(token: string, body: CreateBookingRequest) {
  const { data } = await api.post<ApiResponse<BookingResponse>>('/bookings', body, {
    headers: { Authorization: `Bearer ${token}` },
  })
  return data.data
}

export async function listBookings(token: string, page = 0, size = 20) {
  const { data } = await api.get<ApiResponse<PageResponse<BookingResponse>>>('/bookings', {
    params: { page, size },
    headers: { Authorization: `Bearer ${token}` },
  })
  return data.data
}

export async function getBooking(token: string, id: string) {
  const { data } = await api.get<ApiResponse<BookingResponse>>(`/bookings/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  return data.data
}

export async function retryBookingPayment(token: string, id: string) {
  const { data } = await api.post<ApiResponse<BookingResponse>>(`/bookings/${id}/payment-session`, {}, {
    headers: { Authorization: `Bearer ${token}` },
  })
  return data.data
}
