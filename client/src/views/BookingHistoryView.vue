<!-- Figma: user > booking history (node 7415:8200 desktop, mobile > user > booking) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { useAuth } from '@clerk/vue'
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { IconCaretDown, IconChevronRight } from '@/components/icons'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import SiteNavbar from '@/components/layout/SiteNavbar.vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import type { UserBooking } from '@/data/booking'
import { defaultRoomId, roomDetails } from '@/data/rooms'
import { useBookingStore } from '@/stores/booking'

const router = useRouter()
const route = useRoute()
const { getToken, isLoaded } = useAuth()
const bookingStore = useBookingStore()

const formatter = new DateFormatter('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

interface FormattableDate {
  toDate: (timeZone: string) => Date
}

function formatDate(value: FormattableDate | DateValue | undefined | null) {
  if (!value) return ''
  return formatter.format(value.toDate(getLocalTimeZone()))
}

function formatMoney(amount: number) {
  const formatted = Math.abs(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
  return amount < 0 ? `-${formatted}` : formatted
}

function roomDetailLink(roomName: string) {
  const match = Object.entries(roomDetails).find(([, room]) => room.name === roomName)
  return { name: 'room-detail', params: { roomId: match?.[0] ?? defaultRoomId } }
}

const expandedBookingIds = ref<Record<string, boolean>>({})

function toggleAccordion(id: string) {
  expandedBookingIds.value[id] = !expandedBookingIds.value[id]
}

// Cancel booking dialog states
const cancelDialogOpen = ref(false)
const selectedBooking = ref<UserBooking | null>(null)

function openCancelDialog(booking: UserBooking) {
  selectedBooking.value = booking
  cancelDialogOpen.value = true
}

function proceedToCancel() {
  if (!selectedBooking.value) return
  const id = selectedBooking.value.id
  const isNoRefund = selectedBooking.value.status === 'checkin-soon'
  cancelDialogOpen.value = false

  if (isNoRefund) {
    router.push({ name: 'booking-cancel', params: { bookingId: id } })
  }
  else {
    router.push({ name: 'booking-refund', params: { bookingId: id } })
  }
}

// Pagination state
const currentPage = ref(1)
const itemsPerPage = ref(4)
const totalPages = computed(() =>
  Math.max(1, Math.ceil(bookingStore.bookings.length / itemsPerPage.value)),
)
const paginatedBookings = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value
  return bookingStore.bookings.slice(start, start + itemsPerPage.value)
})

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

async function focusBooking() {
  const id = typeof route.query.bookingId === 'string' ? route.query.bookingId : ''
  if (!id)
    return
  const index = bookingStore.bookings.findIndex(booking => booking.id === id)
  if (index < 0)
    return
  currentPage.value = Math.floor(index / itemsPerPage.value) + 1
  expandedBookingIds.value = { ...expandedBookingIds.value, [id]: true }
  await nextTick()
  document.getElementById(`booking-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadHistory() {
  const tokenFn = getToken.value
  const token = typeof tokenFn === 'function' ? await tokenFn() : null
  if (!token)
    return
  await bookingStore.loadMine(token)
  await focusBooking()
}

watch(isLoaded, (ready) => {
  if (ready)
    void loadHistory()
}, { immediate: true })
</script>

<template>
  <div class="flex min-h-screen flex-col bg-bg">
    <SiteNavbar />

    <main class="flex-1">
      <section
        aria-labelledby="booking-history-title"
        class="mx-auto max-w-280 px-4 pt-10 pb-14 lg:pt-20 lg:pb-32"
      >
        <h1
          id="booking-history-title"
          class="font-serif text-h3 text-green-800 lg:text-h2 lg:text-green-700"
        >
          Booking History
        </h1>

        <p
          v-if="bookingStore.loading"
          class="mt-10 text-body1 text-gray-700"
          role="status"
        >
          Loading bookings…
        </p>
        <p
          v-else-if="bookingStore.error"
          class="mt-10 text-body1 text-red"
          role="alert"
        >
          {{ bookingStore.error }}
        </p>
        <p
          v-else-if="bookingStore.bookings.length === 0"
          class="mt-10 text-body1 text-gray-700"
        >
          You have no bookings yet.
        </p>

        <!-- Booking Cards List -->
        <div v-else class="mt-6 flex flex-col lg:mt-12">
          <article
            v-for="booking in paginatedBookings"
            :key="booking.id"
            :id="`booking-${booking.id}`"
            class="-mx-4 scroll-mt-24 border-b border-gray-300 py-6 lg:mx-0 lg:py-10"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:gap-12">
              <!-- Room Thumbnail -->
              <img
                :src="booking.roomImage"
                :alt="booking.roomImageAlt"
                width="1600"
                height="1067"
                class="h-55.25 w-full rounded-sm object-cover lg:h-52.5 lg:w-89.25 lg:shrink-0"
              >

              <!-- Booking Information -->
              <div class="flex min-w-0 flex-1 flex-col gap-6 px-4 lg:px-0">
                <!-- Header: Room Name & Booking Date / Cancellation Date -->
                <header class="flex flex-col gap-1 lg:flex-row lg:items-center lg:justify-between">
                  <h2 class="text-h4 text-black font-semibold">
                    {{ booking.roomName }}
                  </h2>
                  <div class="text-left text-body1 text-gray-600 lg:text-right">
                    <p>Booking date: {{ formatDate(booking.bookedAt) }}</p>
                    <p v-if="booking.cancellationDate">
                      Cancellation date: {{ formatDate(booking.cancellationDate) }}
                    </p>
                  </div>
                </header>

                <!-- Check-in & Check-out Timings -->
                <div class="flex flex-col gap-4 sm:flex-row sm:gap-12">
                  <div>
                    <h3 class="text-body1 font-semibold text-gray-800">
                      Check-in
                    </h3>
                    <p class="mt-1 text-body1 text-gray-700">
                      {{ formatDate(booking.checkIn.date) }} &nbsp;|&nbsp; {{ booking.checkIn.timeText }}
                    </p>
                  </div>

                  <div>
                    <h3 class="text-body1 font-semibold text-gray-800">
                      Check-out
                    </h3>
                    <p class="mt-1 text-body1 text-gray-700">
                      {{ formatDate(booking.checkOut.date) }} &nbsp;|&nbsp; {{ booking.checkOut.timeText }}
                    </p>
                  </div>
                </div>

                <!-- Collapsible Booking Detail Accordion -->
                <div class="overflow-hidden rounded-sm bg-gray-100">
                  <button
                    type="button"
                    class="flex w-full cursor-pointer items-center justify-between p-4 text-left outline-none transition-colors is-hover:bg-gray-200 is-focus:ring-2 is-focus:ring-ring"
                    :aria-expanded="!!expandedBookingIds[booking.id]"
                    @click="toggleAccordion(booking.id)"
                  >
                    <span class="text-body1 font-semibold text-gray-800">Booking Detail</span>
                    <IconCaretDown
                      class="size-5 text-gray-700 transition-transform duration-200"
                      :class="{ 'rotate-180': expandedBookingIds[booking.id] }"
                    />
                  </button>

                  <div
                    v-show="expandedBookingIds[booking.id]"
                    class="flex flex-col gap-6 border-t border-gray-200 px-4 pt-4 pb-6"
                  >
                    <!-- Guests & Payment status -->
                    <div class="flex flex-col gap-1 text-body1 text-gray-700 sm:flex-row sm:items-center sm:justify-between">
                      <p>{{ booking.guestsText }} ({{ booking.nightsText }})</p>
                      <p>{{ booking.paymentMethodText }}</p>
                    </div>

                    <!-- Price breakdown -->
                    <div class="flex flex-col gap-3">
                      <div
                        v-for="(item, idx) in booking.breakdown"
                        :key="idx"
                        class="flex items-center justify-between text-body1"
                        :class="item.isDiscount ? 'text-gray-600' : 'text-gray-700'"
                      >
                        <span>{{ item.label }}</span>
                        <span>{{ formatMoney(item.amount) }}</span>
                      </div>

                      <div class="mt-2 flex items-center justify-between border-t border-gray-300 pt-3 text-body1 font-semibold text-gray-900">
                        <span>Total</span>
                        <span class="text-h5">THB {{ formatMoney(booking.totalPrice) }}</span>
                      </div>
                    </div>

                    <!-- Additional Request -->
                    <div
                      v-if="booking.additionalRequest"
                      class="rounded-sm bg-gray-200 p-4 text-body1"
                    >
                      <h4 class="font-semibold text-gray-800">
                        Additional Request
                      </h4>
                      <p class="mt-1 text-gray-700">
                        {{ booking.additionalRequest }}
                      </p>
                    </div>
                  </div>
                </div>

                <!-- Desktop Action Bar -->
                <div class="hidden items-center justify-between pt-2 lg:flex">
                  <div>
                    <button
                      v-if="booking.status !== 'checked-in' && booking.status !== 'cancelled'"
                      type="button"
                      class="rounded-sm text-body1 font-semibold text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring"
                      @click="openCancelDialog(booking)"
                    >
                      Cancel Booking
                    </button>
                  </div>

                  <div class="flex items-center gap-6">
                    <Button
                      v-if="booking.status !== 'cancelled'"
                      variant="ghost"
                      as-child
                    >
                      <RouterLink :to="roomDetailLink(booking.roomName)">Room Detail</RouterLink>
                    </Button>

                    <Button
                      v-if="booking.status === 'within-24h'"
                      as-child
                    >
                      <RouterLink :to="`/bookings/${booking.id}/change-date`">
                        Change Date
                      </RouterLink>
                    </Button>
                  </div>
                </div>

                <!-- Mobile Action Bar -->
                <div class="flex flex-col gap-4 pt-2 lg:hidden">
                  <div
                    v-if="booking.status !== 'cancelled'"
                    class="flex items-center justify-between"
                  >
                    <Button variant="ghost" as-child>
                      <RouterLink :to="roomDetailLink(booking.roomName)">Room Detail</RouterLink>
                    </Button>

                    <Button
                      v-if="booking.status === 'within-24h'"
                      as-child
                    >
                      <RouterLink :to="`/bookings/${booking.id}/change-date`">
                        Change Date
                      </RouterLink>
                    </Button>
                  </div>

                  <div
                    v-if="booking.status !== 'checked-in' && booking.status !== 'cancelled'"
                    class="flex justify-end pt-1"
                  >
                    <button
                      type="button"
                      class="rounded-sm text-body1 font-semibold text-orange-500 outline-none is-hover:text-orange-400 is-focus:ring-2 is-focus:ring-ring"
                      @click="openCancelDialog(booking)"
                    >
                      Cancel Booking
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- Pagination -->
        <nav
          v-if="!bookingStore.loading && bookingStore.bookings.length > 0"
          aria-label="Pagination"
          class="mt-10 flex items-center justify-center gap-2 lg:mt-16"
        >
          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-sm text-body2 outline-none transition-colors is-focus:ring-2 is-focus:ring-ring"
            :class="currentPage === 1 ? 'cursor-not-allowed text-gray-400' : 'cursor-pointer text-gray-600 is-hover:text-black'"
            :disabled="currentPage === 1"
            aria-label="Previous page"
            @click="prevPage"
          >
            <IconChevronRight class="size-4 rotate-180" />
          </button>

          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            class="flex size-8 items-center justify-center rounded-sm text-body2 outline-none transition-colors is-focus:ring-2 is-focus:ring-ring"
            :class="page === currentPage
              ? 'border border-gray-300 bg-white font-semibold text-green-700'
              : 'cursor-pointer text-gray-600 is-hover:text-black'"
            :aria-current="page === currentPage ? 'page' : undefined"
            @click="currentPage = page"
          >
            {{ page }}
          </button>

          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-sm text-body2 outline-none transition-colors is-focus:ring-2 is-focus:ring-ring"
            :class="currentPage === totalPages ? 'cursor-not-allowed text-gray-400' : 'cursor-pointer text-gray-600 is-hover:text-black'"
            :disabled="currentPage === totalPages"
            aria-label="Next page"
            @click="nextPage"
          >
            <IconChevronRight class="size-4" />
          </button>
        </nav>
      </section>
    </main>

    <!-- Cancel Booking Confirmation Dialog -->
    <Dialog v-model:open="cancelDialogOpen">
      <DialogContent class="w-11/12 sm:w-full">
        <DialogHeader>
          <DialogTitle>Cancel Booking</DialogTitle>
        </DialogHeader>

        <DialogDescription as="div" class="p-6 text-body1 text-gray-700">
          <p v-if="selectedBooking?.status === 'checkin-soon'">
            Cancellation of the booking now will not be able to request a refund.<br>
            Are you sure you would like to cancel this booking?
          </p>
          <p v-else>
            Are you sure you would like to cancel this booking?
          </p>
        </DialogDescription>

        <DialogFooter class="flex flex-row flex-wrap justify-end gap-4 px-6 pb-6">
          <Button
            variant="secondary"
            @click="proceedToCancel"
          >
            {{ selectedBooking?.status === 'checkin-soon' ? 'Yes, I want to cancel' : 'Yes, I want to cancel and request refund' }}
          </Button>

          <DialogClose as-child>
            <Button type="button">
              No, Don’t Cancel
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <SiteFooter />
  </div>
</template>
