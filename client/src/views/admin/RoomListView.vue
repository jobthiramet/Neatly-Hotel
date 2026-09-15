<script setup lang="ts">
import type { PageResponse, RoomSummary } from '@/stores/rooms'
import { watchDebounced } from '@vueuse/core'
import { isAxiosError, isCancel } from 'axios'
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationNext, PaginationPrevious } from '@/components/ui/pagination'
import { apiErrorMessage } from '@/stores/hotel'
import { BED_TYPE_LABELS, useRoomsStore } from '@/stores/rooms'

const PAGE_SIZE = 10

const rooms = useRoomsStore()
const route = useRoute()
const router = useRouter()

// URL query is the source of truth (`?search=&page=`, page is 1-based), so refresh and back keep the state.
const querySearch = computed(() => (typeof route.query.search === 'string' ? route.query.search : ''))
const queryPage = computed(() => Math.max(1, Number.parseInt(String(route.query.page ?? '1'), 10) || 1))

const search = ref(querySearch.value)
const result = ref<PageResponse<RoomSummary> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const money = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

let controller: AbortController | null = null

async function load() {
  // Cancel the superseded request; only the latest one may update the table.
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = null
  try {
    const page = await rooms.list({ search: querySearch.value, page: queryPage.value - 1, size: PAGE_SIZE }, { signal: current.signal })
    if (controller !== current)
      return // a newer request already started
    // A deleted room or stale link can leave ?page= past the end: go to the last page instead.
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
      toast.warning('Too many searches, please wait a moment.') // keep the current results
    else
      error.value = apiErrorMessage(e, 'Could not load rooms.')
  }
  finally {
    if (controller === current)
      loading.value = false
  }
}

watch(() => [querySearch.value, queryPage.value], load, { immediate: true })

// Back/forward changes the query; keep the input in sync.
watch(querySearch, (value) => {
  search.value = value
})

// A new search always starts at page 1.
function applySearch(value: string) {
  const term = value.trim()
  if (term === querySearch.value)
    return
  router.replace({ query: { ...route.query, search: term || undefined, page: undefined } })
}

watchDebounced(search, applySearch, { debounce: 300 })

// Clearing the box shows all rooms right away; Enter skips the debounce.
watch(search, (value) => {
  if (!value.trim())
    applySearch('')
})

function goToPage(page: number, replace = false) {
  const location = { query: { ...route.query, page: page > 1 ? page : undefined } }
  if (replace)
    router.replace(location)
  else
    router.push(location)
}

function openRoom(id: string) {
  router.push({ name: 'admin-room-edit', params: { id } })
}
</script>

<!-- Figma: (admin) room & property -->
<template>
  <Teleport defer to="#admin-header-actions">
    <Input v-model="search" type="search" placeholder="Search..." aria-label="Search rooms" class="w-80" @keydown.enter="applySearch(search)" />
    <Button as-child>
      <RouterLink :to="{ name: 'admin-room-create' }">
        + Create Room
      </RouterLink>
    </Button>
  </Teleport>

  <div v-if="error" role="alert" class="flex flex-col items-start gap-4 rounded-sm bg-white px-6 py-10 lg:px-20">
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
              Image
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Room type
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Price
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Promotion Price
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Guest(s)
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Bed Type
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Room Size
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !result">
            <td colspan="7" role="status" class="px-4 py-10 text-center text-body1 text-gray-700">
              Loading rooms…
            </td>
          </tr>
          <tr v-else-if="result && result.content.length === 0">
            <td colspan="7" class="px-4 py-10 text-center text-body1 text-gray-700">
              {{ querySearch ? `No rooms match “${querySearch}”.` : 'No rooms yet.' }}
            </td>
          </tr>
          <tr
            v-for="room in result?.content"
            :key="room.id"
            class="cursor-pointer border-t border-gray-200 is-hover:bg-gray-100"
            @click="openRoom(room.id)"
          >
            <td class="px-4 py-4">
              <img v-if="room.mainImageUrl" :src="room.mainImageUrl" alt="" class="h-18 w-30 rounded-sm object-cover">
              <div v-else class="h-18 w-30 rounded-sm bg-gray-200" />
            </td>
            <td class="px-4 py-4">
              <!-- The link gives keyboard users the same action as the row click. -->
              <RouterLink
                :to="{ name: 'admin-room-edit', params: { id: room.id } }"
                class="rounded-sm outline-none is-focus:ring-2 is-focus:ring-orange-500"
                @click.stop
              >
                {{ room.name }}
              </RouterLink>
            </td>
            <td class="px-4 py-4">
              {{ money.format(room.pricePerNight) }}
            </td>
            <td class="px-4 py-4">
              {{ room.promotionPrice == null ? '-' : money.format(room.promotionPrice) }}
            </td>
            <td class="px-4 py-4">
              {{ room.capacity }}
            </td>
            <td class="px-4 py-4">
              {{ BED_TYPE_LABELS[room.bedType] }}
            </td>
            <td class="px-4 py-4">
              {{ room.sizeSqm }} sqm
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
