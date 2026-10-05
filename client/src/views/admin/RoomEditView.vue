<script setup lang="ts">
import type { RoomFormValues } from '@/components/admin/RoomForm.vue'
import type { RoomResponse } from '@/stores/rooms'
import { isAxiosError } from 'axios'
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import RoomForm from '@/components/admin/RoomForm.vue'
import { IconArrowRight } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { apiErrorMessage } from '@/stores/hotel'
import { apiFieldErrors, useRoomsStore } from '@/stores/rooms'

const MIN_GALLERY = 4

const rooms = useRoomsStore()
const route = useRoute()
const router = useRouter()

const room = ref<RoomResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const notFound = ref(false)
const saving = ref(false)
const deleting = ref(false)
const deleteOpen = ref(false)
const serverErrors = ref<Record<string, string>>({})

async function load() {
  loading.value = true
  error.value = null
  notFound.value = false
  try {
    room.value = await rooms.get(String(route.params.id))
  }
  catch (e) {
    notFound.value = isAxiosError(e) && (e.response?.status === 404 || e.response?.status === 400)
    error.value = apiErrorMessage(e, 'Could not load room.')
  }
  finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })

/**
 * Saves fields, then syncs images with the image endpoints.
 * Removals and uploads are interleaved so the gallery never drops below 4 or goes above 12 in between.
 */
async function onSubmit(values: RoomFormValues) {
  const id = room.value!.id
  saving.value = true
  serverErrors.value = {}
  try {
    let current = await rooms.update(id, values.room)

    if (values.mainImage instanceof File)
      current = await rooms.uploadImage(id, values.mainImage, true)

    const keptUrls = new Set(values.gallery.filter(item => typeof item === 'string'))
    const toRemove = current.gallery.filter(image => !keptUrls.has(image.url))
    const toAdd = values.gallery.filter(item => item instanceof File)
    const newIds = new Map<File, string>()
    while (toRemove.length || toAdd.length) {
      if (toRemove.length && (current.gallery.length > MIN_GALLERY || !toAdd.length)) {
        current = await rooms.removeImage(id, toRemove.shift()!.id)
      }
      else {
        const file = toAdd.shift()!
        const before = new Set(current.gallery.map(image => image.id))
        current = await rooms.uploadImage(id, file, false)
        newIds.set(file, current.gallery.find(image => !before.has(image.id))!.id)
      }
    }

    const idByUrl = new Map(current.gallery.map(image => [image.url, image.id]))
    const order = values.gallery.map(item => (typeof item === 'string' ? idByUrl.get(item)! : newIds.get(item)!))
    if (order.join() !== current.gallery.map(image => image.id).join())
      current = await rooms.reorderImages(id, order)

    room.value = current
    toast.success('Room updated')
  }
  catch (e) {
    serverErrors.value = isAxiosError(e) && e.response?.status === 409
      ? { name: apiErrorMessage(e, 'Room type already exists.') }
      : apiFieldErrors(e)
    toast.error(apiErrorMessage(e, 'Could not update room.'))
    // Some steps may have succeeded; show what the server has now.
    rooms.get(id).then(latest => (room.value = latest)).catch(() => {})
  }
  finally {
    saving.value = false
  }
}

function focusCancel(event: Event) {
  event.preventDefault()
  document.querySelector<HTMLElement>('[data-delete-cancel]')?.focus()
}

async function onDelete() {
  deleting.value = true
  try {
    await rooms.remove(room.value!.id)
    deleteOpen.value = false
    toast.success('Room deleted')
    await router.push({ name: 'admin-rooms' })
  }
  catch (e) {
    toast.error(apiErrorMessage(e, 'Could not delete room.'))
  }
  finally {
    deleting.value = false
  }
}
</script>

<!-- Figma: (admin) room & property - view/edit -->
<template>
  <Teleport defer to="#admin-header-title">
    <div class="flex min-w-0 items-center gap-4">
      <RouterLink
        :to="{ name: 'admin-rooms' }"
        aria-label="Back to Room & Property"
        class="rounded-sm text-gray-600 outline-none is-hover:text-black is-focus:ring-2 is-focus:ring-orange-500"
      >
        <IconArrowRight class="size-6 rotate-180" />
      </RouterLink>
      <h1 class="truncate text-h5 text-black">
        {{ room?.name ?? (notFound ? 'Room not found' : 'Room') }}
      </h1>
    </div>
  </Teleport>

  <Teleport defer to="#admin-header-actions">
    <Button type="submit" form="room-form" :disabled="!room || saving">
      {{ saving ? 'Updating…' : 'Update' }}
    </Button>
  </Teleport>

  <div v-if="loading && !room" role="status" class="rounded-sm bg-white px-6 py-10 text-body1 text-gray-700 lg:px-20">
    Loading room…
  </div>

  <div v-else-if="error" role="alert" class="flex flex-col items-start gap-4 rounded-sm bg-white px-6 py-10 lg:px-20">
    <p class="text-body1 text-red">
      {{ notFound ? 'This room does not exist or has been deleted.' : error }}
    </p>
    <Button v-if="notFound" variant="secondary" as-child>
      <RouterLink :to="{ name: 'admin-rooms' }">
        Back to Room & Property
      </RouterLink>
    </Button>
    <Button v-else variant="secondary" @click="load">
      Try again
    </Button>
  </div>

  <template v-else-if="room">
    <RoomForm id="room-form" :room="room" :aria-busy="saving" :server-errors="serverErrors" @submit="onSubmit" />

    <div class="mt-6 flex justify-end">
      <Button variant="ghost" class="text-gray-600" @click="deleteOpen = true">
        Delete Room
      </Button>
    </div>

    <Dialog v-model:open="deleteOpen">
      <!-- Focus the safe choice first so Enter never deletes by accident. -->
      <DialogContent @open-auto-focus="focusCancel">
        <DialogHeader>
          <DialogTitle>Delete Room</DialogTitle>
        </DialogHeader>
        <DialogDescription>
          Are you sure you want to delete this room?
        </DialogDescription>
        <DialogFooter>
          <Button variant="secondary" :disabled="deleting" @click="onDelete">
            {{ deleting ? 'Deleting…' : 'Yes, I want to delete' }}
          </Button>
          <DialogClose as-child>
            <Button data-delete-cancel>No, I don’t</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </template>
</template>
