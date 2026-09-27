import { parseDate, today } from '@internationalized/date'
import type { AnalyticsMetric, AnalyticsResponse } from '@/api/analytics'

export const mockToday = today('Asia/Bangkok')
export const mockStart = mockToday.set({ day: 1 }).subtract({ months: 5 })
export const mockRoomTypes = [
  { id: 'superior-garden-view', name: 'Superior Garden View', capacity: 12 },
  { id: 'deluxe', name: 'Deluxe', capacity: 16 },
  { id: 'superior', name: 'Superior', capacity: 20 },
  { id: 'premier-sea-view', name: 'Premier Sea View', capacity: 10 },
  { id: 'supreme', name: 'Supreme', capacity: 14 },
  { id: 'suite', name: 'Suite', capacity: 8 },
]

// Fixtures stay identical across reloads within the same demo month.
function noise(seed: number) {
  return ((Math.sin(seed * 12.9898) * 43758.5453) % 1 + 1) % 1
}
const round = (value: number) => Math.round(value * 10) / 10
const dayNumber = (date: string) => Math.floor(Date.parse(`${date}T00:00:00Z`) / 86400000)

function makeDay(date: string) {
  const seed = dayNumber(date)
  const calendarDate = parseDate(date)
  const monthIndex = (calendarDate.year - mockStart.year) * 12 + calendarDate.month - mockStart.month
  const season = [0.75, 1.3, 0.55, 1.55, 0.85, 1.15][monthIndex]!
  // This month's demo has more repeat bookings: sales grow while unique guests fall.
  const currentMonth = date.slice(0, 7) === mockToday.toString().slice(0, 7)
  const activity = currentMonth ? 1.2 : 1
  const returningEvery = currentMonth ? 2 : 3
  const weekday = new Date(`${date}T00:00:00Z`).getUTCDay()
  const weekend = [0, 5, 6].includes(weekday)
  // Sunday through Saturday: quieter midweek, a Friday rise and a Saturday peak.
  const weekdayDemand = [1.6, 0.85, 0.5, 0.3, 0.65, 1.35, 2.1][weekday]!
  const bookings = Array.from({ length: Math.round((8 + Math.floor(noise(seed) * 12)) * weekdayDemand * activity) }, (_, index) => ({
    user: index % returningEvery === 0 ? `returning-${(seed + index) % 30}` : `${date}-${index}`,
    returning: index % returningEvery === 0,
    total: Math.round((2400 + Math.floor(noise(seed + index + 0.2) * 45) * 100) * (1 + index % 3) * season),
    card: noise(seed + index + 0.4) > [0.2, 0.45, 0.3, 0.55, 0.25, 0.4][monthIndex]!,
  }))
  const roomTypes = mockRoomTypes.map((room, index) => {
    // Monthly room-specific demand survives averaging, so each series has its own peaks.
    const monthlyOccupancy = [
      [0.8, 0.3, 0.2, 0.9, 0.6, 0.35],
      [0.4, 0.65, 0.35, 0.7, 0.85, 0.55],
      [0.2, 0.45, 0.75, 0.6, 0.35, 0.8],
      [0.55, 0.85, 0.4, 0.8, 0.25, 0.65],
      [0.3, 0.5, 0.65, 0.85, 0.45, 0.2],
      [0.65, 0.2, 0.3, 0.75, 0.55, 0.9],
    ][index]![monthIndex]!
    const dailySwing = Math.sin(calendarDate.day / 2.5) * 0.18
    const occupancy = Math.max(0.05, Math.min(0.95, monthlyOccupancy + dailySwing + (weekend ? 0.05 : -0.03)))
    const occupied = Math.round(room.capacity * occupancy)
    const booked = Math.round((room.capacity - occupied) * (0.45 + Math.sin(calendarDate.day / 3 + 1) * 0.25))
    return { ...room, occupied, booked, available: room.capacity - occupied - booked }
  })
  const traffic = Object.fromEntries(['home', ...mockRoomTypes.map(room => room.id)].map((page, index) => [
    page, Array.from({ length: 24 }, (_, hour) => {
      const hourlyDemand = [0.3, 0.2, 0.1, 0.1, 0.2, 0.4, 0.8, 1.4, 2.4, 3.8, 2.8, 1.6, 1.1, 1.7, 2.6, 1.9, 1.2, 2.3, 3.6, 4.5, 3.2, 1.8, 0.9, 0.5][(hour + index) % 24]!
      const dailyDemand = 1 + Math.sin(calendarDate.day / 1.8) * 0.65
      return Math.round((index === 0 ? 22 : 7) * (0.7 + noise(seed + hour + index / 10) * 0.6) * hourlyDemand * dailyDemand * season * activity)
    }),
  ]))
  return { date, bookings, roomTypes, traffic, checkIn: 14 * 60 + Math.floor(noise(seed + 0.3) * 100), checkOut: 10 * 60 + Math.floor(noise(seed + 0.5) * 90) }
}

const days = [] as ReturnType<typeof makeDay>[]
for (let date = mockStart; date.compare(mockToday) <= 0; date = date.add({ days: 1 })) days.push(makeDay(date.toString()))

export function getMockDays(from: string, to: string) {
  return days.filter(day => day.date >= from && day.date <= to)
}

function metric(value: number, previousValue: number, complete: boolean): AnalyticsMetric {
  return { value, previousValue, changePercent: complete && previousValue ? round((value - previousValue) / previousValue * 100) : null }
}

function breakdown(first: string, second: string, firstCount: number, total: number) {
  const percentage = total ? round(firstCount / total * 100) : 0
  return [
    { key: first, count: firstCount, percentage },
    { key: second, count: total - firstCount, percentage: total ? round(100 - percentage) : 0 },
  ]
}

export function getMockAvailability(from: string, to: string) {
  const selected = getMockDays(from, to)
  const totalBookable = mockRoomTypes.reduce((total, room) => total + room.capacity, 0)
  const occupied = selected.length ? round(selected.reduce((total, day) => total + day.roomTypes.reduce((sum, room) => sum + room.occupied, 0), 0) / selected.length) : 0
  const booked = selected.length ? round(selected.reduce((total, day) => total + day.roomTypes.reduce((sum, room) => sum + room.booked, 0), 0) / selected.length) : 0
  return { occupied, booked, available: selected.length ? round(totalBookable - occupied - booked) : 0, totalBookable }
}

export function getMockAnalytics(from: string, to: string): AnalyticsResponse & { visitors: AnalyticsMetric } {
  const selected = getMockDays(from, to)
  const count = dayNumber(to) - dayNumber(from) + 1
  const comparisonTo = parseDate(from).subtract({ days: 1 })
  const comparisonFrom = comparisonTo.subtract({ days: count - 1 })
  const previous = getMockDays(comparisonFrom.toString(), comparisonTo.toString())
  const bookings = selected.flatMap(day => day.bookings)
  const priorBookings = previous.flatMap(day => day.bookings)
  const users = new Map(bookings.map(booking => [booking.user, booking.returning]))
  const sumSales = (items: typeof bookings) => items.reduce((sum, booking) => sum + booking.total, 0)
  const visitors = (items: typeof days) => items.reduce((total, day) => total + Object.values(day.traffic).flat().reduce((sum, value) => sum + value, 0), 0)
  const daily = count <= 31
  const groups = new Map<string, typeof days>()
  for (const day of selected) {
    const key = daily ? day.date : day.date.slice(0, 7)
    groups.set(key, [...(groups.get(key) ?? []), day])
  }
  return {
    period: { from, to, comparisonFrom: comparisonFrom.toString(), comparisonTo: comparisonTo.toString(), granularity: daily ? 'DAY' : 'MONTH' },
    summary: {
      totalBookings: metric(bookings.length, priorBookings.length, previous.length === count),
      totalSales: metric(sumSales(bookings), sumSales(priorBookings), previous.length === count),
      bookingUsers: metric(users.size, new Set(priorBookings.map(booking => booking.user)).size, previous.length === count),
    },
    visitors: metric(visitors(selected), visitors(previous), previous.length === count),
    roomAvailability: getMockAvailability(mockToday.toString(), mockToday.toString()),
    bookingTrend: [...groups].map(([label, items]) => ({ label, value: items.reduce((sum, day) => sum + day.bookings.length, 0) })),
    revenueTrend: [...groups].map(([label, items]) => ({ label, value: sumSales(items.flatMap(day => day.bookings)) })),
    guestMix: breakdown('NEW', 'RETURNING', [...users.values()].filter(returning => !returning).length, users.size),
    paymentMethods: breakdown('STRIPE', 'CASH', bookings.filter(booking => booking.card).length, bookings.length),
  }
}

export function getMockOccupancy(from: string, to: string, roomType = 'overall') {
  const selected = getMockDays(from, to)
  const groups = new Map<string, { occupied: number, capacity: number }>()
  for (const day of selected) {
    const key = day.date.slice(0, 7)
    const group = groups.get(key) ?? { occupied: 0, capacity: 0 }
    for (const room of day.roomTypes.filter(room => roomType === 'overall' || room.id === roomType)) {
      group.occupied += room.occupied
      group.capacity += room.capacity
    }
    groups.set(key, group)
  }
  return {
    labels: [...groups.keys()].map(month => new Intl.DateTimeFormat('en-US', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${month}-01T00:00:00Z`))),
    values: [...groups.values()].map(group => group.capacity ? round(group.occupied / group.capacity * 100) : 0),
  }
}

export function getMockCheckTimes(from: string, to: string) {
  const selected = getMockDays(from, to)
  const total = selected.reduce((sum, day) => sum + day.bookings.length, 0)
  const average = (key: 'checkIn' | 'checkOut') => {
    if (!total) return 'N/A'
    const minutes = Math.round(selected.reduce((sum, day) => sum + day[key] * day.bookings.length, 0) / total)
    return `${Math.floor(minutes / 60) % 12 || 12}:${String(minutes % 60).padStart(2, '0')} ${minutes >= 720 ? 'PM' : 'AM'}`
  }
  return { checkIn: average('checkIn'), checkOut: average('checkOut') }
}

export function getMockTraffic(page: string, period: string) {
  const hourly = period === 'realtime' || period === 'yesterday'
  const to = period === 'yesterday' ? mockToday.subtract({ days: 1 }) : mockToday
  const from = to.subtract({ days: hourly ? 0 : period === 'week' ? 6 : 29 })
  const selected = getMockDays(from.toString(), to.toString())
  const values = selected.map(day => Array.from({ length: 24 }, (_, hour) => (
    Object.entries(day.traffic).filter(([key]) => page === 'all' || key === page).reduce((sum, [, hours]) => sum + (hours[hour] ?? 0), 0)
  )))
  return hourly
    ? { labels: Array.from({ length: 24 }, (_, hour) => `${String(hour).padStart(2, '0')}:00`), values: values[0] ?? [] }
    : { labels: selected.map(day => day.date.slice(5)), values: values.map(hours => hours.reduce((sum, value) => sum + value, 0)) }
}
