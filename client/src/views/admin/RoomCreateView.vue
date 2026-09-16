<script setup lang="ts">
import type { RoomFormValues } from '@/components/admin/RoomForm.vue'
import { isAxiosError } from 'axios'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import RoomForm from '@/components/admin/RoomForm.vue'
import { Button } from '@/components/ui/button'
import { apiErrorMessage } from '@/stores/hotel'
import { apiFieldErrors, useRoomsStore } from '@/stores/rooms'

const rooms = useRoomsStore()
const router = useRouter()

const saving = ref(false)
const serverErrors = ref<Record<string, string>>({})

async function onSubmit({ room, mainImage, gallery }: RoomFormValues) {
  saving.value = true
  try {
    // The form only allows files here: a new room has no uploaded images yet.
    await rooms.create(room, mainImage as File, gallery as File[])
    toast.success('Room created')
    await router.push({ name: 'admin-rooms' })
  }
  catch (e) {
    serverErrors.value = isAxiosError(e) && e.response?.status === 409
      ? { name: apiErrorMessage(e, 'Room type already exists.') }
      : apiFieldErrors(e)
    toast.error(apiErrorMessage(e, 'Could not create room.'))
  }
  finally {
    saving.value = false
  }
}
</script>

<!-- Figma: (admin) room & property - create -->
<template>
  <Teleport defer to="#admin-header-actions">
    <Button variant="secondary" as-child>
      <RouterLink :to="{ name: 'admin-rooms' }">
        Cancel
      </RouterLink>
    </Button>
    <Button type="submit" form="room-form" :disabled="saving">
      {{ saving ? 'Creating…' : 'Create' }}
    </Button>
  </Teleport>

  <RoomForm id="room-form" :aria-busy="saving" :server-errors="serverErrors" @submit="onSubmit" />
</template>
