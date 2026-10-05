<!-- Figma: room card (7410:4984, Desktop / Mobile) -->
<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Button } from '@/components/ui/button'
import { formatThb } from '@/data/booking'
import { type AvailableRoom, BED_TYPE_LABELS } from '@/stores/rooms'

const props = defineProps<{
  room: AvailableRoom
  /** The search the card came from, as ISO dates; carried into Book Now. */
  query: { checkIn: string, checkOut: string, rooms: number, guests: number }
}>()

const price = computed(() => props.room.promotionPrice ?? props.room.pricePerNight)

const stay = computed(() => ({
  checkIn: props.query.checkIn,
  checkOut: props.query.checkOut,
  rooms: String(props.query.rooms),
  guests: String(props.query.guests),
}))
const bookingLink = computed(() => ({ name: 'booking', query: { roomId: props.room.id, ...stay.value } }))
// The stay rides along so the detail page's Book Now keeps it.
const detailLink = computed(() => ({ name: 'room-detail', params: { roomId: props.room.id }, query: stay.value }))
</script>

<template>
  <article class="flex flex-col gap-6 border-b border-gray-300 pb-10 lg:flex-row lg:gap-12">
    <img
      v-if="room.mainImageUrl"
      :src="room.mainImageUrl"
      :alt="room.name"
      loading="lazy"
      class="aspect-4/3 w-full rounded-sm object-cover lg:w-113 lg:shrink-0"
    >
    <div v-else aria-hidden="true" class="aspect-4/3 w-full rounded-sm bg-gray-200 lg:w-113 lg:shrink-0" />

    <div class="flex flex-1 flex-col gap-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:justify-between lg:gap-6">
        <div class="flex flex-col gap-4">
          <h2 class="text-h4 text-black">
            {{ room.name }}
          </h2>
          <p class="flex flex-wrap items-center gap-x-4 gap-y-1 text-body1 text-gray-700">
            <span>{{ room.capacity }} Guests</span>
            <span aria-hidden="true" class="h-4 w-px bg-gray-500" />
            <span>{{ BED_TYPE_LABELS[room.bedType] }}</span>
            <span aria-hidden="true" class="h-4 w-px bg-gray-500" />
            <span>{{ room.sizeSqm }} sqm</span>
          </p>
          <p class="text-body1 text-gray-700">
            {{ room.description }}
          </p>
        </div>

        <div class="flex shrink-0 flex-col lg:items-end lg:text-right">
          <p v-if="room.promotionPrice !== null" class="text-body1 text-gray-700 line-through">
            THB {{ formatThb(room.pricePerNight) }}
          </p>
          <p class="text-h5 text-gray-900">
            THB {{ formatThb(price) }}
          </p>
          <p class="mt-2 text-body1 text-gray-700">
            Per Night
          </p>
          <p class="text-body1 text-gray-700">
            (Including Taxes &amp; Fees)
          </p>
        </div>
      </div>

      <div class="flex items-center justify-end gap-6 lg:mt-auto">
        <Button variant="ghost" as-child>
          <RouterLink :to="detailLink">
            Room Detail
          </RouterLink>
        </Button>
        <Button as-child>
          <RouterLink :to="bookingLink">
            Book Now
          </RouterLink>
        </Button>
      </div>
    </div>
  </article>
</template>
