import { api } from '@/api/client'
import type { BedType } from '@/stores/rooms'
import type { BookingItemKind } from '@/api/bookings'

export interface AdminBookingSummary {
  id: string
  customerName: string
  guests: number
  roomType: string
  roomsCount: number
  bedType: BedType | null
  checkIn: string
  checkOut: string
}

export interface AdminBookingLineItem {
  kind: BookingItemKind
  code: string
  label: string
  quantity: number
  unitPrice: number
  amount: number
}

export interface AdminBookingDetail {
  id: string
  customerName: string
  guests: number
  roomType: string
  roomsCount: number
  bedType: BedType | null
  checkIn: string
  checkOut: string
  nights: number
  bookingDate: string
  paymentMethodText: string
  currency: string
  grandTotal: number
  items: AdminBookingLineItem[]
  additionalRequest: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function agentToken() {
  const token = await window.Clerk?.session?.getToken()
  if (!token)
    throw new Error('Please log in as an agent to view customer bookings.')
  return token
}

export async function listAdminBookings(
  params: { search?: string, page?: number, size?: number },
  options: { signal?: AbortSignal } = {},
) {
  const token = await agentToken()
  const { data } = await api.get<{ data: PageResponse<AdminBookingSummary> }>('/admin/bookings', {
    params: {
      search: params.search || undefined,
      page: params.page ?? 0,
      size: params.size ?? 10,
    },
    headers: { Authorization: `Bearer ${token}` },
    signal: options.signal,
  })
  return data.data
}

export async function getAdminBooking(id: string, options: { signal?: AbortSignal } = {}) {
  const token = await agentToken()
  const { data } = await api.get<{ data: AdminBookingDetail }>(`/admin/bookings/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
    signal: options.signal,
  })
  return data.data
}
