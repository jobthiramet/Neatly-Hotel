<!-- Figma: user > booking history (node 7415:8200 desktop, mobile > user > booking) -->
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { IconCaretDown } from '@/components/icons'
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
import { useBookingStore } from '@/stores/booking'

const router = useRouter()
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

// Track accordion expanded states
const expandedBookingIds = ref<Record<string, boolean>>(
  Object.fromEntries(
    bookingStore.bookings.map(b => [b.id, !!b.expandedInitially]),
  ),
)

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
const totalPages = 5
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

        <!-- Booking Cards List -->
        <div class="mt-6 flex flex-col lg:mt-12">
          <article
            v-for="booking in bookingStore.bookings"
            :key="booking.id"
            class="-mx-4 border-b border-gray-300 py-6 lg:mx-0 lg:py-10"
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
                    class="flex w-full items-center justify-between p-4 text-left outline-none is-focus:ring-2 is-focus:ring-ring"
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
                      <RouterLink to="/#rooms">Room Detail</RouterLink>
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
                      <RouterLink to="/#rooms">Room Detail</RouterLink>
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
          aria-label="Pagination"
          class="mt-10 flex items-center justify-center gap-2 lg:mt-16"
        >
          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-sm text-body2 text-gray-400 outline-none is-focus:ring-2 is-focus:ring-ring"
            :disabled="currentPage === 1"
            aria-label="Previous page"
          >
            &lt;
          </button>

          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            class="flex size-8 items-center justify-center rounded-sm text-body2 outline-none is-focus:ring-2 is-focus:ring-ring"
            :class="page === currentPage
              ? 'border border-orange-500 font-semibold text-orange-500'
              : 'text-gray-700 is-hover:text-black'"
            :aria-current="page === currentPage ? 'page' : undefined"
            @click="currentPage = page"
          >
            {{ page }}
          </button>

          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-sm text-body2 text-gray-700 outline-none is-hover:text-black is-focus:ring-2 is-focus:ring-ring"
            aria-label="Next page"
          >
            &gt;
          </button>
        </nav>
      </section>
    </main>

    <!-- Cancel Booking Confirmation Dialog -->
    <Dialog v-model:open="cancelDialogOpen">
      <DialogContent class="w-11/12 max-w-130 sm:w-full">
        <DialogHeader>
          <DialogTitle>Cancel Booking</DialogTitle>
        </DialogHeader>

        <DialogDescription class="text-body1 text-gray-700">
          <template v-if="selectedBooking?.status === 'checkin-soon'">
            Cancellation of the booking now will not be able to request a refund.
            Are you sure you would like to cancel this booking?
          </template>
          <template v-else>
            Are you sure you would like to cancel this booking?
          </template>
        </DialogDescription>

        <DialogFooter class="flex flex-col gap-4 px-6 pb-6 sm:flex-col sm:justify-start">
          <DialogClose as-child>
            <Button
              type="button"
              class="w-full"
            >
              No, Don't Cancel
            </Button>
          </DialogClose>

          <Button
            variant="secondary"
            class="w-full"
            @click="proceedToCancel"
          >
            {{ selectedBooking?.status === 'checkin-soon' ? 'Yes, I want to cancel' : 'Yes, I want to cancel and request refund' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <SiteFooter />
  </div>
</template>
