<!-- Figma: footer (13228:5083 desktop, 7410:4126 mobile) -->
<script setup lang="ts">
import { IconFacebook, IconInstagram, IconMail, IconMapPin, IconPhone, IconTwitter } from '@/components/icons'
import NeatlyLogo from '@/components/NeatlyLogo.vue'
import { contact, socialLinks } from '@/data/home'

const socials = [
  { label: 'Facebook', href: socialLinks.facebook, icon: IconFacebook },
  { label: 'Instagram', href: socialLinks.instagram, icon: IconInstagram },
  { label: 'Twitter', href: socialLinks.twitter, icon: IconTwitter },
]
</script>

<template>
  <footer class="bg-green-800 text-white">
    <div class="mx-auto max-w-288 px-4 pt-6 pb-10 lg:pt-15">
      <div class="flex flex-col gap-10 lg:flex-row lg:justify-between">
        <div>
          <NeatlyLogo variant="light" />
          <p class="mt-6 text-h5 lg:mt-10">Neatly Hotel</p>
          <p class="mt-2 text-body1">The best hotel for rising your experience</p>
        </div>

        <address class="not-italic lg:w-86 lg:pt-4">
          <p class="text-body1">CONTACT</p>
          <ul class="mt-6 flex flex-col gap-6 text-body1 lg:mt-10">
            <li class="flex gap-4">
              <IconPhone class="size-5 shrink-0 text-green-400" />
              <a :href="`tel:${contact.phone.replaceAll(' ', '')}`" class="rounded-sm outline-none is-hover:underline is-focus:ring-2 is-focus:ring-ring">{{ contact.phone }}</a>
            </li>
            <li class="flex gap-4">
              <IconMail class="size-5 shrink-0 text-green-400" />
              <a :href="`mailto:${contact.email}`" class="rounded-sm outline-none is-hover:underline is-focus:ring-2 is-focus:ring-ring">{{ contact.email }}</a>
            </li>
            <li class="flex gap-4">
              <IconMapPin class="size-5 shrink-0 text-green-400" />
              <span>{{ contact.address }}</span>
            </li>
          </ul>
        </address>
      </div>

      <div class="mt-8 flex items-center justify-between border-t border-green-700 pt-6 lg:mt-30">
        <ul class="flex gap-2">
          <li v-for="social in socials" :key="social.label">
            <a :href="social.href" :aria-label="social.label" class="block rounded-sm p-1 outline-none is-hover:text-green-300 is-focus:ring-2 is-focus:ring-ring">
              <component :is="social.icon" class="size-5" />
            </a>
          </li>
        </ul>
        <p class="text-body1">Copyright ©2022 Neatly Hotel</p>
      </div>
    </div>
  </footer>
</template>
