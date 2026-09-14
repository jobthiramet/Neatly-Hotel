<script setup lang="ts">
import type { HotelInfo } from '@/stores/hotel'
import { reactive } from 'vue'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { ImageUpload } from '@/components/ui/image-upload'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { useHotelStore } from '@/stores/hotel'

const LOGO_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/svg+xml']
const LOGO_MAX_BYTES = 2 * 1024 * 1024

const hotel = useHotelStore()

const form = reactive<HotelInfo>({
  name: hotel.name,
  description: hotel.description,
  logo: hotel.logo,
})
const errors = reactive<Partial<Record<keyof HotelInfo, string>>>({})

function validate() {
  errors.name = form.name.trim() ? undefined : 'Hotel name is required.'
  errors.description = form.description.trim() ? undefined : 'Hotel description is required.'
  if (!form.logo)
    errors.logo = 'Hotel logo is required.'
  else if (form.logo instanceof File && !LOGO_TYPES.includes(form.logo.type))
    errors.logo = 'Logo must be a PNG, JPG, WEBP or SVG image.'
  else if (form.logo instanceof File && form.logo.size > LOGO_MAX_BYTES)
    errors.logo = 'Logo must be 2 MB or smaller.'
  else
    errors.logo = undefined
  return !errors.name && !errors.description && !errors.logo
}

function onSubmit() {
  if (!validate())
    return
  hotel.update({ ...form, name: form.name.trim() })
  toast.success('Hotel information updated')
}
</script>

<template>
  <Teleport defer to="#admin-header-actions">
    <Button type="submit" form="hotel-information-form">
      Update
    </Button>
  </Teleport>

  <form
    id="hotel-information-form"
    novalidate
    class="flex flex-col gap-10 rounded-sm bg-white px-6 pt-10 pb-15 lg:px-20"
    @submit.prevent="onSubmit"
  >
    <FormField label="Hotel name *" for="hotel-name" :error="errors.name">
      <Input
        id="hotel-name"
        v-model="form.name"
        :aria-invalid="!!errors.name"
        aria-describedby="hotel-name-error"
      />
    </FormField>

    <FormField label="Hotel description *" for="hotel-description" :error="errors.description">
      <Textarea
        id="hotel-description"
        v-model="form.description"
        rows="8"
        :aria-invalid="!!errors.description"
        aria-describedby="hotel-description-error"
      />
    </FormField>

    <FormField label="Hotel logo *" for="hotel-logo" :error="errors.logo">
      <ImageUpload
        id="hotel-logo"
        v-model="form.logo"
        :accept="LOGO_TYPES.join(',')"
        :aria-invalid="!!errors.logo"
        aria-describedby="hotel-logo-error"
      />
    </FormField>
  </form>
</template>
