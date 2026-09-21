<!-- Figma: home / hero section search box (12:214 desktop, 7410:3835 mobile); search result / search box (17:281, 7410:4580) -->
<script lang="ts">
import type { DateValue } from '@internationalized/date'
import { getLocalTimeZone, parseDate, today } from '@internationalized/date'

export interface RoomSearchQuery {
  checkIn: DateValue
  checkOut: DateValue
  rooms: number
  guests: number
  /** Room type ids to search; empty means every type. */
  types: string[]
}

/** Longest stay the API accepts (`app.search.max-nights`). */
export const MAX_NIGHTS = 30

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/** `/search` URL query for a search; ISO dates keep it shareable. */
export function toSearchRouteQuery(query: RoomSearchQuery) {
  return {
    checkIn: query.checkIn.toString(),
    checkOut: query.checkOut.toString(),
    rooms: String(query.rooms),
    guests: String(query.guests),
    ...(query.types.length ? { types: query.types.join(',') } : {}),
  }
}

/** Reads a `/search` URL query. Unparseable fields come back undefined. */
export function fromSearchRouteQuery(query: Record<string, unknown>): Partial<RoomSearchQuery> {
  const date = (value: unknown) => {
    try {
      return typeof value === 'string' ? parseDate(value) : undefined
    }
    catch {
      return undefined
    }
  }
  const count = (value: unknown) => {
    const number = Number(value)
    return Number.isInteger(number) && number > 0 ? number : undefined
  }
  // Only real ids reach the API; mock slugs and junk are dropped instead of 400ing.
  const types = typeof query.types === 'string'
    ? query.types.split(',').filter(value => UUID_RE.test(value))
    : []
  return { checkIn: date(query.checkIn), checkOut: date(query.checkOut), rooms: count(query.rooms), guests: count(query.guests), types }
}

/** Field errors for a search, or `{}` when it can be sent. */
export function validateSearch(query: Partial<RoomSearchQuery>) {
  const errors: { checkIn?: string, checkOut?: string, occupancy?: string } = {}
  const todayDate = today(getLocalTimeZone())
  if (!query.checkIn)
    errors.checkIn = 'Please select a check-in date.'
  else if (query.checkIn.compare(todayDate) < 0)
    errors.checkIn = 'Check-in date cannot be in the past.'
  if (!query.checkOut)
    errors.checkOut = 'Please select a check-out date.'
  else if (query.checkIn && query.checkOut.compare(query.checkIn) <= 0)
    errors.checkOut = 'Check-out must be after check-in.'
  else if (query.checkIn && query.checkOut.compare(query.checkIn.add({ days: MAX_NIGHTS })) > 0)
    errors.checkOut = `Stays can be at most ${MAX_NIGHTS} nights.`
  if (!query.rooms || !query.guests || query.rooms > 10 || query.guests > 6)
    errors.occupancy = 'Please select rooms and guests.'
  return errors
}
</script>

<script setup lang="ts">
import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { FormField } from '@/components/ui/form-field'
import { MultiSelect } from '@/components/ui/multi-select'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useRoomsStore } from '@/stores/rooms'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  /** Pre-fills the form (e.g. from the URL); errors show straight away. */
  initial?: Partial<RoomSearchQuery>
  /** `hero`: white card on the home page. `bar`: plain row in the Search Result header. */
  variant?: 'hero' | 'bar'
}>(), {
  variant: 'hero',
})

const emit = defineEmits<{
  search: [query: RoomSearchQuery]
}>()

const baseOptions = [
  { value: '1-1', label: '1 room, 1 guest' },
  { value: '1-2', label: '1 room, 2 guests' },
  { value: '1-3', label: '1 room, 3 guests' },
  { value: '2-4', label: '2 rooms, 4 guests' },
]

const minDate = today(getLocalTimeZone())
const checkIn = shallowRef<DateValue | undefined>()
const checkOut = shallowRef<DateValue | undefined>()
const occupancy = ref('1-2')
const selectedTypes = ref<string[]>([])
const submitted = ref(false)

// Options come from the rooms API once per session; ids in the URL that no longer exist are dropped.
const roomsStore = useRoomsStore()
const typeOptions = computed(() => roomsStore.roomTypes.map(type => ({ value: type.id, label: type.name })))
onMounted(() => roomsStore.types().catch(() => {}))
watch(typeOptions, (options) => {
  if (options.length)
    selectedTypes.value = selectedTypes.value.filter(id => options.some(option => option.value === id))
})

function reset(initial: Partial<RoomSearchQuery> | undefined) {
  checkIn.value = initial ? initial.checkIn : minDate
  checkOut.value = initial ? initial.checkOut : minDate.add({ days: 1 })
  occupancy.value = initial?.rooms && initial.guests ? `${initial.rooms}-${initial.guests}` : '1-2'
  selectedTypes.value = initial?.types ?? []
  submitted.value = !!initial
}
// Back/forward on /search changes `initial` without remounting.
watch(() => props.initial, reset, { immediate: true })

// A shared link can carry a combination the menu doesn't list; keep it selectable.
const occupancyOptions = computed(() => {
  if (baseOptions.some(option => option.value === occupancy.value))
    return baseOptions
  const [rooms, guests] = occupancy.value.split('-').map(Number)
  return [...baseOptions, { value: occupancy.value, label: `${rooms} room${rooms === 1 ? '' : 's'}, ${guests} guest${guests === 1 ? '' : 's'}` }]
})

// Check-out moves with check-in so it stays at least one night later.
watch(checkIn, (value) => {
  if (value && (!checkOut.value || checkOut.value.compare(value) <= 0))
    checkOut.value = value.add({ days: 1 })
})

const current = computed(() => {
  const [rooms, guests] = occupancy.value.split('-').map(Number)
  return { checkIn: checkIn.value, checkOut: checkOut.value, rooms, guests, types: selectedTypes.value }
})
const errors = computed(() => submitted.value ? validateSearch(current.value) : {})

function submit() {
  submitted.value = true
  if (Object.keys(errors.value).length)
    return
  emit('search', current.value as RoomSearchQuery)
}
</script>

<template>
  <form
    role="search"
    aria-label="Search rooms"
    novalidate
    :class="cn(
      'flex flex-col gap-4 lg:flex-row lg:items-start',
      variant === 'hero' ? 'rounded-sm bg-white p-4 shadow-md lg:gap-4 lg:p-10 xl:gap-10' : 'lg:gap-4 xl:gap-10',
    )"
    @submit.prevent="submit"
  >
    <div class="flex flex-col gap-4 lg:flex-2 lg:flex-row lg:items-start lg:gap-4 xl:gap-6">
      <FormField label="Check In" for="check-in" :error="errors.checkIn" class="lg:min-w-40">
        <DatePicker
          id="check-in"
          v-model="checkIn"
          format="compact"
          :min-value="minDate"
          :invalid="!!errors.checkIn"
          :aria-describedby="errors.checkIn ? 'check-in-error' : undefined"
        />
      </FormField>
      <span aria-hidden="true" class="hidden pt-10 text-body1 text-gray-900 lg:block">-</span>
      <FormField label="Check Out" for="check-out" :error="errors.checkOut" class="lg:min-w-40">
        <DatePicker
          id="check-out"
          v-model="checkOut"
          format="compact"
          :min-value="(checkIn ?? minDate).add({ days: 1 })"
          :max-value="(checkIn ?? minDate).add({ days: MAX_NIGHTS })"
          :invalid="!!errors.checkOut"
          :aria-describedby="errors.checkOut ? 'check-out-error' : undefined"
        />
      </FormField>
    </div>

    <FormField label="Rooms & Guests" for="occupancy" :error="errors.occupancy" class="lg:min-w-0 lg:flex-1">
      <Select v-model="occupancy">
        <SelectTrigger id="occupancy">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem v-for="option in occupancyOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </SelectItem>
        </SelectContent>
      </Select>
    </FormField>

    <FormField label="Room Types" for="room-types" class="lg:min-w-0 lg:flex-1">
      <MultiSelect
        id="room-types"
        v-model="selectedTypes"
        :options="typeOptions"
        all-label="All room types"
        :count-label="count => count === 1 ? '1 room type selected' : `${count} room types selected`"
      />
    </FormField>

    <Button
      type="submit"
      :variant="variant === 'bar' ? 'secondary' : 'primary'"
      class="mt-2 lg:mt-7 lg:w-32 xl:w-36"
    >
      Search
    </Button>
  </form>
</template>
