<script setup lang="ts">
import { getLocalTimeZone, today } from '@internationalized/date'
import type { DateValue } from '@internationalized/date'
import { useAuth, useSignUp } from '@clerk/vue'
import { isClerkAPIResponseError } from '@clerk/vue/errors'
import { parsePhoneNumberFromString } from 'libphonenumber-js'
import { reactive, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

type VerificationKind = 'email_address'
const DEFAULT_PHONE_COUNTRY = 'TH'

const countries = `
Afghanistan
Albania
Algeria
Andorra
Angola
Antigua and Barbuda
Argentina
Armenia
Australia
Austria
Azerbaijan
Bahamas
Bahrain
Bangladesh
Barbados
Belarus
Belgium
Belize
Benin
Bhutan
Bolivia
Bosnia and Herzegovina
Botswana
Brazil
Brunei
Bulgaria
Burkina Faso
Burundi
Cabo Verde
Cambodia
Cameroon
Canada
Central African Republic
Chad
Chile
China
Colombia
Comoros
Congo
Costa Rica
Côte d'Ivoire
Croatia
Cuba
Cyprus
Czechia
Denmark
Djibouti
Dominica
Dominican Republic
Ecuador
Egypt
El Salvador
Equatorial Guinea
Eritrea
Estonia
Eswatini
Ethiopia
Fiji
Finland
France
Gabon
Gambia
Georgia
Germany
Ghana
Greece
Grenada
Guatemala
Guinea
Guinea-Bissau
Guyana
Haiti
Honduras
Hungary
Iceland
India
Indonesia
Iran
Iraq
Ireland
Israel
Italy
Jamaica
Japan
Jordan
Kazakhstan
Kenya
Kiribati
Kuwait
Kyrgyzstan
Laos
Latvia
Lebanon
Lesotho
Liberia
Libya
Liechtenstein
Lithuania
Luxembourg
Madagascar
Malawi
Malaysia
Maldives
Mali
Malta
Marshall Islands
Mauritania
Mauritius
Mexico
Micronesia
Moldova
Monaco
Mongolia
Montenegro
Morocco
Mozambique
Myanmar
Namibia
Nauru
Nepal
Netherlands
New Zealand
Nicaragua
Niger
Nigeria
North Korea
North Macedonia
Norway
Oman
Pakistan
Palau
Palestine
Panama
Papua New Guinea
Paraguay
Peru
Philippines
Poland
Portugal
Qatar
Romania
Russia
Rwanda
Saint Kitts and Nevis
Saint Lucia
Saint Vincent and the Grenadines
Samoa
San Marino
Sao Tome and Principe
Saudi Arabia
Senegal
Serbia
Seychelles
Sierra Leone
Singapore
Slovakia
Slovenia
Solomon Islands
Somalia
South Africa
South Korea
South Sudan
Spain
Sri Lanka
Sudan
Suriname
Sweden
Switzerland
Syria
Taiwan
Tajikistan
Tanzania
Thailand
Timor-Leste
Togo
Tonga
Trinidad and Tobago
Tunisia
Türkiye
Turkmenistan
Tuvalu
Uganda
Ukraine
United Arab Emirates
United Kingdom
United States
Uruguay
Uzbekistan
Vanuatu
Vatican City
Venezuela
Vietnam
Yemen
Zambia
Zimbabwe
`.trim().split('\n')
const router = useRouter()
const { getToken } = useAuth()
const { isLoaded, signUp, setActive } = useSignUp()

const firstName = ref('')
const lastName = ref('')
const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const phoneNumber = ref('')
const dateOfBirth = shallowRef<DateValue>()
const maximumDateOfBirth = today(getLocalTimeZone()).subtract({ years: 18 })
const country = ref('')
const profilePicture = shallowRef<File>()
const profilePreview = ref('')
const verificationCode = ref('')
const verificationKind = ref<VerificationKind>()
const error = ref('')
const loading = ref(false)
type FieldName = 'firstName' | 'lastName' | 'username' | 'email' | 'password' | 'confirmPassword' | 'phoneNumber' | 'dateOfBirth' | 'country'
const fieldErrors = reactive<Record<FieldName, string>>({
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  phoneNumber: '',
  dateOfBirth: '',
  country: '',
})
const touchedFields = reactive<Record<FieldName, boolean>>({
  firstName: false,
  lastName: false,
  username: false,
  email: false,
  password: false,
  confirmPassword: false,
  phoneNumber: false,
  dateOfBirth: false,
  country: false,
})

function validateField(field: FieldName) {
  if (!touchedFields[field]) return

  const value = {
    firstName: firstName.value,
    lastName: lastName.value,
    username: username.value,
    email: email.value,
    password: password.value,
    confirmPassword: confirmPassword.value,
    phoneNumber: phoneNumber.value,
    dateOfBirth: dateOfBirth.value,
    country: country.value,
  }[field]

  fieldErrors[field] = !value
    ? `${field === 'dateOfBirth' ? 'Date of birth' : field === 'confirmPassword' ? 'Confirm password' : field.charAt(0).toUpperCase() + field.slice(1)} is required.`
    : field === 'email' && !/^\S+@\S+\.\S+$/.test(String(value))
      ? 'Please enter a valid email address.'
      : field === 'password' && String(value).length < 8
        ? 'Password must be at least 8 characters.'
        : field === 'confirmPassword' && value !== password.value
          ? 'Passwords do not match.'
          : field === 'phoneNumber' && !isValidPhoneNumber(String(value))
            ? 'Please enter a valid phone number, e.g. 0812345678.'
            : field === 'dateOfBirth' && dateOfBirth.value && dateOfBirth.value.compare(maximumDateOfBirth) > 0
              ? 'You must be at least 18 years old.'
              : ''
}

function touchField(field: FieldName) {
  touchedFields[field] = true
  validateField(field)
}

function validateAllFields() {
  ;(Object.keys(fieldErrors) as FieldName[]).forEach((field) => {
    touchedFields[field] = true
    validateField(field)
  })
  return Object.values(fieldErrors).some(Boolean)
}

function normalizePhoneNumber(value: string) {
  return parsePhoneNumberFromString(value, DEFAULT_PHONE_COUNTRY)?.number ?? ''
}

function isValidPhoneNumber(value: string) {
  const phoneNumber = parsePhoneNumberFromString(value, DEFAULT_PHONE_COUNTRY)
  return phoneNumber?.country === DEFAULT_PHONE_COUNTRY && phoneNumber.isValid()
}

function selectProfilePicture(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  profilePicture.value = file
  if (profilePreview.value) URL.revokeObjectURL(profilePreview.value)
  profilePreview.value = URL.createObjectURL(file)
}

function showError(caught: unknown) {
  if (!isClerkAPIResponseError(caught)) {
    error.value = 'Unable to create your account. Please try again.'
    return
  }

  const clerkError = caught.errors[0]
  const message = clerkError?.longMessage ?? clerkError?.message ?? 'Unable to create your account.'
  const metadata = clerkError as typeof clerkError & { meta?: { paramName?: string }; fields?: string[] }
  const field = `${metadata.meta?.paramName ?? metadata.fields?.[0] ?? ''}`.toLowerCase()

  if (field.includes('username') || message.toLowerCase().includes('username')) {
    fieldErrors.username = message
  }
  else if (field.includes('email') || message.toLowerCase().includes('email')) {
    fieldErrors.email = message
  }
  else error.value = message
}

async function prepareNextVerification(unverifiedFields: string[]) {
  if (!signUp.value) return
  if (unverifiedFields.includes('email_address')) {
    await signUp.value.prepareEmailAddressVerification({ strategy: 'email_code' })
    verificationKind.value = 'email_address'
  }
}

async function uploadProfilePicture(token: string) {
  if (!profilePicture.value) return null

  const formData = new FormData()
  formData.append('file', profilePicture.value)
  const { data } = await api.post<{ data: string }>('/profiles/picture', formData, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}

async function finishRegistration(sessionId: string | null) {
  if (!sessionId || !setActive.value) return
  await setActive.value({ session: sessionId })
  const token = await getToken.value()
  if (!token) throw new Error('Unable to authenticate profile creation.')
  const profilePicturePath = await uploadProfilePicture(token)
  await api.post('/profiles', {
    firstName: firstName.value,
    lastName: lastName.value,
    phoneNumber: normalizePhoneNumber(phoneNumber.value),
    dateOfBirth: dateOfBirth.value!.toString(),
    country: country.value,
    profilePicture: profilePicturePath,
  }, {
    headers: { Authorization: `Bearer ${token}` },
  })
  await router.push('/')
}

async function register() {
  error.value = ''
  if (validateAllFields()) return
  if (!isLoaded.value || !signUp.value) return

  loading.value = true
  try {
    const result = await signUp.value.create({
      username: username.value,
      emailAddress: email.value,
      password: password.value,
    })
    if (result.status === 'complete') {
      await finishRegistration(result.createdSessionId)
    }
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
    const result = await signUp.value.attemptEmailAddressVerification({ code: verificationCode.value })
    if (result.status === 'complete') {
      await finishRegistration(result.createdSessionId)
    }
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
      style="background-image: url('/site-background.jpg')"
    >
      <section class="mx-auto max-w-273 rounded-sm bg-bg px-6 py-12 lg:px-20 lg:py-22.5">
        <template v-if="verificationKind">
          <h1 class="font-serif text-h3 text-green-800">Verify your account</h1>
          <p class="mt-8 text-body1 text-gray-700">
            Enter the code sent to your email.
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
              <FormField label="First name" for="first-name" :error="fieldErrors.firstName"><Input id="first-name" v-model="firstName" :aria-invalid="!!fieldErrors.firstName" aria-describedby="first-name-error" autocomplete="given-name" placeholder="Enter your first name" required @input="validateField('firstName')" @blur="touchField('firstName')" /></FormField>
              <FormField label="Last name" for="last-name" :error="fieldErrors.lastName"><Input id="last-name" v-model="lastName" :aria-invalid="!!fieldErrors.lastName" aria-describedby="last-name-error" autocomplete="family-name" placeholder="Enter your last name" required @input="validateField('lastName')" @blur="touchField('lastName')" /></FormField>
              <FormField label="Username" for="username" :error="fieldErrors.username"><Input id="username" v-model="username" :aria-invalid="!!fieldErrors.username" aria-describedby="username-error" autocomplete="username" placeholder="Enter your username" required @input="validateField('username')" @blur="touchField('username')" /></FormField>
              <FormField label="Email" for="email" :error="fieldErrors.email"><Input id="email" v-model="email" :aria-invalid="!!fieldErrors.email" aria-describedby="email-error" type="email" autocomplete="email" placeholder="Enter your email" required @input="validateField('email')" @blur="touchField('email')" /></FormField>
              <FormField label="Password" for="password" :error="fieldErrors.password"><Input id="password" v-model="password" :aria-invalid="!!fieldErrors.password" aria-describedby="password-error" type="password" autocomplete="new-password" placeholder="Enter your password" minlength="8" required @input="validateField('password'); validateField('confirmPassword')" @blur="touchField('password')" /></FormField>
              <FormField label="Confirm password" for="confirm-password" :error="fieldErrors.confirmPassword"><Input id="confirm-password" v-model="confirmPassword" :aria-invalid="!!fieldErrors.confirmPassword" aria-describedby="confirm-password-error" type="password" autocomplete="new-password" placeholder="Confirm your password" minlength="8" required @input="validateField('confirmPassword')" @blur="touchField('confirmPassword')" /></FormField>
              <FormField label="Phone number" for="phone-number" :error="fieldErrors.phoneNumber"><Input id="phone-number" v-model="phoneNumber" :aria-invalid="!!fieldErrors.phoneNumber" aria-describedby="phone-number-error" type="tel" inputmode="tel" autocomplete="tel" placeholder="0812345678" required @input="validateField('phoneNumber')" @blur="touchField('phoneNumber')" /></FormField>
              <FormField label="Date of Birth" for="date-of-birth" :error="fieldErrors.dateOfBirth"><DatePicker id="date-of-birth" v-model="dateOfBirth" :max-value="maximumDateOfBirth" placeholder="Select your date of birth" @update:model-value="touchField('dateOfBirth')" /></FormField>
              <FormField label="Country" for="country" :error="fieldErrors.country">
                <Select v-model="country" @update:model-value="touchField('country')">
                  <SelectTrigger id="country"><SelectValue placeholder="Select your country" /></SelectTrigger>
                  <SelectContent><SelectItem v-for="item in countries" :key="item" :value="item">{{ item }}</SelectItem></SelectContent>
                </Select>
              </FormField>
            </div>
            <div class="mt-10 border-t border-gray-300 pt-10">
              <h2 class="text-h5 text-gray-600">Profile Picture</h2>
              <label for="profile-picture" class="mt-8 flex size-42 cursor-pointer flex-col items-center justify-center gap-2 rounded-sm bg-gray-200 text-body2 text-orange-500 outline-none is-focus:ring-2 is-focus:ring-ring">
                <img v-if="profilePreview" :src="profilePreview" alt="Selected profile" class="size-full rounded-sm object-cover" />
                <template v-else><span class="text-h4 font-normal">+</span><span>Upload photo</span></template>
              </label>
              <input id="profile-picture" class="sr-only" type="file" accept="image/jpeg,image/png,image/webp" @change="selectProfilePicture">
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
