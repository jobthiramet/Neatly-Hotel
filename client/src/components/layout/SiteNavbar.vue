<!-- Figma: nav bar / non user (12:20 desktop, 7410:3826 mobile) + hambergur menu (7431:4779) -->
<script setup lang="ts">
import { Show, UserButton } from '@clerk/vue'
import { RouterLink } from 'vue-router'
import { Button } from '@/components/ui/button'
import { onKeyStroke } from '@vueuse/core'
import { ref, useTemplateRef } from 'vue'
import { IconClose, IconMenu } from '@/components/icons'
import NeatlyLogo from '@/components/NeatlyLogo.vue'
import { navLinks } from '@/data/home'

const menuOpen = ref(false)
const menuButton = useTemplateRef('menuButton')

function closeMenu() {
  if (!menuOpen.value) return
  menuOpen.value = false
  menuButton.value?.focus()
}

onKeyStroke('Escape', closeMenu)
</script>

<template>
  <header class="relative z-40 bg-white">
    <nav aria-label="Main" class="mx-auto flex h-12 max-w-288 items-center justify-between px-4 lg:h-25">
      <a href="/" class="rounded-sm outline-none is-focus:ring-2 is-focus:ring-ring">
        <NeatlyLogo class="h-6 lg:h-11.25" />
      </a>

      <ul class="ml-12 hidden flex-1 lg:flex">
        <li v-for="link in navLinks" :key="link.href">
          <a :href="link.href" class="block rounded-sm px-4 py-6 text-body2 text-gray-900 outline-none is-hover:text-orange-500 is-focus:ring-2 is-focus:ring-ring">
            {{ link.label }}
          </a>
        </li>
      </ul>

      <div class="hidden items-center gap-3 lg:flex">
        <Show when="signed-out">
          <Button as-child variant="ghost"><RouterLink to="/sign-in">Log in</RouterLink></Button>
          <Button as-child><RouterLink to="/sign-up">Sign up</RouterLink></Button>
        </Show>
        <Show when="signed-in"><UserButton /></Show>
      </div>

      <button
        ref="menuButton"
        type="button"
        class="rounded-sm p-1 text-gray-700 outline-none is-focus:ring-2 is-focus:ring-ring lg:hidden"
        :aria-expanded="menuOpen"
        aria-controls="mobile-menu"
        :aria-label="menuOpen ? 'Close menu' : 'Open menu'"
        @click="menuOpen = !menuOpen"
      >
        <IconClose v-if="menuOpen" class="size-6" />
        <IconMenu v-else class="size-6" />
      </button>
    </nav>

    <div
      v-show="menuOpen"
      id="mobile-menu"
      class="fixed inset-x-0 top-12 bottom-0 overflow-y-auto border-t border-gray-300 bg-white px-4 pt-6 lg:hidden"
    >
      <ul>
        <li v-for="link in navLinks" :key="link.href">
          <a :href="link.href" class="block rounded-sm px-4 py-6 text-body2 text-gray-900 outline-none is-focus:ring-2 is-focus:ring-ring" @click="closeMenu">
            {{ link.label }}
          </a>
        </li>
      </ul>
      <hr class="my-4 border-gray-300">
      <div class="flex items-center gap-3">
        <Show when="signed-out">
          <Button as-child variant="ghost"><RouterLink to="/sign-in" @click="closeMenu">Log in</RouterLink></Button>
          <Button as-child><RouterLink to="/sign-up" @click="closeMenu">Sign up</RouterLink></Button>
        </Show>
        <Show when="signed-in"><UserButton /></Show>
      </div>
    </div>
  </header>
</template>
