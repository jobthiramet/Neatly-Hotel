<script setup lang="ts">
import type { BedType, RoomRequest, RoomResponse } from '@/stores/rooms'
import { reactive, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Checkbox, CheckboxLabel } from '@/components/ui/checkbox'
import { FormField } from '@/components/ui/form-field'
import { ImageGalleryUpload } from '@/components/ui/image-gallery-upload'
import { ImageUpload } from '@/components/ui/image-upload'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { BED_TYPE_LABELS } from '@/stores/rooms'

export interface RoomFormValues {
  room: RoomRequest
  /** Current URL, or a new file to upload. */
  mainImage: string | File
  gallery: (string | File)[]
}

const props = defineProps<{
  /** Form element id, so header buttons can submit it with `form="…"`. */
  id: string
  /** Room to edit; omit to create. */
  room?: RoomResponse | null
  /** Field errors from the server, keyed like `apiFieldErrors()`. */
  serverErrors?: Record<string, string>
}>()

const emit = defineEmits<{
  (e: 'submit', values: RoomFormValues): void
}>()

// Same rules as the Rooms section of docs/API.md. No SVG: it can carry scripts.
const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const IMAGE_MAX_BYTES = 5 * 1024 * 1024
const MIN_GALLERY = 4
const MAX_GALLERY = 12
const GUEST_OPTIONS = [2, 3, 4, 5, 6]

interface FormState {
  name: string
  bedType: BedType
  sizeSqm: string | number
  capacity: number
  pricePerNight: string | number
  hasPromotion: boolean
  promotionPrice: string | number
  description: string
  mainImage: string | File | null
  gallery: (string | File)[]
  amenities: { key: number, value: string }[]
}

type ErrorKey = Exclude<keyof FormState, 'hasPromotion'>

let amenityKey = 0
const amenity = (value = '') => ({ key: amenityKey++, value })

const form = reactive<FormState>(initialState())
const errors = reactive<Partial<Record<ErrorKey, string>>>({})
const dragIndex = ref<number | null>(null)

function initialState(): FormState {
  const room = props.room
  return {
    name: room?.name ?? '',
    bedType: room?.bedType ?? 'DOUBLE',
    sizeSqm: room?.sizeSqm ?? '',
    capacity: room?.capacity ?? 2,
    pricePerNight: room?.pricePerNight ?? '',
    hasPromotion: room?.promotionPrice != null,
    promotionPrice: room?.promotionPrice ?? '',
    description: room?.description ?? '',
    mainImage: room?.mainImage?.url ?? null,
    gallery: room?.gallery.map(image => image.url) ?? [],
    amenities: room?.amenities.length ? room.amenities.map(value => amenity(value)) : [amenity()],
  }
}

// Reset when the parent loads or saves a room.
watch(() => props.room, () => {
  Object.assign(form, initialState())
  Object.keys(errors).forEach(key => delete errors[key as ErrorKey])
})

watch(() => props.serverErrors, (serverErrors) => {
  Object.assign(errors, serverErrors)
})

function imageError(file: string | File | null, label: string) {
  if (!(file instanceof File))
    return undefined
  if (!IMAGE_TYPES.includes(file.type))
    return `${label} must be a PNG, JPG or WEBP image.`
  if (file.size > IMAGE_MAX_BYTES)
    return `${label} must be 5 MB or smaller.`
  return undefined
}

function validate() {
  const size = Number(form.sizeSqm)
  const price = Number(form.pricePerNight)
  const promotion = Number(form.promotionPrice)
  errors.name = form.name.trim() ? undefined : 'Room type is required.'
  errors.bedType = form.bedType ? undefined : 'Bed type is required.'
  errors.sizeSqm = form.sizeSqm !== '' && Number.isInteger(size) && size >= 1 && size <= 10000
    ? undefined
    : 'Room size must be a whole number from 1 to 10,000.'
  errors.capacity = form.capacity >= 2 && form.capacity <= 6 ? undefined : 'Guests must be from 2 to 6.'
  errors.pricePerNight = form.pricePerNight !== '' && price > 0 ? undefined : 'Price per night must be more than 0.'
  if (!form.hasPromotion)
    errors.promotionPrice = undefined
  else if (form.promotionPrice === '' || promotion <= 0)
    errors.promotionPrice = 'Promotion price must be more than 0.'
  else if (price > 0 && promotion >= price)
    errors.promotionPrice = 'Promotion price must be lower than the price per night.'
  else
    errors.promotionPrice = undefined
  errors.description = form.description.trim() ? undefined : 'Room description is required.'
  errors.mainImage = form.mainImage ? imageError(form.mainImage, 'Main image') : 'Main image is required.'
  errors.gallery = form.gallery.length < MIN_GALLERY
    ? `Add at least ${MIN_GALLERY} pictures.`
    : form.gallery.map(file => imageError(file, 'Each picture')).find(Boolean)
  errors.amenities = form.amenities[0]?.value.trim() ? undefined : 'Amenity is required.'
  return Object.values(errors).every(error => !error)
}

function onSubmit() {
  if (!validate())
    return
  emit('submit', {
    room: {
      name: form.name.trim(),
      bedType: form.bedType,
      sizeSqm: Number(form.sizeSqm),
      capacity: form.capacity,
      pricePerNight: Number(form.pricePerNight),
      promotionPrice: form.hasPromotion ? Number(form.promotionPrice) : null,
      description: form.description.trim(),
      // Only the first amenity is required; blank extra rows are dropped.
      amenities: form.amenities.map(item => item.value.trim()).filter(Boolean),
    },
    mainImage: form.mainImage!,
    gallery: form.gallery,
  })
}

function moveAmenity(from: number, to: number) {
  if (to < 0 || to >= form.amenities.length || from === to)
    return
  form.amenities.splice(to, 0, ...form.amenities.splice(from, 1))
}

function onAmenityKeydown(event: KeyboardEvent, index: number) {
  if (!event.altKey || (event.key !== 'ArrowUp' && event.key !== 'ArrowDown'))
    return
  event.preventDefault()
  moveAmenity(index, index + (event.key === 'ArrowUp' ? -1 : 1))
}
</script>

<!-- Figma: (admin) room & property - create / view/edit -->
<template>
  <form
    :id="id"
    novalidate
    class="flex flex-col gap-10 rounded-sm bg-white px-6 pt-10 pb-15 lg:px-20"
    @submit.prevent="onSubmit"
  >
    <section class="flex flex-col gap-10" aria-labelledby="room-basic-heading">
      <h2 id="room-basic-heading" class="text-h5 text-gray-600">
        Basic Information
      </h2>

      <FormField label="Room Type *" for="room-name" :error="errors.name">
        <Input id="room-name" v-model="form.name" maxlength="120" :aria-invalid="!!errors.name" aria-describedby="room-name-error" />
      </FormField>

      <div class="grid gap-10 md:grid-cols-2">
        <FormField label="Room size(sqm) *" for="room-size" :error="errors.sizeSqm">
          <Input id="room-size" v-model="form.sizeSqm" type="number" min="1" max="10000" step="1" :aria-invalid="!!errors.sizeSqm" aria-describedby="room-size-error" />
        </FormField>

        <FormField label="Bed type *" for="room-bed-type" :error="errors.bedType">
          <Select v-model="form.bedType">
            <SelectTrigger id="room-bed-type" :aria-invalid="!!errors.bedType" aria-describedby="room-bed-type-error">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="(label, value) in BED_TYPE_LABELS" :key="value" :value="value">
                {{ label }}
              </SelectItem>
            </SelectContent>
          </Select>
        </FormField>

        <FormField label="Guest(s) *" for="room-guests" :error="errors.capacity">
          <Select v-model="form.capacity">
            <SelectTrigger id="room-guests" :aria-invalid="!!errors.capacity" aria-describedby="room-guests-error">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="guests in GUEST_OPTIONS" :key="guests" :value="guests">
                {{ guests }}
              </SelectItem>
            </SelectContent>
          </Select>
        </FormField>
      </div>

      <div class="grid items-end gap-10 md:grid-cols-2">
        <FormField label="Price per Night(THB) *" for="room-price" :error="errors.pricePerNight">
          <Input id="room-price" v-model="form.pricePerNight" type="number" min="0.01" step="0.01" :aria-invalid="!!errors.pricePerNight" aria-describedby="room-price-error" />
        </FormField>

        <div class="flex flex-col gap-1">
          <div class="flex items-center gap-4">
            <div class="group/checkbox flex shrink-0 items-center gap-3">
              <Checkbox id="room-has-promotion" v-model="form.hasPromotion" />
              <CheckboxLabel for="room-has-promotion">
                Promotion Price
              </CheckboxLabel>
            </div>
            <Input
              id="room-promotion-price"
              v-model="form.promotionPrice"
              type="number"
              min="0.01"
              step="0.01"
              aria-label="Promotion price"
              :disabled="!form.hasPromotion"
              :aria-invalid="!!errors.promotionPrice"
              aria-describedby="room-promotion-price-error"
            />
          </div>
          <p v-if="errors.promotionPrice" id="room-promotion-price-error" class="text-body2 text-red">
            {{ errors.promotionPrice }}
          </p>
        </div>
      </div>

      <FormField label="Room Description *" for="room-description" :error="errors.description">
        <Textarea id="room-description" v-model="form.description" rows="3" maxlength="5000" :aria-invalid="!!errors.description" aria-describedby="room-description-error" />
      </FormField>
    </section>

    <section class="flex flex-col gap-10 border-t border-gray-300 pt-10" aria-labelledby="room-image-heading">
      <h2 id="room-image-heading" class="text-h5 text-gray-600">
        Room Image
      </h2>

      <FormField label="Main Image *" for="room-main-image" :error="errors.mainImage">
        <ImageUpload
          id="room-main-image"
          v-model="form.mainImage"
          :accept="IMAGE_TYPES.join(',')"
          :aria-invalid="!!errors.mainImage"
          aria-describedby="room-main-image-error"
        />
      </FormField>

      <FormField label="Image Gallery(At least 4 pictures) *" for="room-gallery" :error="errors.gallery">
        <ImageGalleryUpload
          id="room-gallery"
          v-model="form.gallery"
          :max="MAX_GALLERY"
          :accept="IMAGE_TYPES.join(',')"
          :aria-invalid="!!errors.gallery"
          aria-describedby="room-gallery-error"
        />
      </FormField>
    </section>

    <section class="flex flex-col gap-6 border-t border-gray-300 pt-10" aria-labelledby="room-amenities-heading">
      <h2 id="room-amenities-heading" class="text-h5 text-gray-600">
        Room Amenities
      </h2>

      <ul class="flex flex-col gap-6">
        <li
          v-for="(item, index) in form.amenities"
          :key="item.key"
          class="flex items-end gap-6"
          :class="dragIndex === index && 'opacity-50'"
          @dragover.prevent
          @drop.prevent="dragIndex !== null && moveAmenity(dragIndex, index); dragIndex = null"
        >
          <button
            type="button"
            draggable="true"
            :aria-label="`Reorder amenity ${index + 1}. Drag or press Alt and arrow keys.`"
            class="mb-3 cursor-grab rounded-sm px-1 text-body1 text-gray-500 outline-none is-focus:ring-2 is-focus:ring-orange-500"
            @dragstart="dragIndex = index"
            @dragend="dragIndex = null"
            @keydown="onAmenityKeydown($event, index)"
          >
            <span aria-hidden="true">⠿</span>
          </button>
          <FormField
            :label="index === 0 ? 'Amenity *' : 'Amenity'"
            :for="`room-amenity-${item.key}`"
            :error="index === 0 ? errors.amenities : undefined"
          >
            <Input
              :id="`room-amenity-${item.key}`"
              v-model="item.value"
              maxlength="120"
              :aria-invalid="index === 0 && !!errors.amenities"
              :aria-describedby="`room-amenity-${item.key}-error`"
            />
          </FormField>
          <Button
            type="button"
            variant="ghost"
            class="mb-2"
            :disabled="form.amenities.length === 1"
            @click="form.amenities.splice(index, 1)"
          >
            Delete
          </Button>
        </li>
      </ul>

      <Button type="button" variant="secondary" class="self-start" :disabled="form.amenities.length >= 50" @click="form.amenities.push(amenity())">
        + Add Amenity
      </Button>
    </section>
  </form>
</template>
