<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { IconEdit, IconTrash } from '@/components/icons'
import { Badge, roomStatusTone, type RoomStatus } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { apiErrorMessage } from '@/stores/hotel'
import { useRoomUnitsStore, type RoomUnitResponse } from '@/stores/roomUnits'
import { BED_TYPE_LABELS, useRoomsStore } from '@/stores/rooms'

const PAGE_SIZE = 10

/** Figma status badges → `room_statuses.code` (1:1). Occupancy prefix is Vacant* because list API reports occupied=false. */
const FIGMA_STATUS_TO_CODE: Partial<Record<RoomStatus, string>> = {
  'Assign Clean': 'ASSIGN_CLEAN',
  'Assign Dirty': 'ASSIGN_DIRTY',
  'Vacant Clean': 'CLEAN',
  'Vacant Clean Inspected': 'CLEAN_INSPECTED',
  'Vacant Clean Pick Up': 'CLEAN_PICK_UP',
  'Occupied Dirty': 'DIRTY',
  'Out of Order': 'OUT_OF_ORDER',
  'Out of Service': 'OUT_OF_SERVICE',
  'Out of Inventory': 'OUT_OF_INVENTORY',
}

const FIGMA_STATUS_OPTIONS = (Object.keys(roomStatusTone) as RoomStatus[])
  .filter(status => status in FIGMA_STATUS_TO_CODE)

const roomUnitsStore = useRoomUnitsStore()
const roomsStore = useRoomsStore()

const search = ref('')
const page = ref(1)
const formOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const deleteOpen = ref(false)
const deletingRoom = ref<RoomUnitResponse | null>(null)
const saving = ref(false)
const deleting = ref(false)
const statusSearch = ref('')
const roomTypes = ref<{ id: string, name: string, bedType: keyof typeof BED_TYPE_LABELS }[]>([])

const form = reactive({
  roomNumber: '',
  roomTypeId: '' as string,
  statusBadge: '' as RoomStatus | '',
})

const errors = reactive<Partial<Record<'roomNumber' | 'roomTypeId' | 'statusBadge', string>>>({})

const availableStatusCodes = computed(() =>
  new Set(roomUnitsStore.statuses.map(s => s.code.toUpperCase())),
)

const statusOptions = computed(() =>
  FIGMA_STATUS_OPTIONS
    .map(status => ({ status, code: FIGMA_STATUS_TO_CODE[status]! }))
    .filter(option => availableStatusCodes.value.has(option.code.toUpperCase())),
)

const filteredStatusOptions = computed(() => {
  const q = statusSearch.value.trim().toLowerCase()
  const selected = form.statusBadge
  const options = !q
    ? statusOptions.value
    : statusOptions.value.filter(option =>
        option.status.toLowerCase().includes(q)
        || option.status === selected,
      )
  return options
})

const selectedStatusCode = computed(() => {
  const match = statusOptions.value.find(option => option.status === form.statusBadge)
  return match?.code ?? ''
})

function onStatusSelect(value: string | number | bigint | Record<string, unknown> | null) {
  const next = value == null ? '' : String(value)
  form.statusBadge = isRoomStatus(next) ? next : ''
  statusSearch.value = ''
  errors.statusBadge = undefined
}

const selectedBedTypeLabel = computed(() => {
  const type = roomTypes.value.find(t => t.id === form.roomTypeId)
  return type ? BED_TYPE_LABELS[type.bedType] : ''
})

const formTitle = computed(() => formMode.value === 'create' ? 'Create Room' : 'Edit Room')
const submitLabel = computed(() => {
  if (saving.value)
    return formMode.value === 'create' ? 'Creating…' : 'Saving…'
  return formMode.value === 'create' ? 'Create' : 'Save'
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

function defaultStatusBadge(): RoomStatus | '' {
  return statusOptions.value.find(option => option.status === 'Vacant Clean')?.status
    ?? statusOptions.value[0]?.status
    ?? ''
}

function statusBadgeForRoom(room: RoomUnitResponse): RoomStatus | '' {
  if (isRoomStatus(room.displayStatus) && statusOptions.value.some(o => o.status === room.displayStatus))
    return room.displayStatus
  const code = room.statusCode.toUpperCase()
  return statusOptions.value.find(option => option.code.toUpperCase() === code)?.status ?? ''
}

function resetForm() {
  form.roomNumber = ''
  form.roomTypeId = ''
  form.statusBadge = defaultStatusBadge()
  statusSearch.value = ''
  errors.roomNumber = undefined
  errors.roomTypeId = undefined
  errors.statusBadge = undefined
  editingId.value = null
}

function openCreate() {
  formMode.value = 'create'
  resetForm()
  formOpen.value = true
}

function openEdit(room: RoomUnitResponse) {
  formMode.value = 'edit'
  editingId.value = room.id
  form.roomNumber = room.roomNumber
  form.roomTypeId = room.roomTypeId
  form.statusBadge = statusBadgeForRoom(room)
  statusSearch.value = ''
  errors.roomNumber = undefined
  errors.roomTypeId = undefined
  errors.statusBadge = undefined
  formOpen.value = true
}

function openDelete(room: RoomUnitResponse) {
  deletingRoom.value = room
  deleteOpen.value = true
}

function onRoomNumberInput(value: string | number) {
  form.roomNumber = String(value ?? '').replace(/\D/g, '').slice(0, 4)
}

function validate() {
  const number = form.roomNumber.trim()
  errors.roomNumber = /^\d{4}$/.test(number) ? undefined : 'Room number must be exactly 4 digits.'
  errors.roomTypeId = form.roomTypeId ? undefined : 'Room type is required.'
  errors.statusBadge = selectedStatusCode.value ? undefined : 'Status is required.'
  return !errors.roomNumber && !errors.roomTypeId && !errors.statusBadge
}

async function submitForm() {
  if (!validate())
    return
  saving.value = true
  const payload = {
    roomNumber: form.roomNumber.trim(),
    roomTypeId: form.roomTypeId,
    statusCode: selectedStatusCode.value,
  }
  try {
    if (formMode.value === 'create') {
      await roomUnitsStore.create(payload)
      toast.success('Room created')
    }
    else if (editingId.value) {
      await roomUnitsStore.update(editingId.value, payload)
      toast.success('Room updated')
    }
    formOpen.value = false
    resetForm()
  }
  catch (e) {
    toast.error(apiErrorMessage(e, formMode.value === 'create' ? 'Could not create room.' : 'Could not update room.'))
  }
  finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!deletingRoom.value)
    return
  deleting.value = true
  try {
    await roomUnitsStore.remove(deletingRoom.value.id)
    deleteOpen.value = false
    deletingRoom.value = null
    toast.success('Room deleted')
  }
  catch (e) {
    toast.error(apiErrorMessage(e, 'Could not delete room.'))
  }
  finally {
    deleting.value = false
  }
}

function isRoomStatus(value: string): value is RoomStatus {
  return value in roomStatusTone
}
</script>

<template>
  <Teleport defer to="#admin-header-actions">
    <Input
      v-model="search"
      type="search"
      placeholder="Search..."
      aria-label="Search rooms"
      class="w-80"
    />
    <Button type="button" @click="openCreate">
      + Create Room
    </Button>
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

  <template v-else>
    <div class="overflow-x-auto rounded-sm bg-white">
      <table class="w-full min-w-200 table-fixed text-left text-body2 text-black">
        <colgroup>
          <col class="w-28">
          <col class="w-2/5">
          <col>
          <col>
          <col class="w-28">
        </colgroup>
        <thead class="bg-gray-300 text-gray-800">
          <tr>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Room no.
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Room type
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Bed Type
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Status
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Update
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="pagedRooms.length === 0">
            <td colspan="5" class="px-4 py-10 text-center text-body1 text-gray-700">
              No rooms found. Create the first room to get started.
            </td>
          </tr>
          <tr
            v-for="room in pagedRooms"
            :key="room.id"
            class="border-t border-gray-200 is-hover:bg-gray-100"
          >
            <td class="px-4 py-4">
              {{ room.roomNumber }}
            </td>
            <td class="px-4 py-4">
              {{ room.roomTypeName }}
            </td>
            <td class="px-4 py-4">
              {{ BED_TYPE_LABELS[room.bedType] }}
            </td>
            <td class="px-4 py-4">
              <Badge v-if="isRoomStatus(room.displayStatus)" :status="room.displayStatus" />
              <Badge v-else tone="neutral">
                {{ room.displayStatus }}
              </Badge>
            </td>
            <td class="px-4 py-4">
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  class="flex size-8 items-center justify-center text-orange-500 outline-none is-hover:text-orange-600 is-focus:ring-2 is-focus:ring-ring"
                  :aria-label="`Edit room ${room.roomNumber}`"
                  @click="openEdit(room)"
                >
                  <IconEdit class="size-5" />
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center text-red outline-none is-hover:text-red is-focus:ring-2 is-focus:ring-ring"
                  :aria-label="`Delete room ${room.roomNumber}`"
                  @click="openDelete(room)"
                >
                  <IconTrash class="size-5" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav
      v-if="filteredRooms.length > 0"
      class="mt-10 flex items-center justify-center gap-2"
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
  </template>

  <Dialog v-model:open="formOpen">
    <DialogContent class="w-11/12 max-w-md">
      <DialogHeader>
        <DialogTitle>{{ formTitle }}</DialogTitle>
      </DialogHeader>

      <form
        id="room-unit-form"
        class="grid grid-cols-1 gap-4 px-6 py-6 sm:grid-cols-2"
        @submit.prevent="submitForm"
      >
        <FormField label="Room no." for="room-number" :error="errors.roomNumber">
          <Input
            id="room-number"
            :model-value="form.roomNumber"
            type="text"
            inputmode="numeric"
            pattern="[0-9]*"
            maxlength="4"
            autocomplete="off"
            placeholder=""
            :aria-invalid="!!errors.roomNumber"
            required
            @update:model-value="onRoomNumberInput"
          />
        </FormField>

        <FormField label="Status" for="room-status" :error="errors.statusBadge">
          <Select
            :model-value="form.statusBadge || undefined"
            @update:model-value="onStatusSelect"
          >
            <SelectTrigger id="room-status" class="h-auto min-h-11" :aria-invalid="!!errors.statusBadge">
              <span class="flex min-w-0 flex-1 items-center">
                <Badge v-if="isRoomStatus(form.statusBadge)" :status="form.statusBadge" />
                <span v-else class="text-body1 text-gray-600">Select status</span>
              </span>
              <SelectValue class="sr-only" :placeholder="form.statusBadge || 'Select status'" />
            </SelectTrigger>
            <SelectContent class="min-w-64">
              <div class="sticky top-0 z-10 bg-white px-3 pb-2" @keydown.stop @pointerdown.stop>
                <Input
                  v-model="statusSearch"
                  type="search"
                  placeholder="Search status…"
                  class="h-9"
                  aria-label="Search status"
                />
              </div>
              <SelectItem
                v-for="option in filteredStatusOptions"
                :key="option.status"
                :value="option.status"
                class="py-2.5"
              >
                <Badge :status="option.status" />
              </SelectItem>
              <p
                v-if="filteredStatusOptions.length === 0"
                class="px-4 py-3 text-body2 text-gray-600"
              >
                No status found.
              </p>
            </SelectContent>
          </Select>
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
      </form>

      <DialogFooter class="border-t border-gray-300 pt-6">
        <DialogClose as-child>
          <Button type="button" variant="secondary" :disabled="saving">
            Cancel
          </Button>
        </DialogClose>
        <Button type="submit" form="room-unit-form" :disabled="saving">
          {{ submitLabel }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <Dialog v-model:open="deleteOpen">
    <DialogContent class="w-11/12 sm:max-w-md">
      <DialogHeader>
        <DialogTitle>Delete room</DialogTitle>
        <DialogDescription>
          Delete room {{ deletingRoom?.roomNumber }}? This cannot be undone from the list.
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <DialogClose as-child>
          <Button type="button" variant="secondary" :disabled="deleting">
            Cancel
          </Button>
        </DialogClose>
        <Button type="button" variant="secondary" :disabled="deleting" @click="confirmDelete">
          {{ deleting ? 'Deleting…' : 'Delete' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
