<script setup lang="ts">
import type { Component } from 'vue'
import {
  IconBooking,
  IconCalendar,
  IconChartPie,
  IconCreditCard,
  IconHotel,
} from '@/components/icons'
import { Button } from '@/components/ui/button'

const summaryCards: { label: string, icon: Component }[] = [
  { label: 'Total booking', icon: IconBooking },
  { label: 'Total sales', icon: IconCreditCard },
  { label: 'Total booking users', icon: IconCalendar },
  { label: 'Total site visitors', icon: IconChartPie },
]
</script>

<template>
  <div class="flex flex-col gap-8">
    <section aria-label="Analytics summary" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article
        v-for="card in summaryCards"
        :key="card.label"
        class="flex min-h-32 items-start justify-between gap-4 rounded-sm border border-gray-300 bg-white p-4"
      >
        <div class="flex flex-col gap-3">
          <h2 class="text-body2 text-gray-800">
            {{ card.label }}
          </h2>
          <span aria-hidden="true" class="text-h4 text-gray-900">—</span>
          <span class="text-body3 text-gray-600">No data available</span>
        </div>
        <span class="flex size-12 shrink-0 items-center justify-center rounded-full bg-gray-200 text-gray-700">
          <component :is="card.icon" class="size-6" />
        </span>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <h2 class="text-h5 text-gray-600">Room Availability</h2>
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            This month <span aria-hidden="true">⌄</span>
          </button>
        </div>
        <div class="mt-8 flex min-h-64 items-center justify-center rounded-sm border border-dashed border-gray-400 text-body2 text-gray-600">
          Room availability chart
        </div>
      </article>

      <article class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <h2 class="text-h5 text-gray-600">Booking Trends by Day</h2>
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            This month <span aria-hidden="true">⌄</span>
          </button>
        </div>
        <div class="mt-8 flex min-h-64 items-center justify-center rounded-sm border border-dashed border-gray-400 text-body2 text-gray-600">
          Booking trend chart
        </div>
      </article>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <h2 class="text-h5 text-gray-600">Revenue Trend</h2>
        <div class="flex flex-wrap items-center gap-3">
          <span class="text-body2 text-gray-600">From</span>
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            Select date <IconCalendar class="size-5 text-gray-600" />
          </button>
          <span class="text-body2 text-gray-600">to</span>
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            Select date <IconCalendar class="size-5 text-gray-600" />
          </button>
          <Button type="button">Export</Button>
        </div>
      </div>
      <div class="mt-8 flex min-h-80 items-center justify-center rounded-sm border border-dashed border-gray-400 text-body2 text-gray-600">
        Revenue trend chart
      </div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <h2 class="text-h5 text-gray-600">Occupancy &amp; Guest</h2>
        <div class="flex flex-wrap items-center gap-3">
          <span class="text-body2 text-gray-600">View by</span>
          <button type="button" class="flex h-12 min-w-36 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            Overall <span aria-hidden="true">⌄</span>
          </button>
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            Select date <IconCalendar class="size-5 text-gray-600" />
          </button>
          <span class="text-body2 text-gray-600">to</span>
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            Select date <IconCalendar class="size-5 text-gray-600" />
          </button>
          <Button type="button">Export</Button>
        </div>
      </div>

      <h3 class="mt-10 text-body2 text-gray-700">Occupancy Rate</h3>
      <div class="mt-4 flex min-h-80 items-center justify-center rounded-sm border border-dashed border-gray-400 text-body2 text-gray-600">
        Occupancy rate chart
      </div>

      <div class="mt-10 grid gap-10 lg:grid-cols-2">
        <div>
          <h3 class="text-body2 text-gray-700">Guest Visit</h3>
          <div class="mt-5 flex min-h-28 items-center justify-center rounded-sm bg-gray-100 text-body2 text-gray-600">
            Guest visit summary
          </div>
        </div>
        <div>
          <h3 class="text-body2 text-gray-700">Payment Method</h3>
          <div class="mt-5 flex min-h-28 items-center justify-center rounded-sm bg-gray-100 text-body2 text-gray-600">
            Payment method summary
          </div>
        </div>
      </div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <h2 class="text-h5 text-gray-600">Check-in and Check-out Times Averages</h2>
      <div class="mt-6 grid gap-4 lg:grid-cols-2">
        <article class="flex min-h-28 items-center gap-5 rounded-sm bg-green-100 p-6 text-green-600">
          <span class="flex size-14 shrink-0 items-center justify-center rounded-full bg-green-200">
            <IconHotel class="size-7" />
          </span>
          <div class="flex-1">
            <h3 class="text-h5">Check-in</h3>
            <p class="mt-1 text-body3 text-green-500">No average time available</p>
          </div>
          <span aria-hidden="true" class="text-h5">—</span>
        </article>
        <article class="flex min-h-28 items-center gap-5 rounded-sm bg-orange-100 p-6 text-orange-500">
          <span class="flex size-14 shrink-0 items-center justify-center rounded-full bg-orange-200">
            <IconHotel class="size-7" />
          </span>
          <div class="flex-1">
            <h3 class="text-h5">Check-out</h3>
            <p class="mt-1 text-body3 text-orange-400">No average time available</p>
          </div>
          <span aria-hidden="true" class="text-h5">—</span>
        </article>
      </div>
    </section>

    <section class="rounded-sm border border-gray-300 bg-white p-6 lg:p-10">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <h2 class="text-h5 text-gray-600">Website traffic</h2>
        <div class="flex flex-wrap gap-3">
          <button type="button" class="flex h-12 min-w-40 items-center justify-between gap-4 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">
            All pages <span aria-hidden="true">⌄</span>
          </button>
          <button type="button" class="h-12 rounded-sm border border-orange-400 bg-orange-100 px-4 text-body2 text-orange-500">Real-time</button>
          <button type="button" class="h-12 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">Yesterday</button>
          <button type="button" class="h-12 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">Last 7 days</button>
          <button type="button" class="h-12 rounded-sm border border-gray-400 px-4 text-body2 text-gray-800">Last 30 days</button>
        </div>
      </div>
      <div class="mt-8 flex min-h-80 items-center justify-center rounded-sm border border-dashed border-gray-400 text-body2 text-gray-600">
        Website traffic chart
      </div>
    </section>
  </div>
</template>
