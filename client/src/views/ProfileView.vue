<!-- Figma: user > profile (53:1108 desktop, 7415:7630 mobile) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { useUser } from '@clerk/vue'
import { parseDate } from '@internationalized/date'
import { computed, onMounted, ref, shallowRef } from 'vue'
import { toast } from 'vue-sonner'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { FormField } from '@/components/ui/form-field'
import { ImageUpload } from '@/components/ui/image-upload'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { updateProfile } from '@/api/profile'
import { countries } from '@/data/countries'
import { useProfileStore } from '@/stores/profile'
import { apiFieldErrors } from '@/stores/rooms'

const { user } = useUser()
const profileStore = useProfileStore()

const firstName = ref('')
const lastName = ref('')
const phoneNumber = ref('')
const country = ref('')
const dateOfBirth = shallowRef<DateValue | undefined>()

const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const errors = ref<Record<string, string>>({})

// Clerk owns the email address and the avatar; the database owns everything else.
const email = computed(() => user.value?.primaryEmailAddress?.emailAddress ?? '')
const avatarUrl = computed(() => user.value?.hasImage ? user.value.imageUrl : null)

function fill(profile: NonNullable<typeof profileStore.profile>) {
  firstName.value = profile.firstName ?? user.value?.firstName ?? ''
  lastName.value = profile.lastName ?? user.value?.lastName ?? ''
  phoneNumber.value = profile.phoneNumber ?? ''
  country.value = profile.country ?? ''
  dateOfBirth.value = profile.dateOfBirth ? parseDate(profile.dateOfBirth) : undefined
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const profile = await profileStore.load()
    if (!profile)
      throw new Error('profile unavailable')
    fill(profile)
  }
  catch {
    loadError.value = 'We couldn\'t load your profile. Please try again.'
  }
  finally {
    loading.value = false
  }
}
onMounted(load)

function validate() {
  const found: Record<string, string> = {}
  if (!firstName.value.trim())
    found.firstName = 'Please enter your first name.'
  if (!lastName.value.trim())
    found.lastName = 'Please enter your last name.'
  if (!/^\+?[0-9][0-9 -]{7,19}$/.test(phoneNumber.value.trim()))
    found.phoneNumber = 'Please enter a valid phone number.'
  if (!dateOfBirth.value)
    found.dateOfBirth = 'Please select your date of birth.'
  if (!country.value)
    found.country = 'Please select your country.'
  return found
}

async function save() {
  errors.value = validate()
  if (Object.keys(errors.value).length)
    return
  saving.value = true
  try {
    const result = await updateProfile({
      firstName: firstName.value.trim(),
      lastName: lastName.value.trim(),
      phoneNumber: phoneNumber.value.trim(),
      dateOfBirth: dateOfBirth.value!.toString(),
      country: country.value,
    })
    fill(result.profile)
    // Keeps the navbar name in step without another request.
    profileStore.set(result.profile)
    // A false flag means the profile saved but Clerk still shows the old name.
    if (result.clerkSynced === false)
      toast.warning('Profile saved, but your account details could not be updated.')
    else
      toast.success('Profile updated')
  }
  catch (error) {
    const fields = apiFieldErrors(error)
    errors.value = fields
    if (!Object.keys(fields).length)
      toast.error('We couldn\'t save your profile. Please try again.')
  }
  finally {
    saving.value = false
  }
}

// Avatars live in Clerk's storage, not the Supabase bucket.
const AVATAR_MAX_BYTES = 5 * 1024 * 1024
const AVATAR_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const avatarBusy = ref(false)

async function onAvatarChange(value: string | File | null) {
  if (!user.value)
    return
  if (value instanceof File) {
    if (!AVATAR_TYPES.includes(value.type)) {
      toast.error('Profile pictures must be a PNG, JPEG or WEBP image.')
      return
    }
    if (value.size > AVATAR_MAX_BYTES) {
      toast.error('Profile pictures must be 5 MB or smaller.')
      return
    }
  }
  avatarBusy.value = true
  try {
    await user.value.setProfileImage({ file: value instanceof File ? value : null })
    await user.value.reload()
    toast.success(value ? 'Profile picture updated' : 'Profile picture removed')
  }
  catch {
    toast.error('We couldn\'t update your profile picture. Please try again.')
  }
  finally {
    avatarBusy.value = false
  }
}
</script>

<template>
  <SiteNavbar />

  <main class="min-h-screen bg-bg">
    <section class="mx-auto max-w-288 px-4 pt-10 pb-16 lg:pt-20 lg:pb-24">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <h1 class="font-serif text-h3 text-green-800 lg:text-h2">
          Profile
        </h1>
        <Button
          class="order-last w-full lg:order-none lg:w-auto"
          :disabled="loading || saving"
          @click="save"
        >
          {{ saving ? 'Updating…' : 'Update Profile' }}
        </Button>
      </div>

      <p v-if="loadError" class="mt-10 text-body1 text-red">
        {{ loadError }}
        <Button variant="ghost" @click="load">
          Try again
        </Button>
      </p>

      <div v-else-if="loading" aria-busy="true" class="mt-10 flex animate-pulse flex-col gap-6">
        <span class="sr-only">Loading your profile…</span>
        <div v-for="row in 3" :key="row" class="grid gap-6 lg:grid-cols-2">
          <div class="h-18 rounded-sm bg-gray-300" />
          <div class="h-18 rounded-sm bg-gray-300" />
        </div>
      </div>

      <form v-else class="mt-10 flex flex-col gap-10" novalidate @submit.prevent="save">
        <section aria-labelledby="basic-information">
          <h2 id="basic-information" class="text-h5 text-gray-600">
            Basic Information
          </h2>

          <div class="mt-6 grid gap-6 lg:grid-cols-2 lg:gap-x-10">
            <FormField label="First name" for="first-name" :error="errors.firstName" class="order-1 lg:order-none">
              <Input id="first-name" v-model="firstName" :aria-invalid="!!errors.firstName" autocomplete="given-name" />
            </FormField>
            <FormField label="Last name" for="last-name" :error="errors.lastName" class="order-2 lg:order-none">
              <Input id="last-name" v-model="lastName" :aria-invalid="!!errors.lastName" autocomplete="family-name" />
            </FormField>

            <FormField label="Email" for="email" class="order-4 lg:order-none">
              <Input id="email" :model-value="email" readonly disabled autocomplete="email" />
            </FormField>
            <FormField label="Phone number" for="phone-number" :error="errors.phoneNumber" class="order-3 lg:order-none">
              <Input id="phone-number" v-model="phoneNumber" :aria-invalid="!!errors.phoneNumber" autocomplete="tel" inputmode="tel" />
            </FormField>

            <FormField label="Date of Birth" for="date-of-birth" :error="errors.dateOfBirth" class="order-5 lg:order-none">
              <DatePicker
                id="date-of-birth"
                v-model="dateOfBirth"
                :invalid="!!errors.dateOfBirth"
                :year-range="[1900, new Date().getFullYear()]"
              />
            </FormField>
            <FormField label="Country" for="country" :error="errors.country" class="order-6 lg:order-none">
              <Select v-model="country">
                <SelectTrigger id="country" :aria-invalid="!!errors.country">
                  <SelectValue placeholder="Select your country" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="item in countries" :key="item" :value="item">
                    {{ item }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </FormField>
          </div>
        </section>

        <hr class="border-gray-300">

        <section aria-labelledby="profile-picture">
          <h2 id="profile-picture" class="text-h5 text-gray-600">
            Profile Picture
          </h2>
          <ImageUpload
            id="profile-picture-upload"
            class="mt-6"
            accept="image/png,image/jpeg,image/webp"
            :model-value="avatarUrl"
            :aria-busy="avatarBusy"
            @update:model-value="onAvatarChange"
          />
        </section>
      </form>
    </section>
  </main>

  <SiteFooter />
</template>
