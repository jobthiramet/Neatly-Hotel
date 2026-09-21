<!-- Figma: user > room detail (102:2788 desktop) & mobile > user > room detail -->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ChatbotWidget from '@/components/chatbot/ChatbotWidget.vue'
import { IconArrowRight } from '@/components/icons'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel'
import { rooms as allHomeRooms } from '@/data/home'
import { defaultRoomId, type RoomDetail, roomDetails } from '@/data/rooms'
import { BED_TYPE_LABELS, type RoomResponse, useRoomsStore } from '@/stores/rooms'

const route = useRoute()

// ── Active Room Data ────────────────────────────────────────────────────────
// A UUID (e.g. from Search Result) loads the real room; mock slugs keep using data/rooms.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const param = computed(() => route.params.roomId as string | undefined)
const isApiRoom = computed(() => !!param.value && UUID_RE.test(param.value))

const activeRoomId = computed(() => {
  if (isApiRoom.value)
    return param.value!
  if (param.value && roomDetails[param.value]) {
    return param.value
  }
  return defaultRoomId
})

const roomsStore = useRoomsStore()
const apiRoom = ref<RoomResponse | null>(null)
const apiError = ref('')

watch(param, async () => {
  apiRoom.value = null
  apiError.value = ''
  if (!isApiRoom.value)
    return
  const requested = param.value
  try {
    const loaded = await roomsStore.get(requested!)
    if (requested === param.value)
      apiRoom.value = loaded
  }
  catch {
    if (requested === param.value)
      apiError.value = "We couldn't load this room. It may no longer be available."
  }
}, { immediate: true })

function toRoomDetail(api: RoomResponse): RoomDetail {
  const half = Math.ceil(api.amenities.length / 2)
  return {
    id: api.id,
    name: api.name,
    description: api.description,
    originalPrice: api.pricePerNight,
    currentPrice: api.promotionPrice ?? api.pricePerNight,
    capacity: `${api.capacity} Person`,
    bedType: BED_TYPE_LABELS[api.bedType],
    size: `${api.sizeSqm} sqm`,
    gallery: [api.mainImage, ...api.gallery].filter(image => !!image).map(image => ({ src: image.url, alt: api.name })),
    amenitiesCol1: api.amenities.slice(0, half),
    amenitiesCol2: api.amenities.slice(half),
  }
}

/** Null while a real room is loading or failed to load. */
const room = computed<RoomDetail | null>(() => {
  if (isApiRoom.value)
    return apiRoom.value && toRoomDetail(apiRoom.value)
  return roomDetails[activeRoomId.value] ?? roomDetails[defaultRoomId]!
})

// Embla carousel needs duplicated slides if length is small to enable smooth loop
const galleryPhotos = computed(() => {
  const g = room.value?.gallery ?? []
  return g.length < 5 ? [...g, ...g, ...g] : [...g, ...g]
})

// ── Other Rooms ─────────────────────────────────────────────────────────────
const otherRooms = computed(() => {
  return allHomeRooms.filter(r => r.id !== activeRoomId.value)
})

// ── Format Currency ─────────────────────────────────────────────────────────
function formatPrice(amount: number) {
  return 'THB ' + amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

// Book Now opens the search page with this room type preselected, carrying over an
// active search (the params this page was opened with) when there is one.
const searchLink = computed(() => {
  const carried = ['checkIn', 'checkOut', 'rooms', 'guests']
    .filter(key => typeof route.query[key] === 'string')
    .map(key => [key, route.query[key] as string])
  return {
    name: 'search',
    query: { ...Object.fromEntries(carried), types: activeRoomId.value },
  }
})
</script>

<template>
  <SiteNavbar />

  <main class="bg-bg min-h-screen">
    <p v-if="!room" role="status" class="mx-auto max-w-288 px-4 py-24 text-center text-body1 text-gray-700">
      {{ apiError || 'Loading room…' }}
    </p>

    <template v-else>
    <!-- ── Hero Image Slider ──────────────────────────────────────────────── -->
    <section aria-label="Room gallery" class="relative overflow-hidden pt-4 lg:pt-8">
      <Carousel
        :opts="{ loop: true, align: 'center', startIndex: 0 }"
        aria-label="Room photo gallery"
        class="w-full"
      >
        <CarouselContent class="-ml-2 lg:-ml-4">
          <CarouselItem
            v-for="(photo, index) in galleryPhotos"
            :key="index"
            class="basis-full pl-2 sm:basis-4/5 lg:basis-3/5 lg:pl-4"
          >
            <div class="overflow-hidden rounded-sm bg-gray-200">
              <img
                :src="photo.src"
                :alt="photo.alt"
                width="1200"
                height="800"
                loading="lazy"
                draggable="false"
                class="h-64 w-full object-cover sm:h-96 lg:h-135"
              >
            </div>
          </CarouselItem>
        </CarouselContent>
        <CarouselPrevious aria-label="Previous image" class="left-4 lg:left-12" />
        <CarouselNext aria-label="Next image" class="right-4 lg:right-12" />
      </Carousel>
    </section>

    <!-- ── Room Details Content Container ────────────────────────────────── -->
    <section class="mx-auto max-w-288 px-4 pt-8 pb-16 lg:px-6 lg:pt-16 lg:pb-24">
      <!-- Title -->
      <h1 class="text-center font-serif text-h3 text-green-800 lg:text-h2">
        {{ room.name }}
      </h1>

      <!-- Info & Pricing Row -->
      <div class="mt-8 flex flex-col justify-between gap-6 lg:mt-12 lg:flex-row lg:items-center">
        <p class="max-w-160 text-body1 text-gray-700">
          {{ room.description }}
        </p>

        <div class="flex flex-col items-start lg:items-end">
          <span v-if="room.originalPrice !== room.currentPrice" class="text-body1 text-gray-500 line-through">
            {{ formatPrice(room.originalPrice) }}
          </span>
          <span class="font-sans text-h4 font-semibold text-gray-900">
            {{ formatPrice(room.currentPrice) }}
          </span>
        </div>
      </div>

      <!-- Specs & CTA Row -->
      <div class="mt-6 flex flex-col justify-between gap-6 sm:flex-row sm:items-center lg:mt-8">
        <!-- Specs inline bar -->
        <div class="flex items-center gap-3 text-body1 text-gray-700 sm:gap-4">
          <span>{{ room.capacity }}</span>
          <span class="text-gray-300" aria-hidden="true">|</span>
          <span>{{ room.bedType }}</span>
          <span class="text-gray-300" aria-hidden="true">|</span>
          <span>{{ room.size }}</span>
        </div>

        <!-- Book Now Button (full width on mobile, auto on desktop) -->
        <Button as-child class="w-full px-8 py-4 sm:w-auto">
          <RouterLink :to="searchLink">
            Book Now
          </RouterLink>
        </Button>
      </div>

      <!-- Divider -->
      <hr class="my-10 border-gray-300 lg:my-16">

      <!-- Room Amenities -->
      <div>
        <h2 class="font-sans text-h4 font-semibold text-gray-900">
          Room Amenities
        </h2>

        <div class="mt-6 grid grid-cols-2 gap-x-4 gap-y-3 sm:gap-x-12 max-w-200">
          <!-- Col 1 -->
          <ul class="list-disc pl-5 space-y-3 text-body2 sm:text-body1 text-gray-700">
            <li v-for="amenity in room.amenitiesCol1" :key="amenity">
              {{ amenity }}
            </li>
          </ul>

          <!-- Col 2 -->
          <ul class="list-disc pl-5 space-y-3 text-body2 sm:text-body1 text-gray-700">
            <li v-for="amenity in room.amenitiesCol2" :key="amenity">
              {{ amenity }}
            </li>
          </ul>
        </div>
      </div>
    </section>

    </template>

    <!-- ── Other Rooms Section ────────────────────────────────────────────── -->
    <section aria-labelledby="other-rooms-title" class="bg-bg border-t border-gray-300 pt-12 pb-16 lg:pt-20 lg:pb-28">
      <div class="mx-auto max-w-288 px-4 lg:px-6">
        <h2 id="other-rooms-title" class="text-center font-serif text-h3 text-green-800 lg:text-h2">
          Other Rooms
        </h2>

        <Carousel
          :opts="{ loop: true, align: 'start' }"
          aria-label="Other rooms carousel"
          class="mt-10 lg:mt-14"
        >
          <CarouselContent class="-ml-4">
            <CarouselItem
              v-for="other in otherRooms"
              :key="other.id"
              class="basis-full pl-4 md:basis-1/2 lg:basis-1/3"
            >
              <div class="group relative isolate h-75 sm:h-85 lg:h-95 overflow-hidden rounded-sm">
                <img
                  :src="other.image"
                  :alt="other.alt"
                  width="800"
                  height="600"
                  loading="lazy"
                  class="absolute inset-0 -z-10 size-full object-cover transition-transform duration-500 group-hover:scale-105"
                >
                <div class="absolute inset-0 -z-10 bg-linear-to-t from-black/70 via-black/20 to-black/0" />
                <div class="flex h-full flex-col justify-end p-6 lg:p-8">
                  <h3 class="font-serif text-h4 font-medium text-white lg:text-h3">
                    {{ other.name }}
                  </h3>
                  <RouterLink
                    :to="`/rooms/${other.id}`"
                    class="mt-3 inline-flex w-fit items-center gap-2 rounded-sm font-button text-button text-white outline-none is-hover:underline is-focus:ring-2 is-focus:ring-ring"
                  >
                    Explore Room <span class="sr-only">{{ other.name }}</span>
                    <IconArrowRight class="size-4" />
                  </RouterLink>
                </div>
              </div>
            </CarouselItem>
          </CarouselContent>

          <!-- Centered Carousel Controls underneath -->
          <div class="mt-8 flex justify-center gap-4 lg:mt-12">
            <CarouselPrevious
              aria-label="Previous other rooms"
              class="static translate-y-0 border-gray-400 bg-white text-gray-700 is-hover:border-orange-500 is-hover:bg-orange-100 is-hover:text-orange-500"
            />
            <CarouselNext
              aria-label="Next other rooms"
              class="static translate-y-0 border-gray-400 bg-white text-gray-700 is-hover:border-orange-500 is-hover:bg-orange-100 is-hover:text-orange-500"
            />
          </div>
        </Carousel>
      </div>
    </section>
  </main>

  <SiteFooter />

  <ChatbotWidget />
</template>
