<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { getLocalTimeZone, parseDate, today } from '@internationalized/date'
import { ref, shallowRef } from 'vue'
import { toast } from 'vue-sonner'
import bookingIcon from '@/assets/icons/booking.svg'
import cartIcon from '@/assets/icons/cart.svg'
import siteIcon from '@/assets/icons/site.svg'
import walletIcon from '@/assets/icons/wallet.svg'
import AnalyticsLineChart from '@/components/admin/AnalyticsLineChart.vue'
import { IconCash, IconCreditCard, IconHotel } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  analyticsSummary,
  bookingTrends,
  guestVisits,
  occupancyTrend,
  paymentMethods,
  revenueTrend,
  roomAvailability,
  weekdayLabels,
  websiteTraffic,
} from '@/data/analytics'

const summaryIcons = [cartIcon, walletIcon, bookingIcon, siteIcon]
const summaryCards = analyticsSummary.map((card, index) => ({ ...card, icon: summaryIcons[index] }))
const roomPeriod = ref('month')
const bookingPeriod = ref('month')
const occupancyView = ref('overall')
const trafficPage = ref('all')
const trafficPeriod = ref('realtime')
const revenueFrom = shallowRef<DateValue>(parseDate('2022-01-01'))
const revenueTo = shallowRef<DateValue>(parseDate('2022-06-28'))
const occupancyFrom = shallowRef<DateValue>(parseDate('2022-01-01'))
const occupancyTo = shallowRef<DateValue>(parseDate('2022-06-28'))
const currentDate = today(getLocalTimeZone())
const maxBookingTrend = Math.max(...bookingTrends)

function exportCsv(
  filenamePrefix: string,
  headers: [string, string],
  labels: string[],
  values: number[],
  from: DateValue,
  to: DateValue,
) {
  const rows = [headers, ...labels.map((label, index) => [label, String(values[index] ?? '')])]
  const csv = rows.map(row => row.map(value => `"${value.replaceAll('"', '""')}"`).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `${filenamePrefix}-${from}-to-${to}.csv`
  link.click()
  URL.revokeObjectURL(url)
  toast.success('File downloaded successfully')
}

function exportRevenue() {
  exportCsv('revenue-trend', ['Month', 'Revenue (THB)'], revenueTrend.labels, revenueTrend.values, revenueFrom.value, revenueTo.value)
}

function exportOccupancy() {
  exportCsv('occupancy-rate', ['Month', 'Occupancy Rate (%)'], occupancyTrend.labels, occupancyTrend.values, occupancyFrom.value, occupancyTo.value)
}
</script>

<template>
  <div class="flex flex-col gap-8 pb-20">
    <section aria-label="Analytics summary" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article v-for="card in summaryCards" :key="card.label" class="flex min-h-32 items-start justify-between gap-4 rounded-sm border border-gray-300 bg-white p-4">
        <div>
          <h2 class="text-body2 text-gray-800">{{ card.label }}</h2>
          <p class="mt-1 text-h4 text-gray-900">{{ card.value }}</p>
          <p :class="['mt-3 flex items-center gap-2 text-body3', card.trend === 'up' ? 'text-green-500' : 'text-orange-500']">
            <span aria-hidden="true" class="text-h5">{{ card.trend === 'up' ? '↗' : '↘' }}</span>
            {{ card.change }}
          </p>
        </div>
        <span class="flex size-12 shrink-0 items-center justify-center rounded-full bg-gray-200 text-gray-700">
          <img :src="card.icon" alt="" class="size-6" aria-hidden="true">
        </span>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
        <div class="flex items-center justify-between gap-4">
          <h2 class="text-h5 text-gray-600">Room Availability</h2>
          <Select v-model="roomPeriod">
            <SelectTrigger class="w-40"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="month">This month</SelectItem>
              <SelectItem value="last-month">This week</SelectItem>
              <SelectItem value="today">Today</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div class="mt-8 flex flex-col items-center gap-8 lg:flex-row lg:justify-center">
          <svg viewBox="0 0 220 220" role="img" aria-label="Room availability" class="size-52 shrink-0 -rotate-90">
            <circle cx="110" cy="110" r="76" fill="none" stroke="var(--color-gray-300)" stroke-width="32" />
            <circle cx="110" cy="110" r="76" fill="none" stroke="var(--color-green-700)" stroke-width="32" stroke-dasharray="155 323" stroke-dashoffset="-235" />
            <circle cx="110" cy="110" r="76" fill="none" stroke="var(--color-orange-500)" stroke-width="32" stroke-dasharray="233 245" />
          </svg>
          <ul class="flex min-w-52 flex-col gap-3">
            <li v-for="item in roomAvailability" :key="item.label" class="flex items-center gap-3 text-body2 text-gray-700">
              <span :class="['size-3 shrink-0 rounded-full', item.tone === 'orange' ? 'bg-orange-500' : item.tone === 'green' ? 'bg-green-700' : 'bg-gray-400']" />
              <span class="flex-1">{{ item.label }}</span><strong class="font-medium">{{ item.value }}</strong><span>Rooms</span>
            </li>
          </ul>
        </div>
      </article>

      <article class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
        <div class="flex items-center justify-between gap-4">
          <h2 class="text-h5 text-gray-600">Booking Trends by Day</h2>
          <Select v-model="bookingPeriod">
            <SelectTrigger class="w-40"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="month">This month</SelectItem>
              <SelectItem value="last-month">Last month</SelectItem>
              <SelectItem value="last-two-months">Last 2 months</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div class="mt-8 flex min-h-52 gap-3">
          <div class="flex shrink-0 flex-col justify-between pb-7 text-body3 text-gray-600">
            <span v-for="value in [100, 80, 60, 40, 20, 0]" :key="value">{{ value }}%</span>
          </div>
          <div class="relative grid flex-1 grid-cols-7">
            <div aria-hidden="true" class="pointer-events-none absolute inset-0 flex flex-col justify-between pb-7">
              <span v-for="value in 6" :key="value" class="block border-t border-gray-300" />
            </div>
            <div v-for="(value, index) in bookingTrends" :key="weekdayLabels[index]" class="z-10 flex min-w-0 flex-col items-center justify-end">
              <div class="w-2 rounded-full bg-orange-500" :style="{ height: `${(value / maxBookingTrend) * 100}%` }" />
              <span class="mt-3 text-body3 text-gray-700">{{ weekdayLabels[index] }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <h2 class="text-h5 text-gray-600">Revenue Trend</h2>
        <div class="flex flex-wrap items-center gap-3">
          <span class="text-body2 text-gray-600">From</span>
          <DatePicker v-model="revenueFrom" format="compact" class="w-40" :max-value="revenueTo.subtract({ days: 1 })" />
          <span class="text-body2 text-gray-600">to</span>
          <DatePicker v-model="revenueTo" format="compact" class="w-40" :min-value="revenueFrom.add({ days: 1 })" :max-value="currentDate" />
          <Button type="button" class="min-w-28" @click="exportRevenue">Export</Button>
        </div>
      </div>
      <div class="mt-8"><AnalyticsLineChart :labels="revenueTrend.labels" :values="revenueTrend.values" :max="70000" :step="10000" /></div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <h2 class="text-h5 text-gray-600">Occupancy &amp; Guest</h2>
        <div class="flex flex-wrap items-center gap-3">
          <span class="text-body2 text-gray-600">View by</span>
          <Select v-model="occupancyView">
            <SelectTrigger class="w-36"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="overall">Overall</SelectItem>
              <SelectItem value="room-type">Room type</SelectItem>
            </SelectContent>
          </Select>
          <DatePicker v-model="occupancyFrom" format="compact" class="w-40" :max-value="occupancyTo.subtract({ days: 1 })" />
          <span class="text-body2 text-gray-600">to</span>
          <DatePicker v-model="occupancyTo" format="compact" class="w-40" :min-value="occupancyFrom.add({ days: 1 })" :max-value="currentDate" />
          <Button type="button" class="min-w-28" @click="exportOccupancy">Export</Button>
        </div>
      </div>
      <h3 class="mt-10 text-body2 text-gray-700">Occupancy Rate</h3>
      <div class="mt-4"><AnalyticsLineChart :labels="occupancyTrend.labels" :values="occupancyTrend.values" :max="100" :step="20" value-suffix="%" /></div>

      <div class="mt-10 grid gap-14 lg:grid-cols-2">
        <div>
          <h3 class="text-body2 text-gray-700">Guest Visit</h3>
          <ul class="mt-6 flex flex-col gap-6">
            <li v-for="item in guestVisits" :key="item.label">
              <div class="mb-2 flex items-end justify-between gap-3 text-body3">
                <p><strong class="font-medium text-gray-900">{{ item.label }}</strong> <span class="ml-2 text-gray-600">{{ item.detail }}</span></p><strong class="text-body2 text-gray-900">{{ item.value }}%</strong>
              </div>
              <div class="h-2 overflow-hidden rounded-full bg-gray-300"><div class="h-full rounded-full bg-orange-500" :style="{ width: `${item.value}%` }" /></div>
            </li>
          </ul>
        </div>
        <div>
          <h3 class="text-body2 text-gray-700">Payment Method</h3>
          <ul class="mt-6 flex flex-col gap-6">
            <li v-for="(item, index) in paymentMethods" :key="item.label" class="flex gap-4">
              <span class="flex size-10 items-center justify-center rounded-full bg-gray-200 text-gray-700"><component :is="index === 0 ? IconCreditCard : IconCash" class="size-5" /></span>
              <div class="flex-1">
                <div class="mb-2 flex items-end justify-between gap-3 text-body3">
                  <p><strong class="font-medium text-gray-900">{{ item.label }}</strong> <span class="ml-2 text-gray-600">{{ item.detail }}</span></p><strong class="text-body2 text-gray-900">{{ item.value }}%</strong>
                </div>
                <div class="h-2 overflow-hidden rounded-full bg-gray-300"><div class="h-full rounded-full bg-orange-500" :style="{ width: `${item.value}%` }" /></div>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <h2 class="text-h5 text-gray-600">Check-in and Check-out Times Averages</h2>
      <div class="mt-6 grid gap-4 lg:grid-cols-2">
        <article class="flex min-h-28 items-center gap-5 rounded-sm bg-green-100 p-6 text-green-600">
          <span class="flex size-14 shrink-0 items-center justify-center rounded-full bg-green-200"><IconHotel class="size-8" /></span>
          <div class="flex-1"><h3 class="text-h5">Check-in</h3><p class="mt-1 text-body3 text-green-500">Check-in time from 2:00 PM onwards</p></div><strong class="text-h5">4:03 PM</strong>
        </article>
        <article class="flex min-h-28 items-center gap-5 rounded-sm bg-orange-100 p-6 text-orange-500">
          <span class="flex size-14 shrink-0 items-center justify-center rounded-full bg-orange-200"><IconHotel class="size-8" /></span>
          <div class="flex-1"><h3 class="text-h5">Check-out</h3><p class="mt-1 text-body3 text-orange-400">Check-out time by 12:00 PM</p></div><strong class="text-h5">10:32 PM</strong>
        </article>
      </div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <h2 class="text-h5 text-gray-600">Website traffic</h2>
        <div class="flex flex-wrap gap-3">
          <Select v-model="trafficPage">
            <SelectTrigger class="w-40"><SelectValue /></SelectTrigger>
            <SelectContent><SelectItem value="all">All pages</SelectItem><SelectItem value="home">Home</SelectItem><SelectItem value="rooms">Rooms</SelectItem></SelectContent>
          </Select>
          <button v-for="period in [{ value: 'realtime', label: 'Real-time' }, { value: 'yesterday', label: 'Yesterday' }, { value: 'week', label: 'Last 7 days' }, { value: 'month', label: 'Last 30 days' }]" :key="period.value" type="button" :class="['h-12 rounded-sm border px-4 text-body2', trafficPeriod === period.value ? 'border-orange-400 bg-orange-100 text-orange-500' : 'border-gray-400 bg-white text-gray-800']" @click="trafficPeriod = period.value">{{ period.label }}</button>
        </div>
      </div>
      <div class="mt-8"><AnalyticsLineChart :labels="websiteTraffic.labels" :values="websiteTraffic.values" :max="140" :step="20" /></div>
    </section>
  </div>
</template>
