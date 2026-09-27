<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { parseDate } from '@internationalized/date'
import { computed, ref, shallowRef } from 'vue'
import { toast } from 'vue-sonner'
import bookingIcon from '@/assets/icons/booking.svg'
import cartIcon from '@/assets/icons/cart.svg'
import siteIcon from '@/assets/icons/site.svg'
import walletIcon from '@/assets/icons/wallet.svg'
import type { AnalyticsMetric } from '@/api/analytics'
import AnalyticsLineChart from '@/components/admin/AnalyticsLineChart.vue'
import { IconCash, IconCreditCard, IconHotel } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { rooms } from '@/data/home'
import { getMockAnalytics, getMockAvailability, getMockCheckTimes, getMockDays, getMockOccupancy, getMockTraffic, mockRoomTypes, mockStart, mockToday } from '@/data/analytics'

const currentDate = mockToday
const monthStart = parseDate(`${currentDate.year}-${String(currentDate.month).padStart(2, '0')}-01`)
// Dashboard demo data; the live client remains in @/api/analytics.
const summaryAnalytics = computed(() => getMockAnalytics(monthStart.toString(), currentDate.toString()))
const revenueAnalytics = computed(() => getMockAnalytics(revenueFrom.value.toString(), revenueTo.value.toString()))
const roomPeriod = ref('today')
const bookingPeriod = ref('month')
const occupancyView = ref('overall')
const trafficPage = ref('all')
const trafficPeriod = ref('realtime')
const revenueFrom = shallowRef<DateValue>(monthStart.subtract({ months: 5 }))
const revenueTo = shallowRef<DateValue>(currentDate)
const occupancyFrom = shallowRef<DateValue>(mockStart)
const occupancyTo = shallowRef<DateValue>(currentDate)

const summaryCards = computed(() => [
  summaryCard('Total booking', summaryAnalytics.value?.summary.totalBookings, cartIcon),
  summaryCard('Total sales', summaryAnalytics.value?.summary.totalSales, walletIcon, true),
  summaryCard('Total booking users', summaryAnalytics.value?.summary.bookingUsers, bookingIcon),
  summaryCard('Total site visitors', summaryAnalytics.value.visitors, siteIcon),
])
const availability = computed(() => {
  const weekday = (new Date(`${currentDate}T00:00:00Z`).getUTCDay() + 6) % 7
  const from = roomPeriod.value === 'month' ? monthStart : roomPeriod.value === 'week' ? currentDate.subtract({ days: weekday }) : currentDate
  return getMockAvailability(from.toString(), currentDate.toString())
})
const roomAvailability = computed(() => [
  { label: 'Occupied', value: availability.value.occupied, tone: 'orange' },
  { label: 'Booked', value: availability.value.booked, tone: 'green' },
  { label: 'Available', value: availability.value.available, tone: 'gray' },
])
const roomTotal = computed(() => availability.value.totalBookable)
const occupiedDash = computed(() => dash(availability.value.occupied))
const bookedDash = computed(() => dash(availability.value.booked))
const bookingTrends = computed(() => {
  const totals = Array.from({ length: 7 }, () => 0)
  const { from, to } = bookingRange()
  getMockDays(from, to).forEach(day => {
    const weekday = (new Date(`${day.date}T00:00:00Z`).getUTCDay() + 6) % 7
    totals[weekday]! += day.bookings.length
  })
  const peakBookings = Math.max(...totals, 1)
  return totals.map(value => value / peakBookings * 100)
})
const weekdayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const revenueTrend = computed(() => {
  const data = revenueAnalytics.value
  const from = parseDate(data?.period.from ?? revenueFrom.value.toString())
  const to = parseDate(data?.period.to ?? revenueTo.value.toString())
  const months = new Map<string, number>()
  for (let month = from.set({ day: 1 }); month.compare(to) <= 0; month = month.add({ months: 1 })) {
    months.set(month.toString(), 0)
  }
  data?.revenueTrend.forEach((point, index) => {
    const date = data.period.granularity === 'DAY'
      ? from.add({ days: index })
      : from.set({ day: 1 }).add({ months: index })
    const key = date.set({ day: 1 }).toString()
    months.set(key, (months.get(key) ?? 0) + point.value)
  })
  const formatter = new Intl.DateTimeFormat('en-US', {
    month: 'long', timeZone: 'UTC', ...(from.year !== to.year ? { year: 'numeric' as const } : {}),
  })
  return {
    labels: [...months.keys()].map(date => formatter.format(new Date(`${date}T00:00:00Z`))),
    values: data ? [...months.values()] : [],
  }
})
const revenueScale = computed(() => scaleFor(revenueTrend.value.values))
const occupancyAnalytics = computed(() => getMockAnalytics(occupancyFrom.value.toString(), occupancyTo.value.toString()))
const occupancyTrend = computed(() => getMockOccupancy(occupancyFrom.value.toString(), occupancyTo.value.toString()))
const roomChartColors = ['var(--color-orange-500)', 'var(--color-green-700)', 'var(--color-orange-300)', 'var(--color-status-info)', 'var(--color-status-warning)', 'var(--color-green-400)']
const occupancyRoomTrends = computed(() => mockRoomTypes.map((room, index) => ({
  ...room,
  color: roomChartColors[index],
  ...getMockOccupancy(occupancyFrom.value.toString(), occupancyTo.value.toString(), room.id),
})))
const checkTimes = computed(() => getMockCheckTimes(occupancyFrom.value.toString(), occupancyTo.value.toString()))
const trafficTrend = computed(() => getMockTraffic(trafficPage.value, trafficPeriod.value))
const trafficScale = computed(() => scaleFor(trafficTrend.value.values, 140))
const guestVisits = computed(() => occupancyAnalytics.value.guestMix.map(item => ({
  label: item.key === 'NEW' ? 'New guests' : 'Returning guests', detail: item.count === null ? 'N/A' : `${item.count} people`, value: item.percentage,
})))
const paymentMethods = computed(() => occupancyAnalytics.value.paymentMethods.map(item => ({
  label: item.key === 'STRIPE' ? 'Credit card' : 'Cash', detail: item.count === null ? 'N/A' : `${item.count} bookings`, value: item.percentage,
})))

function summaryCard(label: string, metric: AnalyticsMetric | undefined, icon: string, currency = false) {
  if (!metric) return { label, value: 'N/A', change: 'Loading data', trend: 'neutral', icon }
  const value = currency
    ? new Intl.NumberFormat('th-TH', { style: 'currency', currency: 'THB', minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(metric.value)
    : metric.value.toLocaleString('en-US')
  if (metric.changePercent === null) return { label, value, change: 'No data in previous period', trend: 'neutral', icon }
  return {
    label, value,
    change: `${metric.changePercent >= 0 ? 'Up' : 'Down'} ${Math.abs(metric.changePercent)}% from last period`,
    trend: metric.changePercent >= 0 ? 'up' : 'down', icon,
  }
}

function bookingRange() {
  if (bookingPeriod.value === 'last-month') {
    const end = monthStart.subtract({ days: 1 })
    return { from: end.set({ day: 1 }).toString(), to: end.toString() }
  }
  if (bookingPeriod.value === 'last-two-months') return { from: monthStart.subtract({ months: 2 }).toString(), to: monthStart.subtract({ days: 1 }).toString() }
  return { from: monthStart.toString(), to: currentDate.toString() }
}

function dash(value: number) {
  return roomTotal.value === 0 ? 0 : (value / roomTotal.value) * 552.92
}

function scaleFor(values: number[], minimum = 70000) {
  const highest = Math.max(...values, minimum)
  const magnitude = 10 ** Math.floor(Math.log10(highest / 7))
  const step = Math.ceil(highest / 7 / magnitude) * magnitude
  return { max: Math.ceil(highest / step) * step, step }
}

function exportRevenue() {
  const rows = [['Period', 'Revenue (THB)'], ...revenueTrend.value.labels.map((label, index) => [label, String(revenueTrend.value.values[index] ?? '')])]
  downloadCsv(rows, `revenue-trend-${revenueFrom.value}-to-${revenueTo.value}.csv`)
}

function exportOccupancy() {
  const series = occupancyView.value === 'overall' ? [{ name: 'Overall', ...occupancyTrend.value }] : occupancyRoomTrends.value
  const rows = [['Period', 'Room type', 'Occupancy (%)'], ...series.flatMap(room => room.labels.map((label, index) => [label, room.name, String(room.values[index] ?? '')]))]
  downloadCsv(rows, `occupancy-${occupancyFrom.value}-to-${occupancyTo.value}.csv`)
}

function downloadCsv(rows: string[][], filename: string) {
  const csv = rows.map(row => row.map(value => `"${value.replaceAll('"', '""')}"`).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
  toast.success('File downloaded successfully')
}
</script>

<template>
  <div class="flex flex-col gap-8 pb-28">
    <section aria-label="Analytics summary" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article v-for="card in summaryCards" :key="card.label" class="flex min-h-32 items-start justify-between gap-4 rounded-sm border border-gray-300 bg-white p-4">
        <div><h2 class="text-body2 text-gray-800">{{ card.label }}</h2><p class="mt-1 text-h4 text-gray-900">{{ card.value }}</p>
          <p :class="['mt-3 flex items-center gap-2 text-body3', card.trend === 'up' ? 'text-green-500' : card.trend === 'down' ? 'text-orange-500' : 'text-gray-600']">
            <span v-if="card.trend !== 'neutral'" aria-hidden="true" class="text-h5">{{ card.trend === 'up' ? '↗' : '↘' }}</span>{{ card.change }}
          </p>
        </div>
        <span class="flex size-12 shrink-0 items-center justify-center rounded-full bg-gray-200 text-gray-700"><img :src="card.icon" alt="" class="size-6" aria-hidden="true"></span>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
        <div class="flex items-center justify-between gap-4"><h2 class="text-h5 text-gray-600">Room Availability</h2>
          <Select v-model="roomPeriod"><SelectTrigger class="w-40"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="month">This month</SelectItem><SelectItem value="week">This week</SelectItem><SelectItem value="today">Today</SelectItem></SelectContent></Select>
        </div>
        <div class="mt-8 flex flex-col items-center gap-4 lg:flex-row lg:items-end lg:justify-between">
          <svg viewBox="0 0 220 220" role="img" aria-label="Room availability" class="size-60 shrink-0 -rotate-90">
            <circle cx="110" cy="110" r="88" fill="none" stroke="var(--color-gray-300)" stroke-width="40" />
            <circle cx="110" cy="110" r="88" fill="none" stroke="var(--color-green-700)" stroke-width="40" :stroke-dasharray="`${bookedDash} 552.92`" :stroke-dashoffset="-occupiedDash" />
            <circle cx="110" cy="110" r="88" fill="none" stroke="var(--color-orange-500)" stroke-width="40" :stroke-dasharray="`${occupiedDash} 552.92`" />
            <circle cx="110" cy="110" r="74" fill="none" stroke="var(--color-white)" stroke-opacity="0.3" stroke-width="12" />
          </svg>
          <ul class="flex shrink-0 flex-col gap-3 pb-4"><li v-for="item in roomAvailability" :key="item.label" class="flex items-center gap-3 text-body2 text-gray-700">
            <span :class="['size-3 shrink-0 rounded-full', item.tone === 'orange' ? 'bg-orange-500' : item.tone === 'green' ? 'bg-green-700' : 'bg-gray-400']" /><span class="flex-1">{{ item.label }}</span><strong class="font-medium">{{ item.value }}</strong><span>Rooms</span>
          </li></ul>
        </div>
      </article>

      <article class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
        <div class="flex items-center justify-between gap-4"><h2 class="text-h5 text-gray-600">Booking Trends by Day</h2>
          <Select v-model="bookingPeriod"><SelectTrigger class="w-40"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="month">This month</SelectItem><SelectItem value="last-month">Last month</SelectItem><SelectItem value="last-two-months">Last 2 months</SelectItem></SelectContent></Select>
        </div>
        <div class="mt-10 flex h-60 gap-3"><div class="flex shrink-0 flex-col justify-between pb-7 text-body3 text-gray-600"><span v-for="value in [100, 80, 60, 40, 20, 0]" :key="value">{{ value }}%</span></div>
          <div class="relative grid flex-1 grid-cols-7"><div aria-hidden="true" class="pointer-events-none absolute inset-0 flex flex-col justify-between pb-7"><span v-for="value in 6" :key="value" class="block border-t border-gray-300" /></div>
            <div v-for="(value, index) in bookingTrends" :key="weekdayLabels[index]" class="z-10 flex min-w-0 flex-col items-center"><div class="flex w-full min-h-0 flex-1 items-end justify-center"><div v-if="value > 0" class="w-2 rounded-full bg-orange-500" :title="`${weekdayLabels[index]}: ${value.toFixed(1)}% of the busiest weekday`" :style="{ height: `${value}%` }" /></div><span class="mt-3 text-body3 text-gray-700">{{ weekdayLabels[index] }}</span></div>
          </div>
        </div>
      </article>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6"><h2 class="text-h5 text-gray-600">Revenue Trend</h2><div class="flex flex-wrap items-center gap-3">
        <span class="text-body2 text-gray-600">From</span><DatePicker v-model="revenueFrom" format="compact" class="w-40" :min-value="mockStart" :max-value="revenueTo" /><span class="text-body2 text-gray-600">to</span><DatePicker v-model="revenueTo" format="compact" class="w-40" :min-value="revenueFrom" :max-value="currentDate" /><Button type="button" class="min-w-28" @click="exportRevenue">Export</Button>
      </div></div>
      <div class="mt-8"><AnalyticsLineChart :labels="revenueTrend.labels" :values="revenueTrend.values" :max="revenueScale.max" :step="revenueScale.step" revenue :empty="!revenueTrend.values.some(value => value > 0)" /></div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6"><h2 class="text-h5 text-gray-600">Occupancy &amp; Guest</h2><div class="flex flex-wrap items-center gap-3">
        <span class="text-body2 text-gray-600">View by</span><Select v-model="occupancyView"><SelectTrigger class="w-36"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="overall">Overall</SelectItem><SelectItem value="room-type">Room type</SelectItem></SelectContent></Select>
        <DatePicker v-model="occupancyFrom" format="compact" class="w-40" :min-value="mockStart" :max-value="occupancyTo" /><span class="text-body2 text-gray-600">to</span><DatePicker v-model="occupancyTo" format="compact" class="w-40" :min-value="occupancyFrom" :max-value="currentDate" /><Button type="button" class="min-w-28" @click="exportOccupancy">Export</Button>
      </div></div>
      <div class="mt-10 flex flex-wrap items-center justify-between gap-6">
        <h3 class="text-body2 text-gray-700">Occupancy Rate</h3>
        <ul v-if="occupancyView === 'room-type'" class="flex flex-wrap gap-4 text-body3 text-gray-700" aria-label="Room type legend">
          <li v-for="room in occupancyRoomTrends" :key="room.id" class="flex items-center gap-2"><span class="size-3 shrink-0 rounded-full" :style="{ backgroundColor: room.color }" />{{ room.name }}</li>
        </ul>
      </div>
      <div class="mt-8">
        <AnalyticsLineChart v-if="occupancyView === 'overall'" :labels="occupancyTrend.labels" :values="occupancyTrend.values" :max="100" :step="20" value-suffix="%" :empty="!occupancyTrend.values.length" />
        <div v-else class="overflow-x-auto">
          <svg viewBox="0 0 1000 280" class="block w-full min-w-160 text-gray-700" role="img" aria-label="Monthly occupancy percentage by room type">
            <g v-for="tick in [0, 20, 40, 60, 80, 100]" :key="tick">
              <line x1="60" x2="986" :y1="230 - tick * 2" :y2="230 - tick * 2" stroke="var(--color-gray-300)" />
              <text x="48" :y="234 - tick * 2" text-anchor="end" fill="currentColor" class="text-body3">{{ tick }}%</text>
            </g>
            <g v-for="(label, monthIndex) in occupancyTrend.labels" :key="label" :transform="`translate(${60 + (monthIndex + 0.5) * 926 / occupancyTrend.labels.length}, 0)`">
              <rect v-for="(room, roomIndex) in occupancyRoomTrends" :key="room.id" :x="(roomIndex - (occupancyRoomTrends.length - 1) / 2) * 14 - 5" :y="230 - (room.values[monthIndex] ?? 0) * 2" width="10" :height="(room.values[monthIndex] ?? 0) * 2" rx="5" :fill="room.color" tabindex="0" :aria-label="`${label}, ${room.name}: ${room.values[monthIndex] ?? 0}%`">
                <title>{{ label }} · {{ room.name }}: {{ room.values[monthIndex] ?? 0 }}%</title>
              </rect>
              <text x="0" y="268" text-anchor="middle" fill="currentColor" class="text-body3">{{ label }}</text>
            </g>
          </svg>
        </div>
      </div>

      <div class="mt-12 grid gap-14 lg:grid-cols-2">
        <div><h3 class="text-body2 text-gray-700">Guest Visit</h3><ul class="mt-6 flex flex-col gap-6"><li v-for="item in guestVisits" :key="item.label"><div data-breakdown-row class="text-body3"><p><strong class="font-medium text-gray-900">{{ item.label }}</strong> <span class="ml-2 text-gray-600">{{ item.detail }}</span></p><strong data-breakdown-percentage class="text-body2 text-gray-900">{{ item.value === null ? 'N/A' : `${item.value ?? 0}%` }}</strong><div class="h-2 overflow-hidden rounded-full bg-gray-300"><div class="h-full rounded-full bg-orange-500" :style="{ width: `${item.value ?? 0}%` }" /></div></div></li></ul></div>
        <div><h3 class="text-body2 text-gray-700">Payment Method</h3><ul class="mt-6 flex flex-col gap-6"><li v-for="(item, index) in paymentMethods" :key="item.label" class="flex gap-4"><span class="flex size-10 items-center justify-center rounded-full bg-gray-200 text-gray-700"><component :is="index === 0 ? IconCreditCard : IconCash" class="size-5" /></span><div class="flex-1"><div data-breakdown-row class="text-body3"><p><strong class="font-medium text-gray-900">{{ item.label }}</strong> <span class="ml-2 text-gray-600">{{ item.detail }}</span></p><strong data-breakdown-percentage class="text-body2 text-gray-900">{{ item.value === null ? 'N/A' : `${item.value ?? 0}%` }}</strong><div class="h-2 overflow-hidden rounded-full bg-gray-300"><div class="h-full rounded-full bg-orange-500" :style="{ width: `${item.value ?? 0}%` }" /></div></div></div></li></ul></div>
      </div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10"><h2 class="text-h5 text-gray-600">Check-in and Check-out Times Averages</h2><div class="mt-6 grid gap-4 lg:grid-cols-2">
      <article class="flex min-h-28 items-center gap-5 rounded-sm bg-green-100 p-6 text-green-600"><span class="flex size-14 shrink-0 items-center justify-center rounded-full bg-green-200"><IconHotel class="size-8" /></span><div class="flex-1"><h3 class="text-h5">Check-in</h3><p class="mt-1 text-body3 text-green-500">Check-in time from 2:00 PM onwards</p></div><strong class="text-h5">{{ checkTimes.checkIn }}</strong></article>
      <article class="flex min-h-28 items-center gap-5 rounded-sm bg-orange-100 p-6 text-orange-500"><span class="flex size-14 shrink-0 items-center justify-center rounded-full bg-orange-200"><IconHotel class="size-8" /></span><div class="flex-1"><h3 class="text-h5">Check-out</h3><p class="mt-1 text-body3 text-orange-400">Check-out time by 12:00 PM</p></div><strong class="text-h5">{{ checkTimes.checkOut }}</strong></article>
    </div></section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10"><div class="flex flex-wrap items-center justify-between gap-6"><h2 class="text-h5 text-gray-600">Website traffic</h2><div class="flex flex-wrap gap-3">
      <Select v-model="trafficPage"><SelectTrigger class="w-40"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">All pages</SelectItem><SelectItem value="home">Home</SelectItem><SelectItem v-for="room in rooms" :key="room.id" :value="room.id">{{ room.name }}</SelectItem></SelectContent></Select>
      <button v-for="period in [{ value: 'realtime', label: 'Real-time' }, { value: 'yesterday', label: 'Yesterday' }, { value: 'week', label: 'Last 7 days' }, { value: 'month', label: 'Last 30 days' }]" :key="period.value" type="button" :class="['h-12 rounded-sm border px-4 text-body2', trafficPeriod === period.value ? 'border-orange-400 bg-orange-100 text-orange-500' : 'border-gray-400 bg-white text-gray-800']" @click="trafficPeriod = period.value">{{ period.label }}</button>
    </div></div><div class="mt-8"><AnalyticsLineChart :labels="trafficTrend.labels" :values="trafficTrend.values" :max="trafficScale.max" :step="trafficScale.step" :empty="!trafficTrend.values.length" /></div></section>
  </div>
</template>

<style scoped>
[data-breakdown-row] {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.5rem 1rem;
}

[data-breakdown-percentage] {
  grid-column: 2;
  grid-row: 2;
}

[data-breakdown-row] > div {
  grid-column: 1;
  grid-row: 2;
}
</style>
