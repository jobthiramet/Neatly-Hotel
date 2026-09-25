<!-- Figma: nav bar / user (28:1990) + drop down - user (28:2286) -->
<script setup lang="ts">
import { useClerk, useUser } from '@clerk/vue'
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { IconLogout, IconUser } from '@/components/icons'
import { useProfileStore } from '@/stores/profile'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

const { user } = useUser()
const clerk = useClerk()
const route = useRoute()
const profileStore = useProfileStore()

// One request per session, once Clerk has restored the session.
watch(() => user.value?.id, (id) => {
  if (id)
    profileStore.load()
}, { immediate: true })

const open = ref(false)
watch(() => route.fullPath, () => { open.value = false })

// Our database owns the name. Clerk's name is the fallback; the email never is,
// because "user1+clerk_test" is not a name.
const name = computed(() => profileStore.fullName
  || [user.value?.firstName, user.value?.lastName].filter(Boolean).join(' '))

/** No name yet: the avatar stands alone rather than showing a placeholder. */
const initials = computed(() => name.value
  .split(/\s+/)
  .filter(Boolean)
  .slice(0, 2)
  .map(part => part[0]!.toUpperCase())
  .join(''))

// Only while the first request is in flight, so the name never pops in after an email.
const nameLoading = computed(() => profileStore.loading && !name.value)

const avatarUrl = computed(() => user.value?.hasImage ? user.value.imageUrl : null)

async function signOut() {
  profileStore.reset()
  await clerk.value?.signOut({ redirectUrl: '/' })
}
</script>

<template>
  <DropdownMenu v-model:open="open">
    <DropdownMenuTrigger as-child>
      <button
        type="button"
        class="flex cursor-pointer items-center gap-3 rounded-sm outline-none is-focus:ring-2 is-focus:ring-ring is-focus:ring-offset-2"
      >
        <img
          v-if="avatarUrl"
          :src="avatarUrl"
          alt=""
          width="40"
          height="40"
          class="size-10 shrink-0 rounded-full object-cover"
        >
        <span
          v-else
          aria-hidden="true"
          class="flex size-10 shrink-0 items-center justify-center rounded-full bg-gray-300 text-gray-700"
        >
          <span v-if="initials" class="text-body2">{{ initials }}</span>
          <IconUser v-else class="size-5" />
        </span>
        <span v-if="nameLoading" aria-hidden="true" class="h-4 w-24 animate-pulse rounded-sm bg-gray-300" />
        <span v-else-if="name" class="max-w-32 truncate text-body1 text-gray-700">{{ name }}</span>
        <span class="sr-only">Account menu</span>
      </button>
    </DropdownMenuTrigger>

    <DropdownMenuContent>
      <DropdownMenuItem as-child>
        <RouterLink to="/profile">
          <IconUser />
          Profile
        </RouterLink>
      </DropdownMenuItem>

      <DropdownMenuSeparator />

      <DropdownMenuItem @select="signOut">
        <IconLogout />
        Log out
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
