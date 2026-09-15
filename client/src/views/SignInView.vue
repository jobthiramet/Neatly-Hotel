<script setup lang="ts">
import { useSignIn } from '@clerk/vue'
import { isClerkAPIResponseError } from '@clerk/vue/errors'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import NeatlyLogo from '@/components/NeatlyLogo.vue'
import { navLinks } from '@/data/home'
import backgroundImage from '@/assets/auth/register-background.jpg'

const router = useRouter()
const { isLoaded, signIn, setActive } = useSignIn()
const identifier = ref('')
const password = ref('')
const loading = ref(false)
const touched = reactive({ identifier: false, password: false })
const errors = reactive({ identifier: '', password: '', form: '' })

function validateField(field: 'identifier' | 'password') {
  if (!touched[field]) return
  const value = field === 'identifier' ? identifier.value.trim() : password.value
  errors[field] = !value
    ? `${field === 'identifier' ? 'Username or email' : 'Password'} is required.`
    : field === 'identifier' && value.includes('@') && !/^\S+@\S+\.\S+$/.test(value)
      ? 'Please enter a valid email address.'
      : ''
}

function touchField(field: 'identifier' | 'password') {
  touched[field] = true
  validateField(field)
}

function validateAll() {
  touchField('identifier')
  touchField('password')
  return Boolean(errors.identifier || errors.password)
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

async function login() {
  errors.form = ''
  if (validateAll() || !isLoaded.value || !signIn.value) return
  loading.value = true
  try {
    await signIn.value.create({ identifier: identifier.value.trim() })
    const result = await signIn.value.attemptFirstFactor({ strategy: 'password', password: password.value })
    if (result.status === 'complete' && setActive.value) {
      await setActive.value({ session: result.createdSessionId })
      await router.push('/')
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
          <h1 class="font-serif text-h2 text-green-800">Log In</h1>
          <form class="mt-14" novalidate @submit.prevent="login">
            <FormField label="Username or Email" for="identifier" :error="errors.identifier">
              <Input id="identifier" v-model="identifier" autocomplete="username" placeholder="Enter your username or email" :aria-invalid="!!errors.identifier" aria-describedby="identifier-error" required @input="validateField('identifier')" @blur="touchField('identifier')" />
            </FormField>
            <FormField class="mt-10" label="Password" for="password" :error="errors.password">
              <Input id="password" v-model="password" type="password" autocomplete="current-password" placeholder="Enter your password" :aria-invalid="!!errors.password" aria-describedby="password-error" required @input="validateField('password')" @blur="touchField('password')" />
            </FormField>
            <p v-if="errors.form" role="alert" class="mt-4 text-body2 text-red">{{ errors.form }}</p>
            <Button type="submit" class="mt-10 h-12 w-full" :disabled="loading || !isLoaded">{{ loading ? 'Logging in...' : 'Log In' }}</Button>
          </form>
          <p class="mt-4 text-body2 text-gray-700">Don’t have an account yet? <RouterLink to="/sign-up" class="ml-1 text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring">Register</RouterLink></p>
        </div>
      </section>
    </main>
  </div>
</template>
