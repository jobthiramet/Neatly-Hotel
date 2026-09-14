<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { ImageUpload } from '@/components/ui/image-upload'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { apiErrorMessage, useHotelStore } from '@/stores/hotel'

// Same rules as PUT /api/hotel/logo (docs/API.md). No SVG: it can carry scripts.
const LOGO_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const LOGO_MAX_BYTES = 2 * 1024 * 1024

interface HotelForm {
  name: string
  description: string
  /** Current logo URL, or a freshly picked file not uploaded yet. */
  logo: string | File | null
}

const hotel = useHotelStore()

const form = reactive<HotelForm>({ name: '', description: '', logo: null })
const errors = reactive<Partial<Record<keyof HotelForm, string>>>({})
const saving = ref(false)

function resetForm() {
  form.name = hotel.name
  form.description = hotel.description
  form.logo = hotel.logoUrl
}

onMounted(async () => {
  await hotel.fetch()
  resetForm()
})

function validate() {
  errors.name = form.name.trim() ? undefined : 'Hotel name is required.'
  errors.description = form.description.trim() ? undefined : 'Hotel description is required.'
  if (!form.logo)
    errors.logo = 'Hotel logo is required.'
  else if (form.logo instanceof File && !LOGO_TYPES.includes(form.logo.type))
    errors.logo = 'Logo must be a PNG, JPG or WEBP image.'
  else if (form.logo instanceof File && form.logo.size > LOGO_MAX_BYTES)
    errors.logo = 'Logo must be 2 MB or smaller.'
  else
    errors.logo = undefined
  return !errors.name && !errors.description && !errors.logo
}

async function onSubmit() {
  if (!validate())
    return
  saving.value = true
  try {
    await hotel.save({ name: form.name.trim(), description: form.description.trim() })
    if (form.logo instanceof File)
      await hotel.uploadLogo(form.logo)
    resetForm()
    toast.success('Hotel information updated')
  }
  catch (e) {
    toast.error(apiErrorMessage(e, 'Could not update hotel information.'))
  }
  finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport defer to="#admin-header-actions">
    <Button type="submit" form="hotel-information-form" :disabled="hotel.loading || !!hotel.error || saving">
      {{ saving ? 'Updating…' : 'Update' }}
    </Button>
  </Teleport>

  <div v-if="hotel.loading" role="status" class="rounded-sm bg-white px-6 py-10 text-body1 text-gray-700 lg:px-20">
    Loading hotel information…
  </div>

  <div v-else-if="hotel.error" role="alert" class="flex flex-col items-start gap-4 rounded-sm bg-white px-6 py-10 lg:px-20">
    <p class="text-body1 text-red">{{ hotel.error }}</p>
    <Button variant="secondary" @click="hotel.fetch().then(resetForm)">
      Try again
    </Button>
  </div>

  <form
    v-else
    id="hotel-information-form"
    novalidate
    :aria-busy="saving"
    class="flex flex-col gap-10 rounded-sm bg-white px-6 pt-10 pb-15 lg:px-20"
    @submit.prevent="onSubmit"
  >
    <FormField label="Hotel name *" for="hotel-name" :error="errors.name">
      <Input
        id="hotel-name"
        v-model="form.name"
        maxlength="120"
        :aria-invalid="!!errors.name"
        aria-describedby="hotel-name-error"
      />
    </FormField>

    <FormField label="Hotel description *" for="hotel-description" :error="errors.description">
      <Textarea
        id="hotel-description"
        v-model="form.description"
        rows="8"
        maxlength="5000"
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
