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
import { useRoomUnitsStore } from '@/stores/roomUnits'
import { BED_TYPE_LABELS, useRoomsStore } from '@/stores/rooms'

const PAGE_SIZE = 10

const roomUnitsStore = useRoomUnitsStore()
const roomsStore = useRoomsStore()

const search = ref('')
const page = ref(1)
const createOpen = ref(false)
const saving = ref(false)
const roomTypes = ref<{ id: string, name: string, bedType: keyof typeof BED_TYPE_LABELS }[]>([])

const form = reactive({
  roomNumber: '',
  roomTypeId: '' as string,
  statusCode: '' as string,
})

const errors = reactive<Partial<Record<keyof typeof form, string>>>({})

const selectedBedTypeLabel = computed(() => {
  const type = roomTypes.value.find(t => t.id === form.roomTypeId)
  return type ? BED_TYPE_LABELS[type.bedType] : ''
})

const filteredRooms = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q)
    return roomUnitsStore.sortedUnits
  return roomUnitsStore.sortedUnits.filter(room =>
    room.roomNumber.toLowerCase().includes(q)
    || room.roomTypeName.toLowerCase().includes(q)
    || BED_TYPE_LABELS[room.bedType].toLowerCase().includes(q)
    || room.displayStatus.toLowerCase().includes(q)
    || room.statusLabel.toLowerCase().includes(q),
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
    const [typesPage] = await Promise.all([
      roomsStore.list({ search: '', page: 0, size: 50 }),
      roomUnitsStore.fetchStatuses(),
      roomUnitsStore.fetchAll(),
    ])
    roomTypes.value = typesPage.content.map(t => ({
      id: t.id,
      name: t.name,
      bedType: t.bedType,
    }))
  }
  catch (e) {
    toast.error(apiErrorMessage(e, roomUnitsStore.error ?? 'Could not load rooms.'))
  }
})

function resetForm() {
  form.roomNumber = ''
  form.roomTypeId = ''
  form.statusCode = roomUnitsStore.statuses.find(s => s.code === 'CLEAN')?.code ?? ''
  errors.roomNumber = undefined
  errors.roomTypeId = undefined
  errors.statusCode = undefined
}

function openCreate() {
  resetForm()
  createOpen.value = true
}

function onRoomNumberInput(event: Event) {
  const input = event.target as HTMLInputElement
  form.roomNumber = input.value.replace(/\D/g, '').slice(0, 4)
}

function validate() {
  const number = form.roomNumber.trim()
  errors.roomNumber = /^\d{4}$/.test(number) ? undefined : 'Room number must be exactly 4 digits.'
  errors.roomTypeId = form.roomTypeId ? undefined : 'Room type is required.'
  errors.statusCode = form.statusCode ? undefined : 'Status is required.'
  return !errors.roomNumber && !errors.roomTypeId && !errors.statusCode
}

async function submitCreate() {
  if (!validate())
    return
  saving.value = true
  try {
    await roomUnitsStore.create({
      roomNumber: form.roomNumber.trim(),
      roomTypeId: form.roomTypeId,
      statusCode: form.statusCode,
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
    v-if="roomUnitsStore.loading"
    role="status"
    class="rounded-sm bg-white px-6 py-10 text-body1 text-gray-700"
  >
    Loading rooms…
  </div>

  <div
    v-else-if="roomUnitsStore.error"
    role="alert"
    class="rounded-sm bg-white px-6 py-10 text-body1 text-red"
  >
    {{ roomUnitsStore.error }}
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
              {{ room.roomTypeName }}
            </td>
            <td class="px-6 py-4">
              {{ BED_TYPE_LABELS[room.bedType] }}
            </td>
            <td class="px-6 py-4">
              <Badge v-if="isRoomStatus(room.displayStatus)" :status="room.displayStatus" />
              <Badge v-else tone="neutral">
                {{ room.displayStatus }}
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
            :model-value="form.roomNumber"
            inputmode="numeric"
            maxlength="4"
            placeholder="0001"
            :aria-invalid="!!errors.roomNumber"
            required
            @input="onRoomNumberInput"
          />
        </FormField>

        <FormField label="Room type" for="room-type" :error="errors.roomTypeId">
          <Select v-model="form.roomTypeId">
            <SelectTrigger id="room-type" :aria-invalid="!!errors.roomTypeId">
              <SelectValue placeholder="Select room type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in roomTypes" :key="option.id" :value="option.id">
                {{ option.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </FormField>

        <FormField label="Bed type" for="bed-type">
          <Input
            id="bed-type"
            :model-value="selectedBedTypeLabel || 'Select a room type first'"
            readonly
            disabled
          />
        </FormField>

        <FormField label="Status" for="room-status" :error="errors.statusCode">
          <Select v-model="form.statusCode">
            <SelectTrigger id="room-status" :aria-invalid="!!errors.statusCode">
              <SelectValue placeholder="Select status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem
                v-for="option in roomUnitsStore.statuses"
                :key="option.code"
                :value="option.code"
              >
                {{ option.label }}
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
