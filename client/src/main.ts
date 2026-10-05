import { clerkPlugin } from '@clerk/vue';
import { shadcn } from '@clerk/ui/themes'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const PUBLISHABLE_KEY = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;
if (!PUBLISHABLE_KEY) {
  throw new Error("Missing VITE_CLERK_PUBLISHABLE_KEY. Add your key to .env.local.\nRun: 1) clerk auth login  2) clerk link  3) clerk env pull — then restart the dev server.");
}

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.use(clerkPlugin, {
  publishableKey: PUBLISHABLE_KEY,
  signInUrl: '/sign-in',
  signUpUrl: '/sign-up',
  signInFallbackRedirectUrl: '/',
  signUpFallbackRedirectUrl: '/',
  appearance: { theme: shadcn },
});
app.mount('#app')
