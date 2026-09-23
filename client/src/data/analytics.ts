export const analyticsSummary = [
  { label: 'Total booking', value: '76', change: 'Up 8.5% from last month', trend: 'up' },
  { label: 'Total sales', value: '฿58,829', change: 'Up 8.5% from last month', trend: 'up' },
  { label: 'Total booking users', value: '66', change: 'Down 2.4% from last month', trend: 'down' },
  { label: 'Total site visitors', value: '459', change: 'Up 8.5% from last month', trend: 'up' },
] as const

export const roomAvailability = [
  { label: 'Occupied', value: 21, tone: 'orange' },
  { label: 'Booked', value: 14, tone: 'green' },
  { label: 'Available', value: 8, tone: 'gray' },
] as const

export const bookingTrends = [15, 20, 7, 12, 90, 100, 50]
export const weekdayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

export const revenueTrend = {
  labels: ['January', 'February', 'March', 'April', 'May', 'June'],
  values: [37000, 16000, 55000, 66000, 37000, 56000],
}

export const occupancyTrend = {
  labels: ['January', 'February', 'March', 'April', 'May', 'June'],
  values: [40, 52, 98, 38, 50, 82],
}

export const websiteTraffic = {
  labels: ['04:00 AM', '08:00 AM', '12:00 PM', '04:00 PM', '08:00 PM', '12:00 PM'],
  values: [3, 2, 27, 27, 63, 49],
}

export const guestVisits = [
  { label: 'New guests', detail: '867 people', value: 88 },
  { label: 'Returning guests', detail: '118 people', value: 12 },
]

export const paymentMethods = [
  { label: 'Credit card', detail: '699 people', value: 71 },
  { label: 'Cash', detail: '286 people', value: 29 },
]
