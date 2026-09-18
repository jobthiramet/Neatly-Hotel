<!-- Figma: user > booking (basic information / special request / payment method) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { getLocalTimeZone, parseDate, today } from '@internationalized/date'
import { useAuth, useUser } from '@clerk/vue'
import { useDebounceFn, useIntervalFn } from '@vueuse/core'
import { parsePhoneNumberFromString } from 'libphonenumber-js'
import { computed, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isAxiosError } from 'axios'
import { toast } from 'vue-sonner'
import { api } from '@/api/client'
import { createBooking, type CreateBookingRequest } from '@/api/bookings'
import BookingBasicInfoStep from '@/components/booking/BookingBasicInfoStep.vue'
import BookingDetailSidebar from '@/components/booking/BookingDetailSidebar.vue'
import BookingPaymentStep from '@/components/booking/BookingPaymentStep.vue'
import BookingSpecialRequestStep from '@/components/booking/BookingSpecialRequestStep.vue'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Stepper, StepperIndicator, StepperItem, StepperTitle, StepperTrigger } from '@/components/ui/stepper'
import type { BasicInfoField, CheckoutPayment, GuestDetails } from '@/data/booking'
import {
  BOOKING_HOLD_SECONDS,
  CHECKOUT_STEPS,
  nightsBetween,
  promotionDiscount,
  specialRequests,
  standardRequests,
} from '@/data/booking'
import { countries } from '@/data/countries'
import { defaultRoomId, roomDetails } from '@/data/rooms'
import { useRoomsStore, type RoomResponse } from '@/stores/rooms'

const DEFAULT_PHONE_COUNTRY = 'TH'

interface ProfileResponse {
  firstName: string
  lastName: string
  phoneNumber: string
  dateOfBirth: string | number[]
  country: string
}

interface ApiResponse<T> {
  data: T
}

const route = useRoute()
const router = useRouter()
const { getToken, isLoaded } = useAuth()
const { user } = useUser()

const maximumDateOfBirth = today(getLocalTimeZone()).subtract({ years: 18 })

function parseStayDate(value: unknown, fallback: DateValue) {
  if (typeof value !== 'string')
    return fallback
  try {
    return parseDate(value)
  }
  catch {
    return fallback
  }
}

const defaultCheckIn = today(getLocalTimeZone())
const checkIn = computed(() => parseStayDate(route.query.checkIn, defaultCheckIn))
const checkOut = computed(() => {
  const parsed = parseStayDate(route.query.checkOut, checkIn.value.add({ days: 1 }))
  return parsed.compare(checkIn.value) <= 0 ? checkIn.value.add({ days: 1 }) : parsed
})
const guests = computed(() => {
  const value = Number(route.query.guests)
  return Number.isInteger(value) && value > 0 ? value : 2
})
// Rooms of this type to book (from Search Result); checkout needs that many free units.
const rooms = computed(() => {
  const value = Number(route.query.rooms)
  return Number.isInteger(value) && value >= 1 && value <= 10 ? value : 1
})
const roomsStore = useRoomsStore()
const apiRoom = ref<RoomResponse | null>(null)
const roomLoadError = ref('')
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const roomId = computed(() => {
  const value = typeof route.query.roomId === 'string' ? route.query.roomId : ''
  return roomDetails[value] ? value : (UUID_RE.test(value) ? value : defaultRoomId)
})
const mockRoom = computed(() => roomDetails[roomId.value] ?? roomDetails[defaultRoomId]!)
const roomName = computed(() => apiRoom.value?.name ?? mockRoom.value.name)
const roomPrice = computed(() => apiRoom.value?.promotionPrice ?? apiRoom.value?.pricePerNight ?? mockRoom.value.currentPrice)
const roomTypeId = computed(() => apiRoom.value?.id ?? '')

const step = ref(1)
const guest = reactive<GuestDetails>({
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  country: '',
})
const dateOfBirth = shallowRef<DateValue>()
const selectedRequestIds = ref<string[]>([])
const additionalRequest = ref('')
const payment = reactive<CheckoutPayment>({
  method: 'credit',
  cardNumber: '',
  expiry: '',
  cvc: '',
  cardOwner: '',
  promotionCode: '',
})
const paymentStep = ref<{ confirmCard: () => Promise<{ ok: boolean, message?: string }> } | null>(null)
const clientSecret = ref<string | null>(null)
const pendingBookingId = ref('')
const stripeError = ref('')
const submitting = ref(false)

const basicFields: BasicInfoField[] = ['firstName', 'lastName', 'email', 'phoneNumber', 'dateOfBirth', 'country']
const basicLabels: Record<BasicInfoField, string> = {
  firstName: 'First name',
  lastName: 'Last name',
  email: 'Email',
  phoneNumber: 'Phone number',
  dateOfBirth: 'Date of birth',
  country: 'Country',
}
const basicErrors = reactive<Record<BasicInfoField, string>>({
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  dateOfBirth: '',
  country: '',
})

const remainingSeconds = ref(BOOKING_HOLD_SECONDS)
const expiredOpen = ref(false)

const { pause: pauseTimer } = useIntervalFn(() => {
  if (remainingSeconds.value <= 0)
    return
  remainingSeconds.value -= 1
  if (remainingSeconds.value === 0) {
    pauseTimer()
    expiredOpen.value = true
  }
}, 1000)

const nights = computed(() => Math.max(1, nightsBetween(checkIn.value, checkOut.value)))
const roomAmount = computed(() => roomPrice.value * nights.value * rooms.value)
const extraItems = computed(() =>
  specialRequests
    .filter(item => selectedRequestIds.value.includes(item.id))
    .map(item => ({ label: item.label, amount: item.price })),
)
const promotionAmount = computed(() => promotionDiscount(payment.promotionCode))
const lineItems = computed(() => {
  const items = [
    { label: rooms.value > 1 ? `${rooms.value} × ${roomName.value} Room` : `${roomName.value} Room`, amount: roomAmount.value },
    ...extraItems.value,
  ]
  if (promotionAmount.value)
    items.push({ label: 'Promotion Code', amount: -promotionAmount.value })
  return items
})
const total = computed(() => Math.max(0, lineItems.value.reduce((sum, item) => sum + item.amount, 0)))

function parseStep(value: unknown) {
  const parsed = Number(value)
  if (parsed === 1 || parsed === 2 || parsed === 3)
    return parsed
  return 1
}

watch(() => route.query.step, (value) => {
  const next = parseStep(value)
  if (next !== step.value)
    step.value = next
}, { immediate: true })

watch(step, (value) => {
  if (String(route.query.step) === String(value))
    return
  router.replace({ query: { ...route.query, step: String(value) } })
})

function isValidPhoneNumber(value: string) {
  const phoneNumber = parsePhoneNumberFromString(value, DEFAULT_PHONE_COUNTRY)
  return Boolean(phoneNumber?.isValid())
}

function validateBasicField(field: BasicInfoField) {
  const value = field === 'dateOfBirth' ? dateOfBirth.value : guest[field]
  let message = !value ? `${basicLabels[field]} is required.` : ''
  if (!message && field === 'email' && !/^\S+@\S+\.\S+$/.test(String(value)))
    message = 'Please enter a valid email address.'
  if (!message && field === 'phoneNumber' && !isValidPhoneNumber(String(value)))
    message = 'Please enter a valid phone number, e.g. 0812345678.'
  if (!message && field === 'dateOfBirth' && dateOfBirth.value && dateOfBirth.value.compare(maximumDateOfBirth) > 0)
    message = 'You must be at least 18 years old.'
  basicErrors[field] = message
}

function touchBasicField(field: BasicInfoField) {
  validateBasicField(field)
}

function validateBasic() {
  basicFields.forEach(validateBasicField)
  return basicFields.every(field => !basicErrors[field])
}

function focusFirstBasicError() {
  const first = basicFields.find(field => basicErrors[field])
  if (!first)
    return
  const ids: Record<BasicInfoField, string> = {
    firstName: 'first-name',
    lastName: 'last-name',
    email: 'email',
    phoneNumber: 'phone-number',
    dateOfBirth: 'date-of-birth',
    country: 'country',
  }
  document.getElementById(ids[first])?.focus()
}

function apiErrorMessage(error: unknown, fallback: string) {
  if (isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.trim())
      return message
  }
  return fallback
}

async function resolveRoom() {
  roomLoadError.value = ''
  try {
    const requested = typeof route.query.roomId === 'string' ? route.query.roomId : ''
    if (UUID_RE.test(requested)) {
      apiRoom.value = await roomsStore.get(requested)
      return
    }
    const page = await roomsStore.list({ search: mockRoom.value.name, page: 0, size: 50 })
    const match = page.content.find(item => item.name.toLowerCase() === mockRoom.value.name.toLowerCase())
      ?? page.content[0]
    if (!match)
      throw new Error('Room not found')
    apiRoom.value = await roomsStore.get(match.id)
  }
  catch {
    roomLoadError.value = 'Could not load this room from the server.'
    stripeError.value = roomLoadError.value
  }
}

function buildRequest(method: 'STRIPE' | 'CASH'): CreateBookingRequest {
  const standardIds = new Set(standardRequests.map(item => item.id))
  const specialIds = new Set(specialRequests.map(item => item.id))
  return {
    roomTypeId: roomTypeId.value,
    checkIn: checkIn.value.toString(),
    checkOut: checkOut.value.toString(),
    guests: guests.value,
    roomsCount: rooms.value,
    firstName: guest.firstName.trim(),
    lastName: guest.lastName.trim(),
    email: guest.email.trim(),
    phoneNumber: guest.phoneNumber.trim(),
    country: guest.country.trim(),
    dateOfBirth: dateOfBirth.value!.toString(),
    standardRequestCodes: selectedRequestIds.value.filter(id => standardIds.has(id)),
    specialRequestCodes: selectedRequestIds.value.filter(id => specialIds.has(id)),
    additionalRequest: additionalRequest.value.trim(),
    promotionCode: payment.promotionCode.trim(),
    paymentMethod: method,
  }
}

async function ensureStripeSession() {
  if (step.value !== 3 || payment.method !== 'credit' || !roomTypeId.value || !dateOfBirth.value)
    return
  const token = await sessionToken()
  if (!token)
    return
  try {
    stripeError.value = ''
    const booking = await createBooking(token, buildRequest('STRIPE'))
    pendingBookingId.value = booking.id
    clientSecret.value = booking.clientSecret
  }
  catch (error) {
    clientSecret.value = null
    stripeError.value = apiErrorMessage(error, 'Could not start card payment.')
  }
}

const startStripeSession = useDebounceFn(ensureStripeSession, 400)

watch(
  () => [
    step.value,
    payment.method,
    payment.promotionCode,
    selectedRequestIds.value.join(','),
    roomTypeId.value,
    dateOfBirth.value?.toString() ?? '',
  ] as const,
  () => {
    if (step.value === 3 && payment.method === 'credit')
      void startStripeSession()
    if (payment.method === 'cash') {
      clientSecret.value = null
      pendingBookingId.value = ''
    }
  },
  { immediate: true },
)

watch(roomId, () => {
  void resolveRoom()
}, { immediate: true })

async function confirmBooking() {
  if (!payment.method) {
    stripeError.value = 'Select a payment method.'
    return
  }
  if (!roomTypeId.value) {
    toast.error(roomLoadError.value || 'Room is not available to book.')
    return
  }
  const token = await sessionToken()
  if (!token) {
    toast.error('Please sign in to complete this booking.')
    return
  }
  submitting.value = true
  stripeError.value = ''
  try {
    if (payment.method === 'cash') {
      const booking = await createBooking(token, buildRequest('CASH'))
      await router.push({ name: 'booking-success', params: { bookingId: booking.id } })
      return
    }
    if (!clientSecret.value)
      await ensureStripeSession()
    const confirmed = await paymentStep.value?.confirmCard()
    if (!confirmed?.ok) {
      const message = confirmed?.message || 'Payment failed.'
      const formNotReady = /not ready/i.test(message)
      if (formNotReady || !pendingBookingId.value) {
        stripeError.value = message
        return
      }
      await router.push({
        name: 'booking-failed',
        params: { bookingId: pendingBookingId.value },
        query: route.query,
      })
      return
    }
    await router.push({ name: 'booking-success', params: { bookingId: pendingBookingId.value } })
  }
  catch (error) {
    stripeError.value = apiErrorMessage(error, 'Could not complete this booking.')
    toast.error(stripeError.value)
  }
  finally {
    submitting.value = false
  }
}

function onStepChange(next: number | undefined) {
  if (next == null || next === step.value)
    return
  if (next < step.value) {
    step.value = next
    return
  }
  if (next === step.value + 1)
    onContinue()
}

function goBack() {
  if (step.value <= 1) {
    router.back()
    return
  }
  step.value -= 1
}

function onContinue() {
  if (step.value === 1) {
    if (!validateBasic()) {
      focusFirstBasicError()
      return
    }
    step.value = 2
    return
  }
  if (step.value === 2) {
    step.value = 3
    return
  }
  void confirmBooking()
}

function roomDetailSlug() {
  if (roomDetails[roomId.value])
    return roomId.value
  const match = Object.entries(roomDetails).find(([, room]) => room.name === roomName.value)
  return match?.[0] ?? defaultRoomId
}

function leaveExpiredSession() {
  expiredOpen.value = false
  router.push({ name: 'room-detail', params: { roomId: roomDetailSlug() } })
}

function fillIfEmpty(field: keyof GuestDetails, value: string | null | undefined) {
  const next = value?.trim()
  if (!guest[field] && next)
    guest[field] = next
}

function metadataString(source: Record<string, unknown> | undefined, key: string) {
  const value = source?.[key]
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

function matchCountry(value: string | null | undefined) {
  const normalized = value?.trim().toLowerCase()
  if (!normalized)
    return
  return countries.find(item => item.toLowerCase() === normalized)
}

function formatPhoneForInput(value: string | null | undefined) {
  if (!value)
    return
  return parsePhoneNumberFromString(value, DEFAULT_PHONE_COUNTRY)?.formatNational() ?? value
}

function parseBirthDate(value: unknown): DateValue | undefined {
  if (typeof value === 'string') {
    try {
      return parseDate(value.slice(0, 10))
    }
    catch {
      return undefined
    }
  }
  if (Array.isArray(value) && value.length >= 3 && value.every(part => typeof part === 'number')) {
    const [year, month, day] = value
    try {
      return parseDate(`${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`)
    }
    catch {
      return undefined
    }
  }
  return undefined
}

function applyClerkIdentity() {
  const current = user.value
  if (!current)
    return

  fillIfEmpty('firstName', current.firstName)
  fillIfEmpty('lastName', current.lastName)
  if (!guest.firstName && current.fullName) {
    const [first, ...rest] = current.fullName.trim().split(/\s+/)
    fillIfEmpty('firstName', first)
    fillIfEmpty('lastName', rest.join(' '))
  }
  fillIfEmpty('email', current.primaryEmailAddress?.emailAddress ?? current.emailAddresses[0]?.emailAddress)
  fillIfEmpty(
    'phoneNumber',
    formatPhoneForInput(current.primaryPhoneNumber?.phoneNumber ?? current.phoneNumbers[0]?.phoneNumber),
  )

  const metadata = {
    ...(current.publicMetadata as Record<string, unknown> | undefined),
    ...(current.unsafeMetadata as Record<string, unknown> | undefined),
  }
  fillIfEmpty('firstName', metadataString(metadata, 'firstName'))
  fillIfEmpty('lastName', metadataString(metadata, 'lastName'))
  fillIfEmpty('phoneNumber', formatPhoneForInput(metadataString(metadata, 'phoneNumber')))
  fillIfEmpty('country', matchCountry(metadataString(metadata, 'country')))
  if (!dateOfBirth.value)
    dateOfBirth.value = parseBirthDate(metadata.dateOfBirth)
}

async function sessionToken() {
  for (let attempt = 0; attempt < 10; attempt++) {
    const fromSession = await window.Clerk?.session?.getToken()
    if (fromSession)
      return fromSession
    const tokenFn = getToken.value
    if (typeof tokenFn === 'function') {
      const token = await tokenFn()
      if (token)
        return token
    }
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  return null
}

async function applySavedProfile() {
  const token = await sessionToken()
  if (!token)
    return
  const { data } = await api.get<ApiResponse<ProfileResponse>>('/profiles/me', {
    headers: { Authorization: `Bearer ${token}` },
  })
  const profile = data.data
  if (!profile)
    return
  if (profile.firstName)
    guest.firstName = profile.firstName.trim()
  if (profile.lastName)
    guest.lastName = profile.lastName.trim()
  const phone = formatPhoneForInput(profile.phoneNumber)
  if (phone)
    guest.phoneNumber = phone
  const country = matchCountry(profile.country)
  if (country)
    guest.country = country
  const birthDate = parseBirthDate(profile.dateOfBirth)
  if (birthDate)
    dateOfBirth.value = birthDate
}

function clearFilledFieldErrors() {
  basicFields.forEach((field) => {
    if (basicErrors[field])
      validateBasicField(field)
  })
}

const prefilledUserId = ref<string>()

watch(
  () => [isLoaded.value, user.value?.id] as const,
  async ([ready, userId]) => {
    if (!ready || !userId || prefilledUserId.value === userId)
      return
    applyClerkIdentity()
    try {
      await applySavedProfile()
    }
    catch {
      // Prefill is optional; the guest can complete the form by hand.
    }
    prefilledUserId.value = userId
    clearFilledFieldErrors()
  },
  { immediate: true },
)
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />

    <main class="flex-1">
      <section aria-labelledby="booking-title" class="mx-auto max-w-288 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32">
        <h1 id="booking-title" class="font-serif text-h3 text-green-800 lg:text-h2">
          Booking Room
        </h1>

        <Stepper :model-value="step" class="mt-8 flex-col gap-4 md:mt-10 md:flex-row md:gap-10" @update:model-value="onStepChange">
          <StepperItem v-for="(title, index) in CHECKOUT_STEPS" :key="title" :step="index + 1">
            <StepperTrigger>
              <StepperIndicator>{{ index + 1 }}</StepperIndicator>
              <StepperTitle>{{ title }}</StepperTitle>
            </StepperTrigger>
          </StepperItem>
        </Stepper>

        <form class="mt-8 grid items-start gap-6 lg:mt-10 lg:grid-cols-3" @submit.prevent="onContinue">
          <div class="order-1 rounded-sm bg-white p-6 shadow-md lg:col-span-2 lg:p-10">
            <BookingBasicInfoStep
              v-if="step === 1"
              v-model:guest="guest"
              v-model:date-of-birth="dateOfBirth"
              :errors="basicErrors"
              :maximum-date-of-birth="maximumDateOfBirth"
              @touch="touchBasicField"
            />
            <BookingSpecialRequestStep
              v-else-if="step === 2"
              v-model:selected-ids="selectedRequestIds"
              v-model:additional-request="additionalRequest"
            />
            <BookingPaymentStep
              v-else
              ref="paymentStep"
              v-model:payment="payment"
              :client-secret="clientSecret"
              :stripe-error="stripeError"
            />

            <div class="mt-10 hidden items-center justify-between gap-4 lg:flex">
              <Button type="button" variant="ghost" @click="goBack">
                Back
              </Button>
              <Button type="submit" :disabled="submitting">
                {{ step === 3 ? (submitting ? 'Confirming…' : 'Confirm Booking') : 'Next' }}
              </Button>
            </div>
          </div>

          <div class="order-2 lg:col-start-3">
            <BookingDetailSidebar
              :remaining-seconds="remainingSeconds"
              :check-in="checkIn"
              :check-out="checkOut"
              :guests="guests"
              :line-items="lineItems"
              :total="total"
            />
          </div>

          <div class="order-3 flex items-center justify-between gap-4 lg:hidden">
            <Button type="button" variant="ghost" @click="goBack">
              Back
            </Button>
            <Button type="submit" :disabled="submitting">
              {{ step === 3 ? (submitting ? 'Confirming…' : 'Confirm Booking') : 'Next' }}
            </Button>
          </div>
        </form>
      </section>
    </main>

    <SiteFooter />

    <Dialog :open="expiredOpen" @update:open="(open) => { if (!open) leaveExpiredSession() }">
      <DialogContent :show-close-button="false">
        <DialogHeader>
          <DialogTitle>Booking session expired</DialogTitle>
        </DialogHeader>
        <DialogDescription>
          Your 5-minute hold has ended. Return to the room to start a new booking.
        </DialogDescription>
        <DialogFooter>
          <Button type="button" @click="leaveExpiredSession">
            Back to room
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
