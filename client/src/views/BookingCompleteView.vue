<script setup lang="ts">
import { useAuth } from '@clerk/vue'
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBooking } from '@/api/bookings'

const route = useRoute()
const router = useRouter()
const { getToken } = useAuth()

onMounted(async () => {
  const bookingId = String(route.query.bookingId || '')
  if (!bookingId) {
    await router.replace({ name: 'booking-failed' })
    return
  }
  const token = await getToken.value?.()
  if (!token) {
    await router.replace({ path: '/sign-in', query: { redirect_url: route.fullPath } })
    return
  }
  try {
    const booking = await getBooking(token, bookingId)
    if (booking.status === 'CONFIRMED') {
      await router.replace({ name: 'booking-success', params: { bookingId } })
      return
    }
  }
  catch {
    // Fall through to the failed page.
  }
  await router.replace({ name: 'booking-failed', params: { bookingId } })
})
</script>

<template>
  <p class="p-10 text-center text-body1 text-gray-700">
    Confirming payment…
  </p>
</template>
