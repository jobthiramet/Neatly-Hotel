import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getProfile, type Profile } from '@/api/profile'

/**
 * The signed-in user's profile from our API, fetched once per session.
 * The navbar and the profile page share it, so navigating never refetches.
 */
export const useProfileStore = defineStore('profile', () => {
  const profile = ref<Profile | null>(null)
  const loading = ref(false)
  /** Kept after a failure too, so a 401 or a dead network can't start a retry loop. */
  let request: Promise<Profile | null> | null = null

  /** The stored name, or '' when the profile has none yet. */
  const fullName = computed(() => {
    const parts = [profile.value?.firstName, profile.value?.lastName].filter(Boolean)
    return parts.join(' ')
  })

  function load() {
    request ??= (async () => {
      loading.value = true
      try {
        profile.value = await getProfile()
        return profile.value
      }
      catch {
        return null
      }
      finally {
        loading.value = false
      }
    })()
    return request
  }

  /** Called after a successful save so the navbar updates without a reload. */
  function set(next: Profile) {
    profile.value = next
    request = Promise.resolve(next)
  }

  /** Sign-out: drop the cached profile so the next user starts clean. */
  function reset() {
    profile.value = null
    request = null
  }

  return { profile, loading, fullName, load, set, reset }
})
