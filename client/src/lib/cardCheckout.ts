import type { StripeCheckoutElementsSdk, StripePaymentElement } from '@stripe/stripe-js'
import { loadStripe } from '@stripe/stripe-js'

export type CardRetryResult = { ok: true } | { ok: false, message: string }

type HeldCheckout = {
  checkout: StripeCheckoutElementsSdk
  element: StripePaymentElement
  host: HTMLDivElement
  parked: boolean
}

let held: HeldCheckout | null = null

export function attachCardCheckout(
  checkout: StripeCheckoutElementsSdk,
  element: StripePaymentElement,
  host: HTMLDivElement,
) {
  held = { checkout, element, host, parked: false }
}

export function isCardCheckoutParked() {
  return held?.parked === true
}

/**
 * Keep the mounted Payment Element alive after leaving the booking form.
 * Card details live in that element, so a later confirm can charge the same card
 * without asking the guest to type them again.
 */
export function parkCardCheckout() {
  if (!held)
    return
  held.parked = true
  const host = held.host
  host.style.position = 'fixed'
  host.style.left = '0'
  host.style.top = '0'
  host.style.width = '28rem'
  host.style.height = '16rem'
  host.style.transform = 'translateX(-120vw)'
  host.style.pointerEvents = 'none'
  host.setAttribute('aria-hidden', 'true')
  host.setAttribute('inert', '')
  document.body.appendChild(host)
}

export function releaseCardCheckout() {
  if (!held)
    return
  try {
    held.element.unmount()
  }
  catch {
    // The element can already be gone if Stripe tore the iframe down.
  }
  held.host.remove()
  held = null
}

export async function confirmParkedCardCheckout(): Promise<CardRetryResult> {
  const checkout = held?.checkout
  if (!checkout)
    return { ok: false, message: 'Card details are no longer available. Go back to payment details.' }
  return confirmCheckout(checkout)
}

export async function confirmOpenSession(clientSecret: string): Promise<CardRetryResult> {
  const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY
  if (!key || key.includes('YOUR_STRIPE'))
    return { ok: false, message: 'Card payments are not configured.' }
  const stripe = await loadStripe(key)
  if (!stripe)
    return { ok: false, message: 'Could not load Stripe.' }
  const checkout = stripe.initCheckoutElementsSdk({ clientSecret })
  const loaded = await checkout.loadActions()
  if (loaded.type === 'error')
    return { ok: false, message: loaded.error.message || 'Could not retry this payment.' }
  const session = loaded.actions.getSession()
  if (session.status.type === 'complete' && session.status.paymentStatus === 'paid')
    return { ok: true }
  const saved = session.savedPaymentMethods?.[0]?.id
  if (!saved && !session.canConfirm)
    return { ok: false, message: 'Card details are no longer available. Go back to payment details.' }
  const result = await loaded.actions.confirm({
    redirect: 'if_required',
    ...(saved ? { paymentMethod: saved } : {}),
  })
  if (result.type === 'error')
    return { ok: false, message: result.error.message || 'Payment failed.' }
  return { ok: true }
}

async function confirmCheckout(checkout: StripeCheckoutElementsSdk): Promise<CardRetryResult> {
  const loaded = await checkout.loadActions()
  if (loaded.type === 'error')
    return { ok: false, message: loaded.error.message || 'Could not retry this payment.' }
  const result = await loaded.actions.confirm({ redirect: 'if_required' })
  if (result.type === 'error')
    return { ok: false, message: result.error.message || 'Payment failed.' }
  return { ok: true }
}
