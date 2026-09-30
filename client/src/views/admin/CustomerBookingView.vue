<script setup lang="ts">
import type { AdminBookingSummary, PageResponse } from '@/api/adminBookings'
import { listAdminBookings } from '@/api/adminBookings'
import { watchDebounced } from '@vueuse/core'
import { isAxiosError, isCancel } from 'axios'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationNext, PaginationPrevious } from '@/components/ui/pagination'
import { apiErrorMessage } from '@/stores/hotel'
import { BED_TYPE_LABELS } from '@/stores/rooms'

const PAGE_SIZE = 10

const route = useRoute()
const router = useRouter()

const querySearch = computed(() => (typeof route.query.search === 'string' ? route.query.search : ''))
const queryPage = computed(() => Math.max(1, Number.parseInt(String(route.query.page ?? '1'), 10) || 1))

const search = ref(querySearch.value)
const result = ref<PageResponse<AdminBookingSummary> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const dateFormat = new Intl.DateTimeFormat('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

let controller: AbortController | null = null

function formatStayDate(iso: string) {
  const [year, month, day] = iso.split('-').map(Number)
  if (!year || !month || !day)
    return iso
  return dateFormat.format(new Date(Date.UTC(year, month - 1, day)))
}

function bedLabel(booking: AdminBookingSummary) {
  return booking.bedType ? BED_TYPE_LABELS[booking.bedType] : '—'
}

async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = null
  try {
    const page = await listAdminBookings({
      search: querySearch.value,
      page: queryPage.value - 1,
      size: PAGE_SIZE,
    }, { signal: current.signal })
    if (controller !== current)
      return
    if (page.totalPages > 0 && queryPage.value > page.totalPages) {
      goToPage(page.totalPages, true)
      return
    }
    result.value = page
  }
  catch (e) {
    if (isCancel(e) || controller !== current)
      return
    if (isAxiosError(e) && e.response?.status === 429)
      toast.warning('Too many searches, please wait a moment.')
    else
      error.value = apiErrorMessage(e, 'Could not load customer bookings.')
  }
  finally {
    if (controller === current)
      loading.value = false
  }
}

watch(() => [querySearch.value, queryPage.value], load, { immediate: true })

watch(querySearch, (value) => {
  search.value = value
})

function applySearch(value: string) {
  const term = value.trim()
  if (term === querySearch.value)
    return
  router.replace({ query: { ...route.query, search: term || undefined, page: undefined } })
}

watchDebounced(search, applySearch, { debounce: 300 })

function goToPage(page: number, replace = false) {
  const next = Math.max(1, page)
  const query = { ...route.query, page: next <= 1 ? undefined : String(next) }
  if (replace)
    router.replace({ query })
  else
    router.push({ query })
}

function openBooking(id: string) {
  router.push({ name: 'admin-customer-booking-detail', params: { id } })
}
</script>

<template>
  <Teleport defer to="#admin-header-actions">
    <Input
      v-model="search"
      type="search"
      placeholder="Search..."
      aria-label="Search customer bookings"
      class="w-80"
    />
  </Teleport>

  <div v-if="error" role="alert" class="flex flex-col items-start gap-4 rounded-sm bg-white px-6 py-10">
    <p class="text-body1 text-red">
      {{ error }}
    </p>
    <Button variant="secondary" @click="load">
      Try again
    </Button>
  </div>

  <template v-else>
    <div class="overflow-x-auto rounded-sm bg-white transition-opacity" :class="loading && result && 'opacity-60'">
      <table class="w-full min-w-200 text-left text-body2 text-black" :aria-busy="loading">
        <thead class="bg-gray-300 text-gray-800">
          <tr>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Customer name
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Guest(s)
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Room type
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Amount
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Bed Type
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Check-in
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Check-out
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !result">
            <td colspan="7" role="status" class="px-4 py-10 text-center text-body1 text-gray-700">
              Loading bookings…
            </td>
          </tr>
          <tr v-else-if="result && result.content.length === 0">
            <td colspan="7" class="px-4 py-10 text-center text-body1 text-gray-700">
              {{ querySearch ? `No bookings match “${querySearch}”.` : 'No customer bookings yet.' }}
            </td>
          </tr>
          <tr
            v-for="booking in result?.content"
            :key="booking.id"
            tabindex="0"
            class="cursor-pointer border-t border-gray-200 is-hover:bg-gray-100"
            @click="openBooking(booking.id)"
            @keydown.enter.prevent="openBooking(booking.id)"
          >
            <td class="px-4 py-4">
              {{ booking.customerName }}
            </td>
            <td class="px-4 py-4">
              {{ booking.guests }}
            </td>
            <td class="px-4 py-4">
              {{ booking.roomType }}
            </td>
            <td class="px-4 py-4">
              {{ booking.roomsCount }}
            </td>
            <td class="px-4 py-4">
              {{ bedLabel(booking) }}
            </td>
            <td class="px-4 py-4">
              {{ formatStayDate(booking.checkIn) }}
            </td>
            <td class="px-4 py-4">
              {{ formatStayDate(booking.checkOut) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Pagination
      v-if="result"
      class="mt-10"
      aria-label="Pagination"
      :total="result.totalElements"
      :items-per-page="PAGE_SIZE"
      :page="queryPage"
      :sibling-count="1"
      show-edges
      @update:page="goToPage"
    >
      <PaginationContent v-slot="{ items }">
        <PaginationPrevious :aria-disabled="queryPage <= 1 || undefined" />
        <template v-for="(item, index) in items" :key="index">
          <PaginationItem v-if="item.type === 'page'" :value="item.value" />
          <PaginationEllipsis v-else :index="index" />
        </template>
        <PaginationNext :aria-disabled="queryPage >= Math.max(1, result.totalPages) || undefined" />
      </PaginationContent>
    </Pagination>
  </template>
</template>
