<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { useClerk, useSignUp } from '@clerk/vue'
import { isClerkAPIResponseError } from '@clerk/vue/errors'
import { ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import registerBackground from '@/assets/auth/register-background.jpg'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

type VerificationKind = 'email_address' | 'phone_number'

const countries = ['Thailand', 'Singapore', 'Japan', 'United Kingdom', 'United States']
const router = useRouter()
const clerk = useClerk()
const { isLoaded, signUp, setActive } = useSignUp()

const firstName = ref('')
const lastName = ref('')
const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const phoneNumber = ref('')
const dateOfBirth = shallowRef<DateValue>()
const country = ref('')
const profilePicture = shallowRef<File>()
const profilePreview = ref('')
const verificationCode = ref('')
const verificationKind = ref<VerificationKind>()
const error = ref('')
const loading = ref(false)

function selectProfilePicture(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  profilePicture.value = file
  if (profilePreview.value) URL.revokeObjectURL(profilePreview.value)
  profilePreview.value = URL.createObjectURL(file)
}

function showError(caught: unknown) {
  error.value = isClerkAPIResponseError(caught)
    ? caught.errors[0]?.longMessage ?? caught.errors[0]?.message ?? 'Unable to create your account.'
    : 'Unable to create your account. Please try again.'
}

async function prepareNextVerification(unverifiedFields: string[]) {
  if (!signUp.value) return
  if (unverifiedFields.includes('email_address')) {
    await signUp.value.prepareEmailAddressVerification({ strategy: 'email_code' })
    verificationKind.value = 'email_address'
  } else if (unverifiedFields.includes('phone_number')) {
    await signUp.value.preparePhoneNumberVerification({ strategy: 'phone_code' })
    verificationKind.value = 'phone_number'
  }
}

async function finishRegistration(sessionId: string | null) {
  if (!sessionId || !setActive.value) return
  await setActive.value({ session: sessionId })
  if (profilePicture.value && clerk.value?.user) {
    await clerk.value.user.setProfileImage({ file: profilePicture.value })
  }
  await router.push('/')
}

async function register() {
  error.value = ''
  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }
  if (!dateOfBirth.value || !country.value) {
    error.value = 'Please select your date of birth and country.'
    return
  }
  if (!isLoaded.value || !signUp.value) return

  loading.value = true
  try {
    const result = await signUp.value.create({
      firstName: firstName.value,
      lastName: lastName.value,
      username: username.value,
      emailAddress: email.value,
      password: password.value,
      phoneNumber: phoneNumber.value,
      unsafeMetadata: { dateOfBirth: dateOfBirth.value.toString(), country: country.value },
    })
    if (result.status === 'complete') await finishRegistration(result.createdSessionId)
    else await prepareNextVerification(result.unverifiedFields)
  } catch (caught) {
    showError(caught)
  } finally {
    loading.value = false
  }
}

async function verify() {
  if (!signUp.value || !verificationKind.value) return
  error.value = ''
  loading.value = true
  try {
    const result = verificationKind.value === 'email_address'
      ? await signUp.value.attemptEmailAddressVerification({ code: verificationCode.value })
      : await signUp.value.attemptPhoneNumberVerification({ code: verificationCode.value })
    if (result.status === 'complete') await finishRegistration(result.createdSessionId)
    else {
      verificationCode.value = ''
      await prepareNextVerification(result.unverifiedFields)
    }
  } catch (caught) {
    showError(caught)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />
    <main
      class="flex-1 bg-cover bg-center px-4 py-10 lg:px-8 lg:py-15"
      :style="{ backgroundImage: `url(${registerBackground})` }"
    >
      <section class="mx-auto max-w-273 rounded-sm bg-bg px-6 py-12 lg:px-20 lg:py-22.5">
        <template v-if="verificationKind">
          <h1 class="font-serif text-h3 text-green-800">Verify your account</h1>
          <p class="mt-8 text-body1 text-gray-700">
            Enter the code sent to your {{ verificationKind === 'email_address' ? 'email' : 'phone number' }}.
          </p>
          <form class="mt-8 max-w-111.5" @submit.prevent="verify">
            <FormField label="Verification code" for="verification-code" :error="error">
              <Input id="verification-code" v-model="verificationCode" inputmode="numeric" autocomplete="one-time-code" placeholder="Enter verification code" required />
            </FormField>
            <Button type="submit" class="mt-8 w-full" :disabled="loading">{{ loading ? 'Verifying...' : 'Verify' }}</Button>
          </form>
        </template>

        <template v-else>
          <h1 class="font-serif text-h3 text-green-800 lg:text-h2">Register</h1>
          <h2 class="mt-12 text-h5 text-gray-600 lg:mt-14">Basic Information</h2>
          <form class="mt-10" @submit.prevent="register">
            <div class="grid gap-x-10 gap-y-7 lg:grid-cols-2">
              <FormField label="First name" for="first-name"><Input id="first-name" v-model="firstName" autocomplete="given-name" placeholder="Enter your first name" required /></FormField>
              <FormField label="Last name" for="last-name"><Input id="last-name" v-model="lastName" autocomplete="family-name" placeholder="Enter your last name" required /></FormField>
              <FormField label="Username" for="username"><Input id="username" v-model="username" autocomplete="username" placeholder="Enter your username" required /></FormField>
              <FormField label="Email" for="email"><Input id="email" v-model="email" type="email" autocomplete="email" placeholder="Enter your email" required /></FormField>
              <FormField label="Password" for="password"><Input id="password" v-model="password" type="password" autocomplete="new-password" placeholder="Enter your password" required /></FormField>
              <FormField label="Confirm password" for="confirm-password"><Input id="confirm-password" v-model="confirmPassword" type="password" autocomplete="new-password" placeholder="Confirm your password" required /></FormField>
              <FormField label="Phone number" for="phone-number"><Input id="phone-number" v-model="phoneNumber" type="tel" autocomplete="tel" placeholder="Enter your phone number" required /></FormField>
              <FormField label="Date of Birth" for="date-of-birth"><DatePicker id="date-of-birth" v-model="dateOfBirth" placeholder="Select your date of birth" /></FormField>
              <FormField label="Country" for="country">
                <Select v-model="country">
                  <SelectTrigger id="country"><SelectValue placeholder="Select your country" /></SelectTrigger>
                  <SelectContent><SelectItem v-for="item in countries" :key="item" :value="item">{{ item }}</SelectItem></SelectContent>
                </Select>
              </FormField>
            </div>
            <div class="mt-10 border-t border-gray-300 pt-10">
              <h2 class="text-h5 text-gray-600">Profile Picture</h2>
              <label for="profile-picture" class="mt-8 flex size-42 cursor-pointer flex-col items-center justify-center gap-2 rounded-sm bg-gray-100 text-body2 text-orange-500 outline-none is-focus:ring-2 is-focus:ring-ring">
                <img v-if="profilePreview" :src="profilePreview" alt="Selected profile" class="size-full rounded-sm object-cover">
                <template v-else><span class="text-h4 font-normal">+</span><span>Upload photo</span></template>
              </label>
              <input id="profile-picture" class="sr-only" type="file" accept="image/*" @change="selectProfilePicture">
            </div>
            <div id="clerk-captcha" />
            <p v-if="error" role="alert" class="mt-6 text-body2 text-red">{{ error }}</p>
            <Button type="submit" class="mt-12 w-full lg:max-w-111.5" :disabled="loading || !isLoaded">{{ loading ? 'Registering...' : 'Register' }}</Button>
          </form>
        </template>
      </section>
    </main>
  </div>
</template>
