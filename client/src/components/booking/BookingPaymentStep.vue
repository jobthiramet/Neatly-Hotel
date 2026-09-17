<!-- Figma: user > booking > payment method (desktop + mobile) -->
<script setup lang="ts">
import { IconCash, IconCreditCard } from '@/components/icons'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { PaymentOption } from '@/components/ui/payment-option'
import { RadioGroup } from '@/components/ui/radio-group'
import type { CheckoutPayment, CheckoutPaymentMethod, PaymentField } from '@/data/booking'

const payment = defineModel<CheckoutPayment>('payment', { required: true })

defineProps<{
  errors: Record<PaymentField, string>
}>()

const emit = defineEmits<{
  touch: [field: PaymentField]
}>()

function setMethod(value: unknown) {
  if (value === 'credit' || value === 'cash') {
    payment.value.method = value
    emit('touch', 'method')
  }
}

function formatCardNumber(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 19)
  return digits.replace(/(\d{4})(?=\d)/g, '$1 ')
}

function formatExpiry(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 4)
  if (digits.length <= 2)
    return digits
  return `${digits.slice(0, 2)}/${digits.slice(2)}`
}

function onCardNumberInput(value: string | number) {
  payment.value.cardNumber = formatCardNumber(String(value))
}

function onExpiryInput(value: string | number) {
  payment.value.expiry = formatExpiry(String(value))
}

function onCvcInput(value: string | number) {
  payment.value.cvc = String(value).replace(/\D/g, '').slice(0, 4)
}

function onPromotionCodeInput(value: string | number) {
  payment.value.promotionCode = String(value).toUpperCase()
}

const methodLabel: Record<CheckoutPaymentMethod, string> = {
  credit: 'Credit Card',
  cash: 'Cash',
}
</script>

<template>
  <div class="flex flex-col gap-10">
    <section aria-labelledby="payment-method-title" class="flex flex-col gap-6">
      <h2 id="payment-method-title" class="sr-only">
        Payment Method
      </h2>
      <RadioGroup
        :model-value="payment.method"
        class="grid grid-cols-2 gap-4"
        aria-label="Payment method"
        @update:model-value="setMethod"
      >
        <PaymentOption
          value="credit"
          class="h-12 w-full min-w-0 text-body1 shadow-none lg:h-14 [&_svg]:size-6"
        >
          <IconCreditCard /> {{ methodLabel.credit }}
        </PaymentOption>
        <PaymentOption
          value="cash"
          class="h-12 w-full min-w-0 text-body1 shadow-none lg:h-14 [&_svg]:size-6"
        >
          <IconCash /> {{ methodLabel.cash }}
        </PaymentOption>
      </RadioGroup>
      <p v-if="errors.method" id="payment-method-error" class="text-body2 font-normal tracking-normal text-red">
        {{ errors.method }}
      </p>
    </section>

    <div class="flex flex-col gap-6">
      <section
        v-if="payment.method === 'credit'"
        aria-labelledby="credit-card-title"
        class="flex flex-col gap-6"
      >
      <h2 id="credit-card-title" class="text-h5 text-gray-800">
        {{ methodLabel.credit }}
      </h2>

      <FormField label="Card Number" for="card-number" :error="errors.cardNumber">
        <Input
          id="card-number"
          :model-value="payment.cardNumber"
          inputmode="numeric"
          autocomplete="cc-number"
          placeholder="8888 8888 8888 8888"
          :aria-invalid="!!errors.cardNumber"
          aria-describedby="card-number-error"
          @update:model-value="onCardNumberInput"
          @blur="emit('touch', 'cardNumber')"
        />
      </FormField>

      <FormField label="Card Owner" for="card-owner" :error="errors.cardOwner">
        <Input
          id="card-owner"
          v-model="payment.cardOwner"
          autocomplete="cc-name"
          placeholder="Card owner"
          :aria-invalid="!!errors.cardOwner"
          aria-describedby="card-owner-error"
          @blur="emit('touch', 'cardOwner')"
        />
      </FormField>

      <div class="grid grid-cols-2 gap-4 lg:gap-6">
        <FormField label="Expiry Date" for="card-expiry" :error="errors.expiry">
          <Input
            id="card-expiry"
            :model-value="payment.expiry"
            inputmode="numeric"
            autocomplete="cc-exp"
            placeholder="MM/YY"
            :aria-invalid="!!errors.expiry"
            aria-describedby="card-expiry-error"
            @update:model-value="onExpiryInput"
            @blur="emit('touch', 'expiry')"
          />
        </FormField>
        <FormField label="CVC/CVV" for="card-cvc" :error="errors.cvc">
          <Input
            id="card-cvc"
            :model-value="payment.cvc"
            inputmode="numeric"
            autocomplete="cc-csc"
            placeholder="CVC"
            :aria-invalid="!!errors.cvc"
            aria-describedby="card-cvc-error"
            @update:model-value="onCvcInput"
            @blur="emit('touch', 'cvc')"
          />
        </FormField>
      </div>
    </section>

    <section
      v-else
      aria-labelledby="cash-title"
      class="flex flex-col gap-6"
    >
      <h2 id="cash-title" class="text-h5 text-gray-800">
        {{ methodLabel.cash }}
      </h2>
      <div class="flex items-start gap-4 rounded-sm bg-gray-200 p-4">
        <IconCash class="size-8 shrink-0 text-orange-500" aria-hidden="true" />
        <p class="text-body2 font-normal text-gray-700">
          Pay at the hotel with cash or cheque. No payment is required until you check in
        </p>
      </div>
    </section>

    <FormField
      label="Promotion Code"
      for="promotion-code"
      class="border-t border-gray-300 pt-6"
    >
      <Input
        id="promotion-code"
        :model-value="payment.promotionCode"
        autocomplete="off"
        spellcheck="false"
        placeholder="NEATLYNEW400"
        @update:model-value="onPromotionCodeInput"
      />
    </FormField>
    </div>
  </div>
</template>
