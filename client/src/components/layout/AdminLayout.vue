<script setup lang="ts">
import type { Component } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import {
  IconBooking,
  IconChartPie,
  IconChatText,
  IconCube,
  IconHotel,
  IconLogout,
  IconRoomManagement,
} from '@/components/icons'
import NeatlyLogo from '@/components/NeatlyLogo.vue'
import { MenuLink } from '@/components/ui/menu-link'

// To enable a tab: add its route under /admin, then set `routeName` here.
// Items without a routeName are owned by other tasks and render as inert placeholders.
const navItems: { label: string, icon: Component, routeName?: string }[] = [
  { label: 'Customer Booking', icon: IconBooking },
  { label: 'Room Management', icon: IconRoomManagement },
  { label: 'Hotel Information', icon: IconHotel, routeName: 'admin-hotel-information' },
  { label: 'Room & Property', icon: IconCube },
  { label: 'Analytics Dashboard', icon: IconChartPie },
  { label: 'Chatbot Setup', icon: IconChatText },
]

// Plain sidebar-row look, without MenuLink's hover/active states.
const placeholderClass = 'flex w-60 whitespace-nowrap cursor-default items-center gap-4 p-6 text-body1 font-medium text-green-300 [&_svg]:size-6 [&_svg]:shrink-0 [&_svg]:text-green-500'

const route = useRoute()
</script>

<!-- Figma: (admin) hotel information — sidebar + header shell shared by admin pages. -->
<template>
  <div class="flex min-h-screen bg-gray-100">
    <aside class="sticky top-0 flex h-screen w-60 shrink-0 flex-col overflow-y-auto bg-green-800">
      <div class="flex flex-col items-center gap-4 pt-6 pb-25">
        <NeatlyLogo variant="light" class="h-9" />
        <p class="text-body2 text-green-400">
          Admin Panel Control
        </p>
      </div>

      <nav aria-label="Admin">
        <ul>
          <li v-for="item in navItems" :key="item.label">
            <MenuLink v-if="item.routeName" as-child class="whitespace-nowrap" :active="route.name === item.routeName">
              <RouterLink :to="{ name: item.routeName }">
                <component :is="item.icon" /> {{ item.label }}
              </RouterLink>
            </MenuLink>
            <div v-else aria-disabled="true" :class="placeholderClass">
              <component :is="item.icon" /> {{ item.label }}
            </div>
          </li>
        </ul>
      </nav>

      <!-- Placeholder: log out isn't wired to auth yet. -->
      <div class="mt-auto border-t border-green-700 py-4">
        <div aria-disabled="true" :class="placeholderClass">
          <IconLogout /> Log Out
        </div>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex h-20 shrink-0 items-center justify-between gap-4 border-b border-gray-300 bg-white px-6 lg:px-15">
        <h1 class="text-h5 text-black">
          {{ route.meta.title }}
        </h1>
        <!-- Pages teleport their header actions (e.g. an Update button) here. -->
        <div id="admin-header-actions" class="flex items-center gap-4" />
      </header>
      <main class="flex-1 px-6 py-10 lg:px-15">
        <RouterView />
      </main>
    </div>
  </div>
</template>
