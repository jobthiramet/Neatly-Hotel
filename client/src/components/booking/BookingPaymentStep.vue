<!-- Figma: user > booking > payment method (desktop + mobile) -->
<script setup lang="ts">
import type { Stripe, StripeCheckoutElementsSdk, StripePaymentElement } from '@stripe/stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { IconCash, IconCreditCard } from '@/components/icons'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { PaymentOption } from '@/components/ui/payment-option'
import { RadioGroup } from '@/components/ui/radio-group'
import type { CheckoutPayment, CheckoutPaymentMethod } from '@/data/booking'

const payment = defineModel<CheckoutPayment>('payment', { required: true })

const props = defineProps<{
  clientSecret: string | null
  stripeError: string
}>()

const elementHost = ref<HTMLElement | null>(null)
let stripePromise: Promise<Stripe | null> | null = null
let checkout: StripeCheckoutElementsSdk | null = null
let paymentElement: StripePaymentElement | null = null
const elementReady = ref(false)

function token(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function stripeInstance() {
  const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY
  if (!key || key.includes('YOUR_STRIPE'))
    return Promise.resolve(null)
  stripePromise ??= loadStripe(key)
  return stripePromise
}

async function mountElement(secret: string) {
  unmountElement()
  elementReady.value = false
  await nextTick()
  if (!elementHost.value)
    return
  const stripe = await stripeInstance()
  if (!stripe) {
    return
  }
  checkout = stripe.initCheckoutElementsSdk({
    clientSecret: secret,
    elementsOptions: {
      appearance: {
        theme: 'stripe',
        variables: {
          colorPrimary: token('--color-orange-600'),
          colorBackground: token('--color-white'),
          colorText: token('--color-gray-800'),
          colorDanger: token('--color-red'),
          fontFamily: token('--font-sans'),
          borderRadius: '4px',
        },
      },
    },
  })
  paymentElement = checkout.createPaymentElement({ layout: 'tabs' })
  paymentElement.mount(elementHost.value)
  elementReady.value = true
}

function unmountElement() {
  paymentElement?.unmount()
  paymentElement = null
  checkout = null
  elementReady.value = false
}

watch(
  () => [payment.value.method, props.clientSecret] as const,
  async ([method, secret]) => {
    if (method !== 'credit' || !secret) {
      unmountElement()
      return
    }
    await mountElement(secret)
  },
  { immediate: true },
)

onBeforeUnmount(unmountElement)

async function confirmCard(beforeConfirm?: () => Promise<void>) {
  if (!checkout)
    return { ok: false as const, recoverable: true, message: 'Card form is not ready yet.' }
  const loaded = await checkout.loadActions()
  if (loaded.type === 'error')
    return { ok: false as const, message: loaded.error.message || 'Could not confirm payment.' }
  if (beforeConfirm) {
    try {
      const updated = await loaded.actions.runServerUpdate(beforeConfirm)
      if (updated.type === 'error')
        return { ok: false as const, recoverable: true, message: updated.error.message || 'Could not apply the promotion code.' }
    }
    catch (error) {
      const message = error instanceof Error ? error.message : 'Could not apply the promotion code.'
      return { ok: false as const, recoverable: true, message }
    }
  }
  const result = await loaded.actions.confirm({ redirect: 'if_required' })
  if (result.type === 'error')
    return { ok: false as const, message: result.error.message || 'Payment failed.' }
  return { ok: true as const }
}

defineExpose({ confirmCard, elementReady })

function setMethod(value: unknown) {
  if (value === 'credit' || value === 'cash')
    payment.value.method = value
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
        <div
          id="payment-element"
          ref="elementHost"
          class="min-h-20"
        />
        <p v-if="stripeError" class="text-body2 font-normal tracking-normal text-red" role="alert">
          {{ stripeError }}
        </p>
        <p
          v-else-if="!clientSecret"
          class="text-body2 font-normal text-gray-600"
        >
          Preparing Stripe’s form…
        </p>
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
