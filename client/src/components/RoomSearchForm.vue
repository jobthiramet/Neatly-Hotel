<!-- Figma: home / hero section search box (12:214 desktop, 7410:3835 mobile) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { getLocalTimeZone, today } from '@internationalized/date'
import { ref, shallowRef, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { FormField } from '@/components/ui/form-field'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

export interface RoomSearchQuery {
  checkIn: DateValue
  checkOut: DateValue
  rooms: number
  guests: number
}

const emit = defineEmits<{
  search: [query: RoomSearchQuery]
}>()

const occupancyOptions = [
  { value: '1-1', label: '1 room, 1 guest' },
  { value: '1-2', label: '1 room, 2 guests' },
  { value: '1-3', label: '1 room, 3 guests' },
  { value: '2-4', label: '2 rooms, 4 guests' },
]

const checkIn = shallowRef<DateValue>(today(getLocalTimeZone()))
const checkOut = shallowRef<DateValue>(checkIn.value.add({ days: 1 }))
const occupancy = ref('1-2')

// Check-out must stay after check-in.
watch(checkIn, (value) => {
  if (checkOut.value.compare(value) <= 0) checkOut.value = value.add({ days: 1 })
})
watch(checkOut, (value) => {
  if (value.compare(checkIn.value) <= 0) checkOut.value = checkIn.value.add({ days: 1 })
})

function submit() {
  const [rooms = 1, guests = 1] = occupancy.value.split('-').map(Number)
  emit('search', { checkIn: checkIn.value, checkOut: checkOut.value, rooms, guests })
}
</script>

<template>
  <form
    role="search"
    aria-label="Search rooms"
    class="flex flex-col gap-4 rounded-sm bg-white p-4 shadow-md lg:flex-row lg:items-end lg:gap-10 lg:p-15"
    @submit.prevent="submit"
  >
    <div class="flex flex-col gap-4 lg:flex-2 lg:flex-row lg:items-end lg:gap-6">
      <FormField label="Check In" for="check-in">
        <DatePicker id="check-in" v-model="checkIn" />
      </FormField>
      <span aria-hidden="true" class="hidden pb-3 text-body1 text-gray-900 lg:block">-</span>
      <FormField label="Check Out" for="check-out">
        <DatePicker id="check-out" v-model="checkOut" />
      </FormField>
    </div>

    <FormField label="Rooms & Guests" for="occupancy" class="lg:flex-1">
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

    <Button type="submit" class="mt-2 lg:mt-0 lg:w-36">
      Search
    </Button>
  </form>
</template>
