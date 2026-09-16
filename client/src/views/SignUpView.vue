<script setup lang="ts">
import { getLocalTimeZone, today } from '@internationalized/date'
import type { DateValue } from '@internationalized/date'
import { useAuth, useSignUp, useUser } from '@clerk/vue'
import { isClerkAPIResponseError } from '@clerk/vue/errors'
import { isAxiosError } from 'axios'
import { parsePhoneNumberFromString } from 'libphonenumber-js'
import { computed, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import IconFacebook from '@/components/icons/IconFacebook.vue'
import IconGoogle from '@/components/icons/IconGoogle.vue'
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
const route = useRoute()
const router = useRouter()
const { getToken, isSignedIn } = useAuth()
const { isLoaded, signUp, setActive } = useSignUp()
const { user } = useUser()
const socialMode = ref(false)
const completedSessionId = ref<string | null>(null)

type FieldName = 'firstName' | 'lastName' | 'username' | 'email' | 'password' | 'confirmPassword' | 'phoneNumber' | 'dateOfBirth' | 'country'
interface FormValues {
  firstName: string
  lastName: string
  username: string
  email: string
  password: string
  confirmPassword: string
  phoneNumber: string
  country: string
}
const fieldNames: FieldName[] = ['firstName', 'lastName', 'username', 'email', 'password', 'confirmPassword', 'phoneNumber', 'dateOfBirth', 'country']
const fieldLabels: Record<FieldName, string> = {
  firstName: 'First name',
  lastName: 'Last name',
  username: 'Username',
  email: 'Email',
  password: 'Password',
  confirmPassword: 'Confirm password',
  phoneNumber: 'Phone number',
  dateOfBirth: 'Date of birth',
  country: 'Country',
}
const form = reactive<FormValues>({
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  phoneNumber: '',
  country: '',
})
const maximumDateOfBirth = today(getLocalTimeZone()).subtract({ years: 18 })
const dateOfBirth = shallowRef<DateValue>()
const profilePicture = shallowRef<File>()
const profilePreview = ref('')
const verificationCode = ref('')
const verificationKind = ref<VerificationKind>()
const error = ref('')
const loading = ref(false)
const requiresSocialUsername = computed(() => socialMode.value && Boolean(signUp.value?.missingFields.includes('username')))
const fieldErrors = reactive<Record<FieldName, string>>(Object.fromEntries(fieldNames.map((field) => [field, ''])) as Record<FieldName, string>)
const touchedFields = reactive<Record<FieldName, boolean>>(Object.fromEntries(fieldNames.map((field) => [field, false])) as Record<FieldName, boolean>)

watch([isLoaded, isSignedIn, user, signUp], () => {
  if (route.query.oauth !== '1' || !isLoaded.value) return
  socialMode.value = true
  form.firstName = form.firstName || user.value?.firstName || signUp.value?.firstName || ''
  form.lastName = form.lastName || user.value?.lastName || signUp.value?.lastName || ''
  form.email = form.email || user.value?.primaryEmailAddress?.emailAddress || signUp.value?.emailAddress || ''
  form.username = form.username || signUp.value?.username || ''
}, { immediate: true })

function validateField(field: FieldName) {
  if (!touchedFields[field]) return

  const value = field === 'dateOfBirth' ? dateOfBirth.value : form[field as Exclude<FieldName, 'dateOfBirth'>]
  let message = !value ? `${fieldLabels[field]} is required.` : ''
  if (!message && field === 'email' && !/^\S+@\S+\.\S+$/.test(String(value))) message = 'Please enter a valid email address.'
  if (!message && field === 'password' && String(value).length < 8) message = 'Password must be at least 8 characters.'
  if (!message && field === 'confirmPassword' && value !== form.password) message = 'Passwords do not match.'
  if (!message && field === 'phoneNumber' && !isValidPhoneNumber(String(value))) message = 'Please enter a valid phone number, e.g. 0812345678.'
  if (!message && field === 'dateOfBirth' && value && typeof value !== 'string' && value.compare(maximumDateOfBirth) > 0) message = 'You must be at least 18 years old.'
  fieldErrors[field] = message
}

function touchField(field: FieldName) {
  touchedFields[field] = true
  validateField(field)
}

function validateAllFields() {
  const fields = socialMode.value
    ? [
        'firstName',
        'lastName',
        ...(requiresSocialUsername.value ? ['username'] as FieldName[] : []),
        'phoneNumber',
        'dateOfBirth',
        'country',
      ] as FieldName[]
    : fieldNames
  fields.forEach((field) => {
    touchedFields[field] = true
    validateField(field)
  })
  return fields.some((field) => Boolean(fieldErrors[field]))
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
  if (!sessionId || !setActive.value) {
    throw new Error('Account was created, but no active session was returned.')
  }
  completedSessionId.value = sessionId
  await setActive.value({ session: sessionId })
  await finishProfile()
}

function showRegistrationError(caught: unknown) {
  if (isClerkAPIResponseError(caught)) {
    showError(caught)
    return
  }
  if (isAxiosError(caught)) {
    const status = caught.response?.status
    if (!caught.response) error.value = 'Unable to reach the profile service. Please try again.'
    else if (status === 401 || status === 403) error.value = 'Your session could not be authenticated. Please sign in and try again.'
    else if (status === 400) error.value = 'Profile information was rejected. Please check your registration details.'
    else error.value = 'Profile data could not be saved. Please try again.'
    return
  }
  error.value = 'Registration could not be completed. Please try again.'
}

async function finishProfile() {
  const token = await getToken.value()
  if (!token) throw new Error('Unable to authenticate profile creation.')

  let profilePicturePath: string | null = null
  try {
    profilePicturePath = await uploadProfilePicture(token)
  } catch {
    // Profile creation must not be lost when optional image storage fails.
  }

  await api.post('/profiles', {
    firstName: form.firstName,
    lastName: form.lastName,
    phoneNumber: normalizePhoneNumber(String(form.phoneNumber)),
    dateOfBirth: dateOfBirth.value!.toString(),
    country: form.country,
    profilePicture: profilePicturePath,
  }, {
    headers: { Authorization: `Bearer ${token}` },
  })
  await router.push('/')
}

async function registerWithSocial(strategy: 'oauth_google' | 'oauth_facebook') {
  error.value = ''
  if (!isLoaded.value || !signUp.value) return

  loading.value = true
  try {
    await signUp.value.authenticateWithRedirect({
      strategy,
      redirectUrl: '/sso-callback',
      redirectUrlComplete: '/sign-up?oauth=1',
    })
  }
  catch (caught) {
    if (isClerkAPIResponseError(caught)) showError(caught)
    else error.value = 'Unable to continue with social login. Please try again.'
    loading.value = false
  }
}

async function register() {
  error.value = ''
  if (validateAllFields()) return
  if (socialMode.value) {
    loading.value = true
    try {
      if (!isSignedIn.value && signUp.value) {
        const missingFields = signUp.value.missingFields
        const result = await signUp.value.update({
          ...(missingFields.includes('first_name') ? { firstName: form.firstName } : {}),
          ...(missingFields.includes('last_name') ? { lastName: form.lastName } : {}),
          ...(missingFields.includes('username') ? { username: form.username } : {}),
          ...(missingFields.includes('phone_number') ? { phoneNumber: normalizePhoneNumber(form.phoneNumber) } : {}),
        })
        if (result.status !== 'complete') {
          error.value = 'Please complete the remaining account information.'
          return
        }
        await finishRegistration(result.createdSessionId)
      }
      else await finishProfile()
    }
    catch (caught) {
      showRegistrationError(caught)
    }
    finally {
      loading.value = false
    }
    return
  }
  if (!isLoaded.value || !signUp.value) return

  loading.value = true
  try {
    const result = await signUp.value.create({
      username: String(form.username),
      emailAddress: String(form.email),
      password: String(form.password),
    })
    if (result.status === 'complete') {
      await finishRegistration(result.createdSessionId)
    }
    else await prepareNextVerification(result.unverifiedFields)
  } catch (caught) {
    showRegistrationError(caught)
  } finally {
    loading.value = false
  }
}

async function verify() {
  if ((!signUp.value && !completedSessionId.value) || !verificationKind.value) return
  error.value = ''
  loading.value = true
  try {
    // Email verification has already succeeded when only profile saving failed.
    if (completedSessionId.value) {
      if (isSignedIn.value) await finishProfile()
      else await finishRegistration(completedSessionId.value)
      return
    }
    if (!signUp.value) return
    const result = await signUp.value.attemptEmailAddressVerification({ code: verificationCode.value })
    if (result.status === 'complete') {
      await finishRegistration(result.createdSessionId)
    }
    else {
      verificationCode.value = ''
      await prepareNextVerification(result.unverifiedFields)
    }
  } catch (caught) {
    showRegistrationError(caught)
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
          <template v-if="!socialMode">
            <div class="mt-8 flex justify-center">
              <div class="grid w-full max-w-111.5 gap-3 sm:grid-cols-2">
                <Button type="button" variant="secondary" class="h-12 w-full gap-3" :disabled="loading || !isLoaded" @click="registerWithSocial('oauth_google')">
                  <IconGoogle class="size-5" />
                  Google
                </Button>
                <Button type="button" variant="secondary" class="h-12 w-full gap-3" :disabled="loading || !isLoaded" @click="registerWithSocial('oauth_facebook')">
                  <IconFacebook class="size-5" />
                  Facebook
                </Button>
              </div>
            </div>
            <div class="mt-8 flex items-center gap-4 text-body3 text-gray-600" aria-hidden="true">
              <span class="h-px flex-1 bg-gray-300" />
              <span>or register with email</span>
              <span class="h-px flex-1 bg-gray-300" />
            </div>
          </template>
          <h2 class="mt-12 text-h5 text-gray-600 lg:mt-14">Basic Information</h2>
          <form class="mt-10" @submit.prevent="register">
            <div class="grid gap-x-10 gap-y-7 lg:grid-cols-2">
              <FormField label="First name" for="first-name" :error="fieldErrors.firstName"><Input id="first-name" v-model="form.firstName" :aria-invalid="!!fieldErrors.firstName" aria-describedby="first-name-error" autocomplete="given-name" placeholder="Enter your first name" required @input="validateField('firstName')" @blur="touchField('firstName')" /></FormField>
              <FormField label="Last name" for="last-name" :error="fieldErrors.lastName"><Input id="last-name" v-model="form.lastName" :aria-invalid="!!fieldErrors.lastName" aria-describedby="last-name-error" autocomplete="family-name" placeholder="Enter your last name" required @input="validateField('lastName')" @blur="touchField('lastName')" /></FormField>
              <FormField v-if="!socialMode || requiresSocialUsername" label="Username" for="username" :error="fieldErrors.username"><Input id="username" v-model="form.username" :aria-invalid="!!fieldErrors.username" aria-describedby="username-error" autocomplete="username" placeholder="Enter your username" required @input="validateField('username')" @blur="touchField('username')" /></FormField>
              <template v-if="!socialMode">
                <FormField label="Email" for="email" :error="fieldErrors.email"><Input id="email" v-model="form.email" :aria-invalid="!!fieldErrors.email" aria-describedby="email-error" type="email" autocomplete="email" placeholder="Enter your email" required @input="validateField('email')" @blur="touchField('email')" /></FormField>
                <FormField label="Password" for="password" :error="fieldErrors.password"><Input id="password" v-model="form.password" :aria-invalid="!!fieldErrors.password" aria-describedby="password-error" type="password" autocomplete="new-password" placeholder="Enter your password" minlength="8" required @input="validateField('password'); validateField('confirmPassword')" @blur="touchField('password')" /></FormField>
                <FormField label="Confirm password" for="confirm-password" :error="fieldErrors.confirmPassword"><Input id="confirm-password" v-model="form.confirmPassword" :aria-invalid="!!fieldErrors.confirmPassword" aria-describedby="confirm-password-error" type="password" autocomplete="new-password" placeholder="Confirm your password" minlength="8" required @input="validateField('confirmPassword')" @blur="touchField('confirmPassword')" /></FormField>
              </template>
              <FormField label="Phone number" for="phone-number" :error="fieldErrors.phoneNumber"><Input id="phone-number" v-model="form.phoneNumber" :aria-invalid="!!fieldErrors.phoneNumber" aria-describedby="phone-number-error" type="tel" inputmode="tel" autocomplete="tel" placeholder="0812345678" required @input="validateField('phoneNumber')" @blur="touchField('phoneNumber')" /></FormField>
              <FormField label="Date of Birth" for="date-of-birth" :error="fieldErrors.dateOfBirth"><DatePicker id="date-of-birth" v-model="dateOfBirth" :max-value="maximumDateOfBirth" placeholder="Select your date of birth" @update:model-value="touchField('dateOfBirth')" /></FormField>
              <FormField label="Country" for="country" :error="fieldErrors.country">
                <Select v-model="form.country" @update:model-value="touchField('country')">
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
