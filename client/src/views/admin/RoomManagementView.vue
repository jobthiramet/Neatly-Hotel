<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { Badge, roomStatusTone, type RoomStatus } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { apiErrorMessage } from '@/stores/hotel'
import { useRoomsStore } from '@/stores/rooms'

const PAGE_SIZE = 10

const ROOM_TYPE_OPTIONS = [
  'Superior Garden View',
  'Deluxe',
  'Superior',
  'Premier Sea View',
  'Supreme',
  'Suit',
] as const

const BED_TYPE_OPTIONS = ['Single Bed', 'Double Bed', 'King Bed'] as const

const STATUS_OPTIONS = Object.keys(roomStatusTone) as RoomStatus[]

const roomsStore = useRoomsStore()

const search = ref('')
const page = ref(1)
const createOpen = ref(false)
const saving = ref(false)

const form = reactive({
  roomNumber: '',
  roomType: '' as string,
  bedType: '' as string,
  status: 'Vacant Clean' as string,
})

const errors = reactive<Partial<Record<keyof typeof form, string>>>({})

const filteredRooms = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q)
    return roomsStore.sortedRooms
  return roomsStore.sortedRooms.filter(room =>
    room.roomNumber.toLowerCase().includes(q)
    || room.roomType.toLowerCase().includes(q)
    || room.bedType.toLowerCase().includes(q)
    || room.status.toLowerCase().includes(q),
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRooms.value.length / PAGE_SIZE)))

const pagedRooms = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredRooms.value.slice(start, start + PAGE_SIZE)
})

const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = page.value
  const windowSize = 5
  let start = Math.max(1, current - Math.floor(windowSize / 2))
  const end = Math.min(total, start + windowSize - 1)
  start = Math.max(1, end - windowSize + 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

watch(search, () => {
  page.value = 1
})

watch(filteredRooms, () => {
  if (page.value > totalPages.value)
    page.value = totalPages.value
})

onMounted(async () => {
  try {
    await roomsStore.fetchAll()
  }
  catch {
    toast.error(roomsStore.error ?? 'Could not load rooms.')
  }
})

function resetForm() {
  form.roomNumber = ''
  form.roomType = ''
  form.bedType = ''
  form.status = 'Vacant Clean'
  errors.roomNumber = undefined
  errors.roomType = undefined
  errors.bedType = undefined
  errors.status = undefined
}

function openCreate() {
  resetForm()
  createOpen.value = true
}

function validate() {
  errors.roomNumber = form.roomNumber.trim() ? undefined : 'Room number is required.'
  errors.roomType = form.roomType.trim() ? undefined : 'Room type is required.'
  errors.bedType = form.bedType.trim() ? undefined : 'Bed type is required.'
  errors.status = form.status.trim() ? undefined : 'Status is required.'
  return !errors.roomNumber && !errors.roomType && !errors.bedType && !errors.status
}

async function submitCreate() {
  if (!validate())
    return
  saving.value = true
  try {
    await roomsStore.create({
      roomNumber: form.roomNumber.trim(),
      roomType: form.roomType.trim(),
      bedType: form.bedType.trim(),
      status: form.status.trim(),
    })
    createOpen.value = false
    resetForm()
    toast.success('Room created')
  }
  catch (e) {
    toast.error(apiErrorMessage(e, 'Could not create room.'))
  }
  finally {
    saving.value = false
  }
}

function isRoomStatus(value: string): value is RoomStatus {
  return value in roomStatusTone
}
</script>

<template>
  <Teleport defer to="#admin-header-actions">
    <div class="flex flex-wrap items-center justify-end gap-4">
      <label class="relative block w-56 max-w-full">
        <span class="sr-only">Search rooms</span>
        <svg
          class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-gray-500"
          viewBox="0 0 20 20"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="M9 15.5a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13Zm7 1.5-3.5-3.5"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
        <Input
          v-model="search"
          type="search"
          placeholder="Search…"
          class="h-10 pl-9"
        />
      </label>
      <div
        class="flex size-10 items-center justify-center rounded-full bg-orange-400 text-body2 font-medium text-white"
        aria-hidden="true"
      >
        W
      </div>
      <Button type="button" @click="openCreate">
        + Create Room
      </Button>
    </div>
  </Teleport>

  <div
    v-if="roomsStore.loading"
    role="status"
    class="rounded-sm bg-white px-6 py-10 text-body1 text-gray-700"
  >
    Loading rooms…
  </div>

  <div
    v-else-if="roomsStore.error"
    role="alert"
    class="rounded-sm bg-white px-6 py-10 text-body1 text-red"
  >
    {{ roomsStore.error }}
  </div>

  <div v-else class="overflow-hidden rounded-sm bg-white shadow-md">
    <div class="overflow-x-auto">
      <table class="w-full min-w-3xl border-collapse text-left">
        <thead>
          <tr class="border-b border-gray-300 bg-gray-100 text-body2 text-gray-800">
            <th class="px-6 py-4 font-medium">
              Room no.
            </th>
            <th class="px-6 py-4 font-medium">
              Room type
            </th>
            <th class="px-6 py-4 font-medium">
              Bed Type
            </th>
            <th class="px-6 py-4 font-medium">
              Status
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="pagedRooms.length === 0">
            <td colspan="4" class="px-6 py-10 text-body1 text-gray-600">
              No rooms found. Create the first room to get started.
            </td>
          </tr>
          <tr
            v-for="room in pagedRooms"
            :key="room.id"
            class="border-b border-gray-200 text-body1 text-gray-800 last:border-b-0"
          >
            <td class="px-6 py-4">
              {{ room.roomNumber }}
            </td>
            <td class="px-6 py-4">
              {{ room.roomType }}
            </td>
            <td class="px-6 py-4">
              {{ room.bedType }}
            </td>
            <td class="px-6 py-4">
              <Badge v-if="isRoomStatus(room.status)" :status="room.status" />
              <Badge v-else tone="neutral">
                {{ room.status }}
              </Badge>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav
      v-if="filteredRooms.length > 0"
      class="flex items-center justify-center gap-2 border-t border-gray-200 px-4 py-6"
      aria-label="Pagination"
    >
      <Button
        type="button"
        variant="ghost"
        class="size-8 p-0"
        :disabled="page <= 1"
        aria-label="Previous page"
        @click="page -= 1"
      >
        ‹
      </Button>
      <Button
        v-for="n in pageNumbers"
        :key="n"
        type="button"
        :variant="n === page ? 'primary' : 'ghost'"
        class="size-8 p-0"
        :aria-current="n === page ? 'page' : undefined"
        @click="page = n"
      >
        {{ n }}
      </Button>
      <Button
        type="button"
        variant="ghost"
        class="size-8 p-0"
        :disabled="page >= totalPages"
        aria-label="Next page"
        @click="page += 1"
      >
        ›
      </Button>
    </nav>
  </div>

  <Dialog v-model:open="createOpen">
    <DialogContent class="w-11/12 sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>Create Room</DialogTitle>
      </DialogHeader>

      <form id="create-room-form" class="flex flex-col gap-4" @submit.prevent="submitCreate">
        <FormField label="Room no." for="room-number" :error="errors.roomNumber">
          <Input
            id="room-number"
            v-model="form.roomNumber"
            placeholder="0001"
            :aria-invalid="!!errors.roomNumber"
            required
          />
        </FormField>

        <FormField label="Room type" for="room-type" :error="errors.roomType">
          <Select v-model="form.roomType">
            <SelectTrigger id="room-type" :aria-invalid="!!errors.roomType">
              <SelectValue placeholder="Select room type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in ROOM_TYPE_OPTIONS" :key="option" :value="option">
                {{ option }}
              </SelectItem>
            </SelectContent>
          </Select>
        </FormField>

        <FormField label="Bed type" for="bed-type" :error="errors.bedType">
          <Select v-model="form.bedType">
            <SelectTrigger id="bed-type" :aria-invalid="!!errors.bedType">
              <SelectValue placeholder="Select bed type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in BED_TYPE_OPTIONS" :key="option" :value="option">
                {{ option }}
              </SelectItem>
            </SelectContent>
          </Select>
        </FormField>

        <FormField label="Status" for="room-status" :error="errors.status">
          <Select v-model="form.status">
            <SelectTrigger id="room-status" :aria-invalid="!!errors.status">
              <SelectValue placeholder="Select status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in STATUS_OPTIONS" :key="option" :value="option">
                {{ option }}
              </SelectItem>
            </SelectContent>
          </Select>
        </FormField>
      </form>

      <DialogFooter>
        <DialogClose as-child>
          <Button type="button" variant="secondary" :disabled="saving">
            Cancel
          </Button>
        </DialogClose>
        <Button type="submit" form="create-room-form" :disabled="saving">
          {{ saving ? 'Creating…' : 'Create' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
