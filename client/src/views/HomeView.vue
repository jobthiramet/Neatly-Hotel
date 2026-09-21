<!-- Figma: home / non user (6:2 desktop, 7410:2846 mobile) -->
<script setup lang="ts">
import { useIntervalFn } from '@vueuse/core'
import { computed, onMounted, ref, useTemplateRef } from 'vue'
import { useRouter } from 'vue-router'
import heroImage from '@/assets/home/hero.webp'
import ChatbotWidget from '@/components/chatbot/ChatbotWidget.vue'
import { IconArrowRight } from '@/components/icons'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import RoomSearchForm, { type RoomSearchQuery, toSearchRouteQuery } from '@/components/RoomSearchForm.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel'
import { facilities, hotelPhotos, rooms, testimonials } from '@/data/home'
import { useSectionScroll } from '@/composables/useSectionScroll'
import { useHotelStore } from '@/stores/hotel'

// ── Desktop: one wheel gesture or key press glides to the next [data-scroll-section] ──
const navbar = useTemplateRef<{ $el: HTMLElement }>('navbar')
const { scrollToTop } = useSectionScroll(useTemplateRef('scrollRoot'), { offset: () => navbar.value?.$el.offsetHeight ?? 0 })

// ── Hero: search opens the Search Result page ──────────────────────────────
const router = useRouter()
function search(query: RoomSearchQuery) {
  router.push({ name: 'search', query: toSearchRouteQuery(query) })
}

// ── About: name and description are edited in admin / hotel information ───
const hotel = useHotelStore()
onMounted(hotel.fetch)
const aboutParagraphs = computed(() => hotel.description.split(/\n\s*\n/).filter(p => p.trim()))


// ── About: photo slider ────────────────────────────────────────────────────
// Embla's loop needs the slides to overflow the viewport; two copies of the
// 5 photos (10 × 416px) cover screens up to ~4000px wide.
const sliderPhotos = [...hotelPhotos, ...hotelPhotos]

// ── Rooms: mosaic layout, one entry per room in `rooms` order ──────────────
const roomLayout = [
  'md:col-span-12 lg:h-135',
  'md:col-span-7 lg:h-100',
  'md:col-span-5 lg:h-100',
  'md:col-span-5 md:row-span-2 md:h-auto',
  'md:col-span-7 lg:h-84.5',
  'md:col-span-7 lg:h-84.5',
]

// ── Testimonials: auto-slides, pauses while hovered or focused ─────────────
const activeTestimonial = ref(0)
const testimonial = computed(() => testimonials[activeTestimonial.value]!)

function showTestimonial(index: number) {
  activeTestimonial.value = (index + testimonials.length) % testimonials.length
}

const autoSlide = useIntervalFn(() => showTestimonial(activeTestimonial.value + 1), 5000)
</script>

<template>
  <SiteNavbar ref="navbar" @scroll-top="scrollToTop" />

  <div ref="scrollRoot">
  <main>
    <!-- Hero -->
    <section data-scroll-section aria-labelledby="hero-title" class="relative isolate flex h-191 flex-col items-center px-4 pt-25 md:h-auto md:min-h-screen-nav md:justify-center md:pt-0 md:pb-12">
      <img :src="heroImage" alt="" width="1920" height="1200" fetchpriority="high" class="absolute inset-0 -z-10 size-full object-cover">
      <div class="absolute inset-0 -z-10 bg-linear-to-b from-black/60 to-black/10" />
      <h1 id="hero-title" class="max-w-73 text-center font-serif text-h3 text-white md:max-w-150 lg:max-w-200 lg:text-h1">
        A Best Place for Your Neatly Experience
      </h1>
      <RoomSearchForm class="mt-12 w-full max-w-288 lg:mt-16" @search="search" />
    </section>

    <!-- About: one screen on desktop; photos shrink so the full description fits (a very long one grows the section) -->
    <section data-scroll-section id="about" aria-labelledby="about-title" class="bg-bg pt-10 pb-10 lg:pt-29 lg:pb-25 desktop:grid desktop:min-h-screen-nav desktop:grid-rows-fill desktop:py-10">
      <div class="mx-auto w-full max-w-288 px-4">
        <h2 id="about-title" class="font-serif text-h3 text-green-800 lg:text-h2">{{ hotel.name }}</h2>
        <div class="mt-10 lg:mt-13 lg:pl-48 desktop:mt-6 desktop:pl-0">
          <div class="flex flex-col gap-6 text-body1 text-gray-700">
            <p v-for="paragraph in aboutParagraphs" :key="paragraph">{{ paragraph }}</p>
          </div>
        </div>
      </div>

      <!-- Desktop: photos fill the space left in the screen, between their mobile and Figma sizes.
           They are positioned inside 4:5 slides so they never stretch the row themselves. -->
      <Carousel :opts="{ loop: true, align: 'center', startIndex: 2 }" aria-label="Hotel photos" class="mt-10 lg:mt-33.5 desktop:mt-8 desktop:min-h-56.25 desktop:*:data-[slot=carousel-content]:h-full">
        <CarouselContent class="-ml-2 lg:-ml-4 desktop:h-full desktop:items-center">
          <CarouselItem v-for="(photo, index) in sliderPhotos" :key="index" class="basis-auto pl-2 lg:pl-4 desktop:relative desktop:ml-4 desktop:aspect-4/5 desktop:h-full desktop:max-h-125 desktop:pl-0">
            <img :src="photo.src" :alt="photo.alt" width="400" height="500" loading="lazy" decoding="async" draggable="false" class="h-56.25 w-45 object-cover lg:h-125 lg:w-100 desktop:absolute desktop:inset-0 desktop:size-full">
          </CarouselItem>
        </CarouselContent>
        <CarouselPrevious aria-label="Previous image" class="lg:left-54" />
        <CarouselNext aria-label="Next image" class="lg:right-54" />
      </Carousel>
    </section>

    <!-- Service & Facilities: icon tabs on top, detail panel below -->
    <section data-scroll-section id="services" aria-labelledby="services-title" class="flex flex-col bg-green-700 px-4 pt-10 pb-10 lg:pt-25 lg:pb-30 desktop:min-h-screen-nav desktop:py-10">
      <h2 id="services-title" class="text-center font-serif text-h3 text-white lg:text-h2">Service & Facilities</h2>
      <Tabs :default-value="facilities[0]!.id" class="mx-auto mt-10 w-full max-w-288 flex-1 lg:mt-18 desktop:mt-8">
        <TabsList aria-label="Services and facilities" class="-mx-4 snap-x overflow-x-auto px-4 md:mx-0 md:flex-wrap md:justify-center md:overflow-visible md:px-0">
          <TabsTrigger v-for="facility in facilities" :key="facility.id" :value="facility.id" class="w-32 snap-start">
            <component :is="facility.icon" class="size-15" />
            {{ facility.label }}
          </TabsTrigger>
        </TabsList>

        <!-- Panels share one grid cell and cross-fade, so switching never shifts layout. -->
        <div class="grid flex-1">
          <TabsContent
            v-for="facility in facilities"
            :key="facility.id"
            :value="facility.id"
            force-mount
            class="col-start-1 row-start-1 grid gap-6 bg-green-800 p-4 transition-all duration-200 data-[state=inactive]:invisible data-[state=inactive]:opacity-0 motion-reduce:transition-none md:grid-cols-2 md:p-6 lg:gap-10"
          >
            <div class="relative aspect-3/2 overflow-hidden rounded-sm desktop:aspect-auto">
              <img :src="facility.image" :alt="facility.alt" width="1200" height="800" loading="lazy" decoding="async" class="absolute inset-0 size-full object-cover">
            </div>
            <div class="flex flex-col justify-center gap-4 text-white">
              <h3 class="font-serif text-h4 font-medium lg:text-h3">{{ facility.label }}</h3>
              <p class="text-body1 text-green-300">{{ facility.description }}</p>
            </div>
          </TabsContent>
        </div>
      </Tabs>
    </section>

    <!-- Rooms & Suits -->
    <section data-scroll-section id="rooms" aria-labelledby="rooms-title" class="relative bg-bg pt-10 pb-10 lg:pt-29 lg:pb-44.5">
      <h2 id="rooms-title" class="px-4 text-center font-serif text-h3 text-green-800 lg:text-h2">Rooms & Suits</h2>
      <ul class="mx-auto mt-10 grid max-w-288 grid-cols-1 gap-4 md:grid-cols-12 md:px-4 lg:mt-18 lg:gap-6">
        <li
          v-for="(room, index) in rooms"
          :key="room.id"
          :class="['group relative isolate h-62.5 overflow-hidden', roomLayout[index]]"
        >
          <img :src="room.image" :alt="room.alt" width="1600" height="1067" loading="lazy" decoding="async" class="absolute inset-0 -z-10 size-full object-cover transition-transform duration-500 group-hover:scale-105">
          <div class="absolute inset-0 -z-10 bg-linear-to-t from-black/50 to-black/0" />
          <div class="flex h-full flex-col justify-end p-6 lg:pr-6 lg:pb-20 lg:pl-15">
            <h3 class="font-serif text-h4 font-medium text-white lg:text-h3">{{ room.name }}</h3>
            <a :href="room.href" class="mt-2 inline-flex w-fit items-center gap-2 rounded-sm font-button text-button text-white outline-none is-hover:underline is-focus:ring-2 is-focus:ring-ring lg:mt-6">
              Explore Room <span class="sr-only">{{ room.name }}</span>
              <IconArrowRight class="size-4" />
            </a>
          </div>
        </li>
      </ul>
    </section>

    <!-- Our Customer Says -->
    <section data-scroll-section aria-labelledby="testimonials-title" class="bg-green-200 px-4 pt-10 pb-10 lg:pt-31 lg:pb-44.5 desktop:flex desktop:min-h-screen-nav desktop:flex-col desktop:justify-center desktop:py-10">
      <h2 id="testimonials-title" class="text-center font-serif text-h3 text-green-800 lg:text-h2">Our Customer Says</h2>

      <div
        class="mx-auto mt-10 flex max-w-270 flex-wrap items-center justify-center gap-x-8 lg:mt-18"
        aria-roledescription="carousel"
        @mouseenter="autoSlide.pause"
        @mouseleave="autoSlide.resume"
        @focusin="autoSlide.pause"
        @focusout="autoSlide.resume"
      >
        <figure aria-live="polite" class="flex w-full flex-col items-center lg:order-2 lg:w-auto lg:flex-1">
          <blockquote class="flex min-h-45 max-w-200 items-center text-center text-h5 text-green-700 lg:min-h-47">
            “{{ testimonial.quote }}”
          </blockquote>
          <figcaption class="mt-8 flex items-center gap-4 text-body1 text-gray-600">
            <img :src="testimonial.avatar" :alt="testimonial.avatarAlt" width="32" height="32" loading="lazy" class="size-8 rounded-full object-cover">
            {{ testimonial.author }}
          </figcaption>
        </figure>

        <div class="mt-8 flex w-full justify-center gap-4 lg:order-4">
          <button
            v-for="(item, index) in testimonials"
            :key="item.id"
            type="button"
            :aria-label="`Show testimonial ${index + 1}`"
            :aria-current="index === activeTestimonial"
            :class="['size-2 rounded-full outline-none is-focus:ring-2 is-focus:ring-ring', index === activeTestimonial ? 'bg-gray-700' : 'bg-gray-400']"
            @click="showTestimonial(index)"
          />
        </div>

        <button
          type="button"
          aria-label="Previous testimonial"
          class="mt-8 flex size-14 items-center justify-center rounded-full border border-orange-500 text-orange-500 outline-none is-hover:bg-orange-100 is-focus:ring-2 is-focus:ring-ring lg:order-1 lg:mt-16.5 lg:self-start"
          @click="showTestimonial(activeTestimonial - 1)"
        >
          <IconArrowRight class="size-6 rotate-180" />
        </button>
        <button
          type="button"
          aria-label="Next testimonial"
          class="mt-8 flex size-14 items-center justify-center rounded-full border border-orange-500 text-orange-500 outline-none is-hover:bg-orange-100 is-focus:ring-2 is-focus:ring-ring lg:order-3 lg:mt-16.5 lg:self-start"
          @click="showTestimonial(activeTestimonial + 1)"
        >
          <IconArrowRight class="size-6" />
        </button>
      </div>
    </section>
  </main>

  <SiteFooter data-scroll-section />
  </div>

  <ChatbotWidget />
</template>
