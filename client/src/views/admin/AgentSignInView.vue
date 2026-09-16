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
const loading = ref(false)
const errors = reactive({ username: '', password: '', form: '' })

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
    if (result.status !== 'complete' || !result.createdSessionId) {
      errors.form = 'This account requires additional verification. Please contact your administrator.'
      return
    }
    await setActive.value({ session: result.createdSessionId })
    password.value = ''
    await router.replace('/admin')
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
      </div>
    </section>
  </main>
</template>
