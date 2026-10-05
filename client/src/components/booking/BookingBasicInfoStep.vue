<!-- Figma: user > booking > basic information -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DatePicker } from '@/components/ui/date-picker'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { BasicInfoField, GuestDetails } from '@/data/booking'
import { countries } from '@/data/countries'

const guest = defineModel<GuestDetails>('guest', { required: true })
const dateOfBirth = defineModel<DateValue | undefined>('dateOfBirth', { required: true })

defineProps<{
  errors: Record<BasicInfoField, string>
  maximumDateOfBirth: DateValue
}>()

const emit = defineEmits<{
  touch: [field: BasicInfoField]
}>()
</script>

<template>
  <div class="flex flex-col gap-6">
    <h2 class="text-h5 text-gray-800">
      Basic Information
    </h2>

    <div class="grid gap-6 md:grid-cols-2">
      <FormField label="First name" for="first-name" :error="errors.firstName">
        <Input
          id="first-name"
          v-model="guest.firstName"
          autocomplete="given-name"
          placeholder="Enter your first name"
          :aria-invalid="!!errors.firstName"
          aria-describedby="first-name-error"
          @blur="emit('touch', 'firstName')"
        />
      </FormField>
      <FormField label="Last name" for="last-name" :error="errors.lastName">
        <Input
          id="last-name"
          v-model="guest.lastName"
          autocomplete="family-name"
          placeholder="Enter your last name"
          :aria-invalid="!!errors.lastName"
          aria-describedby="last-name-error"
          @blur="emit('touch', 'lastName')"
        />
      </FormField>
    </div>

    <FormField label="Email" for="email" :error="errors.email">
      <Input
        id="email"
        v-model="guest.email"
        type="email"
        inputmode="email"
        autocomplete="email"
        spellcheck="false"
        placeholder="Enter your email"
        :aria-invalid="!!errors.email"
        aria-describedby="email-error"
        @blur="emit('touch', 'email')"
      />
    </FormField>

    <FormField label="Phone number" for="phone-number" :error="errors.phoneNumber">
      <Input
        id="phone-number"
        v-model="guest.phoneNumber"
        type="tel"
        inputmode="tel"
        autocomplete="tel"
        placeholder="088 888 8888"
        :aria-invalid="!!errors.phoneNumber"
        aria-describedby="phone-number-error"
        @blur="emit('touch', 'phoneNumber')"
      />
    </FormField>

    <FormField label="Date of Birth" for="date-of-birth" :error="errors.dateOfBirth">
      <DatePicker
        id="date-of-birth"
        v-model="dateOfBirth"
        placeholder="Select your date of birth"
        :max-value="maximumDateOfBirth"
        :invalid="!!errors.dateOfBirth"
        aria-describedby="date-of-birth-error"
        @update:model-value="emit('touch', 'dateOfBirth')"
      />
    </FormField>

    <FormField label="Country" for="country" :error="errors.country">
      <Select :model-value="guest.country || undefined" @update:model-value="(value) => { if (typeof value === 'string') guest.country = value; emit('touch', 'country') }">
        <SelectTrigger id="country" :aria-invalid="!!errors.country" aria-describedby="country-error">
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
</template>
