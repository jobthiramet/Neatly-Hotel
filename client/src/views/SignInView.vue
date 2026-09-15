<script setup lang="ts">
import { useSignIn } from '@clerk/vue'
import { isClerkAPIResponseError } from '@clerk/vue/errors'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import NeatlyLogo from '@/components/NeatlyLogo.vue'
import IconFacebook from '@/components/icons/IconFacebook.vue'
import IconGoogle from '@/components/icons/IconGoogle.vue'
import { navLinks } from '@/data/home'
import backgroundImage from '@/assets/auth/register-background.jpg'

const router = useRouter()
const { isLoaded, signIn, setActive } = useSignIn()
const identifier = ref('')
const password = ref('')
const secondFactorCode = ref('')
const secondFactorStrategy = ref<'phone_code' | 'email_code' | 'totp'>()
const resetCode = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const step = ref<'login' | 'second-factor' | 'forgot-identifier' | 'forgot-code' | 'forgot-password'>('login')
const loading = ref(false)
const touched = reactive({ identifier: false, password: false, secondFactorCode: false, resetCode: false, newPassword: false, confirmPassword: false })
const errors = reactive({ identifier: '', password: '', secondFactorCode: '', resetCode: '', newPassword: '', confirmPassword: '', form: '' })

type FieldName = keyof typeof touched

function validateField(field: FieldName) {
  if (!touched[field]) return
  const values = { identifier: identifier.value.trim(), password: password.value, secondFactorCode: secondFactorCode.value, resetCode: resetCode.value, newPassword: newPassword.value, confirmPassword: confirmPassword.value }
  const value = values[field]
  errors[field] = !value
    ? `${field === 'identifier' ? (step.value === 'login' ? 'Username or email' : 'Email') : field === 'resetCode' || field === 'secondFactorCode' ? 'Verification code' : field === 'confirmPassword' ? 'Confirm password' : 'Password'} is required.`
    : field === 'identifier' && value.includes('@') && !/^\S+@\S+\.\S+$/.test(value)
      ? 'Please enter a valid email address.'
      : (field === 'password' || field === 'newPassword') && value.length < 8
        ? 'Password must be at least 8 characters.'
        : field === 'confirmPassword' && value !== newPassword.value
          ? 'Passwords do not match.'
      : ''
}

function touchField(field: FieldName) {
  touched[field] = true
  validateField(field)
}

function validateFields(fields: FieldName[]) {
  fields.forEach(touchField)
  return fields.some(field => Boolean(errors[field]))
}

function showError(caught: unknown) {
  if (!isClerkAPIResponseError(caught)) {
    errors.form = 'Unable to log in. Please try again.'
    return
  }
  const clerkError = caught.errors[0]
  const message = clerkError?.longMessage ?? clerkError?.message ?? 'Unable to log in.'
  const metadata = clerkError as typeof clerkError & { meta?: { paramName?: string }; fields?: string[] }
  const field = `${metadata.meta?.paramName ?? metadata.fields?.[0] ?? ''}`.toLowerCase()
  if (field.includes('identifier') || field.includes('username') || field.includes('email')) errors.identifier = message
  else if (field.includes('password')) errors.password = message
  else errors.form = message
}

function clearErrors() {
  Object.keys(errors).forEach((field) => { errors[field as keyof typeof errors] = '' })
}

function resetTouched() {
  Object.keys(touched).forEach((field) => { touched[field as keyof typeof touched] = false })
}

function startForgotPassword() {
  step.value = 'forgot-identifier'
  clearErrors()
  resetTouched()
}

function backToLogin() {
  step.value = 'login'
  clearErrors()
  resetTouched()
}

async function login() {
  errors.form = ''
  if (validateFields(['identifier', 'password']) || !isLoaded.value || !signIn.value) return
  loading.value = true
  try {
    await signIn.value.create({ identifier: identifier.value.trim() })
    const result = await signIn.value.attemptFirstFactor({ strategy: 'password', password: password.value })
    if (result.status === 'complete' && setActive.value) {
      await setActive.value({ session: result.createdSessionId })
      await router.push('/')
    }
    else if (result.status === 'needs_second_factor' || result.status === 'needs_client_trust') {
      const factor = result.supportedSecondFactors?.find(({ strategy }) => strategy === 'email_code' || strategy === 'phone_code' || strategy === 'totp')
      if (!factor) {
        errors.form = 'No supported verification method is available for this account.'
        return
      }
      const strategy = factor.strategy === 'email_code' || factor.strategy === 'phone_code' ? factor.strategy : 'totp'
      secondFactorStrategy.value = strategy
      if (strategy === 'email_code' || strategy === 'phone_code') await signIn.value.prepareSecondFactor({ strategy })
      step.value = 'second-factor'
    }
    else errors.form = 'Additional verification is required to log in.'
  }
  catch (caught) {
    showError(caught)
  }
  finally {
    loading.value = false
  }
}

async function verifySecondFactor() {
  errors.form = ''
  if (validateFields(['secondFactorCode']) || !isLoaded.value || !signIn.value || !secondFactorStrategy.value) return
  loading.value = true
  try {
    const result = await signIn.value.attemptSecondFactor({ strategy: secondFactorStrategy.value, code: secondFactorCode.value })
    if (result.status === 'complete' && setActive.value) {
      await setActive.value({ session: result.createdSessionId })
      await router.push('/')
    }
    else errors.form = 'Verification could not be completed. Please try again.'
  }
  catch (caught) {
    showError(caught)
  }
  finally {
    loading.value = false
  }
}

async function loginWithSocial(strategy: 'oauth_google' | 'oauth_facebook') {
  if (!isLoaded.value || !signIn.value) return
  errors.form = ''
  loading.value = true
  try {
    await signIn.value.authenticateWithRedirect({
      strategy,
      redirectUrl: '/sso-callback',
      redirectUrlComplete: '/',
    })
  }
  catch (caught) {
    showError(caught)
    loading.value = false
  }
}

async function sendResetCode() {
  errors.form = ''
  if (validateFields(['identifier']) || !isLoaded.value || !signIn.value) return
  loading.value = true
  try {
    await signIn.value.create({ strategy: 'reset_password_email_code', identifier: identifier.value.trim() })
    step.value = 'forgot-code'
    errors.form = 'A verification code has been sent to your email.'
  }
  catch (caught) {
    showError(caught)
  }
  finally {
    loading.value = false
  }
}

async function verifyResetCode() {
  errors.form = ''
  if (validateFields(['resetCode']) || !isLoaded.value || !signIn.value) return
  loading.value = true
  try {
    await signIn.value.attemptFirstFactor({ strategy: 'reset_password_email_code', code: resetCode.value })
    step.value = 'forgot-password'
    clearErrors()
    resetTouched()
  }
  catch (caught) {
    showError(caught)
  }
  finally {
    loading.value = false
  }
}

async function resetPassword() {
  errors.form = ''
  if (validateFields(['newPassword', 'confirmPassword']) || !isLoaded.value || !signIn.value) return
  loading.value = true
  try {
    const result = await signIn.value.resetPassword({ password: newPassword.value })
    if (result.status === 'complete' && setActive.value) {
      await setActive.value({ session: result.createdSessionId })
      await router.push('/')
    }
    else errors.form = 'Password reset needs an additional verification step.'
  }
  catch (caught) {
    showError(caught)
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-bg">
    <header class="h-25 border-b border-gray-300 bg-white">
      <nav class="mx-auto flex h-full max-w-288 items-center px-4" aria-label="Main">
        <RouterLink to="/" class="rounded-sm outline-none is-focus:ring-2 is-focus:ring-ring"><NeatlyLogo class="h-11.25" /></RouterLink>
        <ul class="ml-17 hidden items-center gap-2 lg:flex">
          <li v-for="link in navLinks" :key="link.href">
            <a :href="`/${link.href}`" class="block rounded-sm px-4 py-6 text-body2 text-gray-900 outline-none is-hover:text-orange-500 is-focus:ring-2 is-focus:ring-ring">{{ link.label }}</a>
          </li>
        </ul>
        <RouterLink to="/sign-in" class="ml-auto rounded-sm px-4 py-6 text-body2 text-orange-500 outline-none is-focus:ring-2 is-focus:ring-ring">Log in</RouterLink>
      </nav>
    </header>

    <main class="grid min-h-screen lg:grid-cols-2">
      <div class="hidden bg-cover bg-center lg:block" :style="{ backgroundImage: `url(${backgroundImage})` }" role="img" aria-label="Hotel pool and lounge chairs" />
      <section class="flex items-start justify-center px-6 py-24 lg:px-20 lg:pt-41">
        <div class="w-full max-w-113">
          <template v-if="step === 'login'">
            <h1 class="font-serif text-h2 text-green-800">Log In</h1>
            <div class="mt-8 grid gap-3 sm:grid-cols-2">
              <Button type="button" variant="secondary" class="h-12 w-full gap-3" :disabled="loading || !isLoaded" @click="loginWithSocial('oauth_google')">
                <IconGoogle class="size-5" />
                Google
              </Button>
              <Button type="button" variant="secondary" class="h-12 w-full gap-3" :disabled="loading || !isLoaded" @click="loginWithSocial('oauth_facebook')">
                <IconFacebook class="size-5" />
                Facebook
              </Button>
            </div>
            <div class="mt-8 flex items-center gap-4 text-body3 text-gray-600" aria-hidden="true">
              <span class="h-px flex-1 bg-gray-300" />
              <span>or continue with email</span>
              <span class="h-px flex-1 bg-gray-300" />
            </div>
            <form class="mt-14" novalidate @submit.prevent="login">
            <FormField label="Username or Email" for="identifier" :error="errors.identifier">
              <Input id="identifier" v-model="identifier" autocomplete="username" placeholder="Enter your username or email" :aria-invalid="!!errors.identifier" aria-describedby="identifier-error" required @input="validateField('identifier')" @blur="touchField('identifier')" />
            </FormField>
            <FormField class="mt-10" label="Password" for="password" :error="errors.password">
              <Input id="password" v-model="password" type="password" autocomplete="current-password" placeholder="Enter your password" :aria-invalid="!!errors.password" aria-describedby="password-error" required @input="validateField('password')" @blur="touchField('password')" />
            </FormField>
            <button type="button" class="mt-3 block text-body2 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring" @click="startForgotPassword">Forgot password?</button>
            <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
            <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Logging in...' : 'Log In' }}</Button>
            </form>
            <p class="mt-4 text-body2 text-gray-700">Don’t have an account yet? <RouterLink to="/sign-up" class="ml-1 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring">Register</RouterLink></p>
          </template>

          <template v-else-if="step === 'second-factor'">
            <h1 class="font-serif text-h2 text-green-800">Additional Verification</h1>
            <p class="mt-6 text-body1 text-gray-700">{{ secondFactorStrategy === 'email_code' ? 'Enter the verification code sent to your email.' : secondFactorStrategy === 'phone_code' ? 'Enter the verification code sent to your phone.' : 'Enter the code from your authenticator app.' }}</p>
            <form class="mt-10" novalidate @submit.prevent="verifySecondFactor">
              <FormField label="Verification code" for="second-factor-code" :error="errors.secondFactorCode">
                <Input id="second-factor-code" v-model="secondFactorCode" inputmode="numeric" autocomplete="one-time-code" placeholder="Enter verification code" :aria-invalid="!!errors.secondFactorCode" aria-describedby="second-factor-code-error" required @input="validateField('secondFactorCode')" @blur="touchField('secondFactorCode')" />
              </FormField>
              <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
              <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Verifying...' : 'Verify code' }}</Button>
            </form>
            <button type="button" class="mt-4 text-body2 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring" @click="backToLogin">Back to Log In</button>
          </template>

          <template v-else-if="step === 'forgot-identifier'">
            <h1 class="font-serif text-h2 text-green-800">Forgot Password</h1>
            <p class="mt-6 text-body1 text-gray-700">Enter your username or email and we’ll send you a verification code.</p>
            <form class="mt-10" novalidate @submit.prevent="sendResetCode">
              <FormField label="Email" for="reset-email" :error="errors.identifier">
                <Input id="reset-email" v-model="identifier" type="email" autocomplete="email" placeholder="Enter your email" :aria-invalid="!!errors.identifier" aria-describedby="identifier-error" required @input="validateField('identifier')" @blur="touchField('identifier')" />
              </FormField>
              <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
              <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Sending...' : 'Send code' }}</Button>
            </form>
            <button type="button" class="mt-4 text-body2 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring" @click="backToLogin">Back to Log In</button>
          </template>

          <template v-else-if="step === 'forgot-code'">
            <h1 class="font-serif text-h2 text-green-800">Verify Email</h1>
            <p class="mt-6 text-body1 text-gray-700">Enter the verification code sent to your email.</p>
            <form class="mt-10" novalidate @submit.prevent="verifyResetCode">
              <FormField label="Verification code" for="reset-code" :error="errors.resetCode">
                <Input id="reset-code" v-model="resetCode" inputmode="numeric" autocomplete="one-time-code" placeholder="Enter verification code" :aria-invalid="!!errors.resetCode" aria-describedby="reset-code-error" required @input="validateField('resetCode')" @blur="touchField('resetCode')" />
              </FormField>
              <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
              <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Verifying...' : 'Verify code' }}</Button>
            </form>
            <button type="button" class="mt-4 text-body2 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring" @click="backToLogin">Back to Log In</button>
          </template>

          <template v-else>
            <h1 class="font-serif text-h2 text-green-800">Set New Password</h1>
            <form class="mt-10" novalidate @submit.prevent="resetPassword">
              <FormField label="New password" for="new-password" :error="errors.newPassword">
                <Input id="new-password" v-model="newPassword" type="password" autocomplete="new-password" placeholder="Enter your new password" :aria-invalid="!!errors.newPassword" aria-describedby="new-password-error" required @input="validateField('newPassword')" @blur="touchField('newPassword')" />
              </FormField>
              <FormField class="mt-10" label="Confirm password" for="confirm-password" :error="errors.confirmPassword">
                <Input id="confirm-password" v-model="confirmPassword" type="password" autocomplete="new-password" placeholder="Confirm your new password" :aria-invalid="!!errors.confirmPassword" aria-describedby="confirm-password-error" required @input="validateField('confirmPassword')" @blur="touchField('confirmPassword')" />
              </FormField>
              <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
              <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Saving...' : 'Set new password' }}</Button>
            </form>
          </template>
        </div>
      </section>
    </main>
  </div>
</template>
