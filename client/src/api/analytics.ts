import { api } from '@/api/client'

export interface AnalyticsMetric { value: number, previousValue: number, changePercent: number | null }
export interface AnalyticsPoint { label: string, value: number }
export interface AnalyticsBreakdown { key: string, count: number, percentage: number }
export interface AnalyticsResponse {
  period: { from: string, to: string, comparisonFrom: string, comparisonTo: string, granularity: 'DAY' | 'MONTH' }
  summary: { totalBookings: AnalyticsMetric, totalSales: AnalyticsMetric, bookingUsers: AnalyticsMetric }
  roomAvailability: { occupied: number, booked: number, available: number, totalBookable: number }
  bookingTrend: AnalyticsPoint[]
  revenueTrend: AnalyticsPoint[]
  guestMix: AnalyticsBreakdown[]
  paymentMethods: AnalyticsBreakdown[]
}

export async function fetchAnalytics(from: string, to: string, signal?: AbortSignal) {
  const token = await window.Clerk?.session?.getToken()
  if (!token) throw new Error('Please log in as an agent to view analytics.')
  const { data } = await api.get<{ data: AnalyticsResponse }>('/admin/analytics', {
    params: { from, to }, headers: { Authorization: `Bearer ${token}` }, signal,
  })
  return data.data
}
