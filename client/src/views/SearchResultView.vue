<!-- Figma: search result (17:281 desktop, 7410:4580 mobile) -->
<script setup lang="ts">
import { isAxiosError } from 'axios'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatbotWidget from '@/components/chatbot/ChatbotWidget.vue'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import RoomResultCard from '@/components/RoomResultCard.vue'
import RoomSearchForm, { fromSearchRouteQuery, type RoomSearchQuery, toSearchRouteQuery, validateSearch } from '@/components/RoomSearchForm.vue'
import { Button } from '@/components/ui/button'
import { type AvailableRoom, useRoomsStore } from '@/stores/rooms'

const route = useRoute()
const router = useRouter()
const roomsStore = useRoomsStore()

// The URL is the source of truth, so refresh, back/forward and shared links all work.
const initial = computed(() => fromSearchRouteQuery(route.query))
const valid = computed(() => Object.keys(validateSearch(initial.value)).length === 0)
const apiQuery = computed(() => valid.value ? toSearchRouteQuery(initial.value as RoomSearchQuery) : null)
// Ids that no longer exist are dropped, so the results match what the filter shows.
const knownTypes = computed(() => new Set(roomsStore.roomTypes.map(type => type.id)))
const typesReady = computed(() => roomsStore.roomTypes.length > 0)
const params = computed(() => apiQuery.value && {
  checkIn: apiQuery.value.checkIn,
  checkOut: apiQuery.value.checkOut,
  rooms: Number(apiQuery.value.rooms),
  guests: Number(apiQuery.value.guests),
  roomTypeIds: (initial.value.types ?? []).filter(id => knownTypes.value.has(id)),
})

const rooms = ref<AvailableRoom[]>([])
const loading = ref(false)
const error = ref('')
let latest = 0

async function load() {
  const query = params.value
  error.value = ''
  // With a filter in the URL, wait for the type list so unknown ids are dropped first.
  // Stay in the loading state meanwhile: an empty list here is not "nothing available".
  if (query && initial.value.types?.length && !typesReady.value) {
    loading.value = true
    return
  }
  if (!query) {
    rooms.value = []
    return
  }
  const request = ++latest
  loading.value = true
  try {
    const result = await roomsStore.available(query)
    if (request === latest)
      rooms.value = result
  }
  catch (err) {
    if (request !== latest)
      return
    rooms.value = []
    error.value = isAxiosError(err) && err.response?.status === 429
      ? 'Too many searches right now. Please wait a moment and try again.'
      : 'We couldn\'t load available rooms. Please try again.'
  }
  finally {
    if (request === latest)
      loading.value = false
  }
}
watch([apiQuery, typesReady], load, { immediate: true, deep: true })

function search(query: RoomSearchQuery) {
  const next = toSearchRouteQuery(query)
  // Same search again: the URL won't change, so refetch directly.
  if (JSON.stringify(next) === JSON.stringify(apiQuery.value))
    load()
  else
    router.push({ name: 'search', query: next })
}
</script>

<template>
  <SiteNavbar />

  <main class="min-h-screen bg-bg">
    <section aria-label="Change search" class="bg-white px-4 py-6 shadow-md lg:py-10">
      <RoomSearchForm :initial="initial" variant="bar" class="mx-auto max-w-250" @search="search" />
    </section>

    <section aria-label="Search results" aria-live="polite" class="mx-auto max-w-280 px-4 py-10 lg:py-21">
      <div v-if="loading" aria-busy="true" class="flex flex-col gap-10">
        <span class="sr-only">Loading available rooms…</span>
        <div v-for="index in 3" :key="index" class="flex animate-pulse flex-col gap-6 border-b border-gray-300 pb-10 lg:flex-row lg:gap-12">
          <div class="aspect-4/3 w-full rounded-sm bg-gray-300 lg:w-113" />
          <div class="flex flex-1 flex-col gap-4">
            <div class="h-8 w-1/2 rounded-sm bg-gray-300" />
            <div class="h-5 w-2/3 rounded-sm bg-gray-300" />
            <div class="h-5 w-full rounded-sm bg-gray-300" />
          </div>
        </div>
      </div>

      <div v-else-if="error" class="flex flex-col items-center gap-4 py-10 text-center">
        <p class="text-body1 text-gray-700">
          {{ error }}
        </p>
        <Button variant="secondary" @click="load">
          Try again
        </Button>
      </div>

      <template v-else-if="params">
        <p v-if="rooms.length === 0" class="py-10 text-center text-body1 text-gray-700">
          Sorry, no rooms were available for the selected dates.
        </p>
        <div v-else class="flex flex-col gap-10">
          <RoomResultCard v-for="room in rooms" :key="room.id" :room="room" :query="params" />
        </div>
      </template>
    </section>
  </main>

  <SiteFooter />
  <ChatbotWidget />
</template>
