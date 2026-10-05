<script setup lang="ts">
import { useSignIn, useUser } from '@clerk/vue'
import { isClerkAPIResponseError } from '@clerk/vue/errors'
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import backgroundImage from '@/assets/auth/register-background.jpg'

defineProps<{ forbidden?: boolean }>()
const router = useRouter()
const { isLoaded, signIn, setActive } = useSignIn()
const { user, isLoaded: userLoaded } = useUser()
const username = ref('')
const password = ref('')
const verificationCode = ref('')
const verificationStrategy = ref<'email_code' | 'phone_code' | 'totp'>()
const step = ref<'login' | 'verification'>('login')
const loading = ref(false)
const errors = reactive({ username: '', password: '', verificationCode: '', form: '' })

// Recheck access if a session changes while this page is open.
watch([userLoaded, user], ([loaded, current]) => {
  if (loaded && current) void router.replace('/admin')
})

async function login() {
  if (loading.value || !isLoaded.value || !signIn.value || !setActive.value) return
  errors.username = username.value.trim() ? '' : 'Username is required.'
  errors.password = password.value ? '' : 'Password is required.'
  errors.form = ''
  if (errors.username || errors.password) return
  loading.value = true
  try {
    await signIn.value.create({ identifier: username.value.trim() })
    const result = await signIn.value.attemptFirstFactor({ strategy: 'password', password: password.value })
    if (result.status === 'complete' && result.createdSessionId) {
      await setActive.value({ session: result.createdSessionId })
      password.value = ''
      await router.replace('/admin')
    }
    else if (result.status === 'needs_second_factor' || result.status === 'needs_client_trust') {
      const factor = result.supportedSecondFactors?.find(({ strategy }) => strategy === 'email_code' || strategy === 'phone_code' || strategy === 'totp')
      if (!factor) {
        errors.form = 'No supported verification method is available for this account.'
        return
      }
      const strategy = factor.strategy === 'email_code' || factor.strategy === 'phone_code' ? factor.strategy : 'totp'
      verificationStrategy.value = strategy
      if (strategy === 'email_code' || strategy === 'phone_code') await signIn.value.prepareSecondFactor({ strategy })
      step.value = 'verification'
    }
    else errors.form = 'Additional verification is required to log in.'
  }
  catch (caught) {
    errors.form = isClerkAPIResponseError(caught)
      ? caught.errors[0]?.longMessage ?? caught.errors[0]?.message ?? 'Unable to log in.'
      : 'Unable to log in. Please try again.'
  }
  finally {
    loading.value = false
  }
}

async function verify() {
  errors.verificationCode = verificationCode.value.trim() ? '' : 'Verification code is required.'
  errors.form = ''
  if (errors.verificationCode || loading.value || !isLoaded.value || !signIn.value || !setActive.value || !verificationStrategy.value) return
  loading.value = true
  try {
    const result = await signIn.value.attemptSecondFactor({ strategy: verificationStrategy.value, code: verificationCode.value.trim() })
    if (result.status === 'complete' && result.createdSessionId) {
      await setActive.value({ session: result.createdSessionId })
      password.value = ''
      verificationCode.value = ''
      await router.replace('/admin')
    }
    else errors.form = 'Verification could not be completed. Please try again.'
  }
  catch (caught) {
    errors.form = isClerkAPIResponseError(caught)
      ? caught.errors[0]?.longMessage ?? caught.errors[0]?.message ?? 'Unable to verify the code.'
      : 'Unable to verify the code. Please try again.'
  }
  finally {
    loading.value = false
  }
}

function backToLogin() {
  step.value = 'login'
  verificationCode.value = ''
  verificationStrategy.value = undefined
  errors.verificationCode = ''
  errors.form = ''
}
</script>

<template>
  <main class="grid min-h-screen bg-bg lg:grid-cols-2">
    <div class="hidden bg-cover bg-center lg:block" :style="{ backgroundImage: `url(${backgroundImage})` }" role="img" aria-label="Hotel pool and lounge chairs" />
    <section class="flex items-start justify-center px-6 py-24 lg:px-20 lg:pt-41">
      <div class="w-full max-w-113">
        <template v-if="forbidden">
          <h1 class="font-serif text-h2 text-green-800">Forbidden</h1>
          <p role="alert" class="mt-8 text-body1 text-gray-900">Only accounts with the agent role can access this section.</p>
          <Button as-child class="mt-10"><RouterLink to="/">Back to home</RouterLink></Button>
        </template>
        <template v-else>
          <template v-if="step === 'login'">
            <h1 class="font-serif text-h2 text-green-800">Agent Log In</h1>
            <form class="mt-14" novalidate @submit.prevent="login">
              <FormField label="Username" for="agent-username" :error="errors.username">
                <Input id="agent-username" v-model="username" autocomplete="username" placeholder="Enter your username" :aria-invalid="!!errors.username" aria-describedby="agent-username-error" required />
              </FormField>
              <FormField class="mt-10" label="Password" for="agent-password" :error="errors.password">
                <Input id="agent-password" v-model="password" type="password" autocomplete="current-password" placeholder="Enter your password" :aria-invalid="!!errors.password" aria-describedby="agent-password-error" required />
              </FormField>
              <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
              <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Logging in...' : 'Log In' }}</Button>
            </form>
          </template>
          <template v-else>
            <h1 class="font-serif text-h2 text-green-800">Additional Verification</h1>
            <p class="mt-6 text-body1 text-gray-700">{{ verificationStrategy === 'email_code' ? 'Enter the verification code sent to your email.' : verificationStrategy === 'phone_code' ? 'Enter the verification code sent to your phone.' : 'Enter the code from your authenticator app.' }}</p>
            <form class="mt-10" novalidate @submit.prevent="verify">
              <FormField label="Verification code" for="agent-verification-code" :error="errors.verificationCode">
                <Input id="agent-verification-code" v-model="verificationCode" inputmode="numeric" autocomplete="one-time-code" placeholder="Enter verification code" :aria-invalid="!!errors.verificationCode" aria-describedby="agent-verification-code-error" required />
              </FormField>
              <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
              <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Verifying...' : 'Verify code' }}</Button>
            </form>
            <button type="button" class="mt-4 text-body2 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring" @click="backToLogin">Back to Log In</button>
          </template>
        </template>
      </div>
    </section>
  </main>
</template>
