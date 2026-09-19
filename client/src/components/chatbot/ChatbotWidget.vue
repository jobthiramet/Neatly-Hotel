<!-- Figma: bottomsheet-chatbot (13259:21785) + FAB (13220:4990) -->
<script setup lang="ts">
import { useAuth } from '@clerk/vue'
import { onKeyStroke } from '@vueuse/core'
import { computed, nextTick, onUnmounted, ref, useTemplateRef, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { IconChat, IconClose } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  chatbotCancelTopic,
  chatbotGuestBookingCta,
  chatbotGuestCancelCta,
  chatbotSignedInCancelCta,
  isChatbotCancelLabel,
  type ChatbotCta,
  type ChatbotPaymentOption,
  type ChatbotTopic,
} from '@/data/chatbot'
import { roomDetails } from '@/data/rooms'
import { cn } from '@/lib/utils'
import { useChatbotStore } from '@/stores/chatbot'

type ChatMessage
  = { id: number, role: 'user', text: string }
  | { id: number, role: 'bot', kind: 'message', text: string }
  | { id: number, role: 'bot', kind: 'room-type', title: string, actionLabel: string, roomIds: string[] }
  | { id: number, role: 'bot', kind: 'option-with-details', title: string, options: ChatbotPaymentOption[] }
  | { id: number, role: 'bot', kind: 'cta', text: string, actionLabel: string, to: string, query?: Record<string, string> }

const BOT_REPLY_DELAY_MS = 200

const chatbot = useChatbotStore()
const route = useRoute()
const { isLoaded, isSignedIn } = useAuth()
const signedIn = computed(() => Boolean(isLoaded.value && isSignedIn.value))
const open = ref(false)
const showFab = ref(true)
const draft = ref('')
const canSend = computed(() => Boolean(draft.value.trim()))
const messages = ref<ChatMessage[]>([])
let nextMessageId = 1
let replyTimer = 0
const allRooms = Object.values(roomDetails)
const fabButton = useTemplateRef<HTMLButtonElement>('fabButton')
const closeButton = useTemplateRef<HTMLButtonElement>('closeButton')
const logRegion = useTemplateRef<HTMLElement>('logRegion')

function roomsFor(roomIds: string[]) {
  if (!roomIds.length)
    return allRooms
  const selected = roomIds.flatMap((id) => {
    const room = roomDetails[id]
    return room ? [room] : []
  })
  return selected.length ? selected : allRooms
}

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function formatPrice(amount: number) {
  return 'THB ' + amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function scrollToLatest() {
  void nextTick(() => {
    const log = logRegion.value
    if (!log) return
    log.scrollTo({
      top: log.scrollHeight,
      behavior: prefersReducedMotion() ? 'auto' : 'smooth',
    })
  })
}

function clearReplyTimer() {
  if (!replyTimer) return
  window.clearTimeout(replyTimer)
  replyTimer = 0
}

function enqueueBotReply(reply: () => void) {
  clearReplyTimer()
  const delay = prefersReducedMotion() ? 0 : BOT_REPLY_DELAY_MS
  if (delay === 0) {
    reply()
    scrollToLatest()
    return
  }
  replyTimer = window.setTimeout(() => {
    replyTimer = 0
    reply()
    scrollToLatest()
  }, delay)
}

function openPanel() {
  showFab.value = false
  open.value = true
}

function onPanelEntered() {
  closeButton.value?.focus()
  scrollToLatest()
}

function onPanelLeft() {
  showFab.value = true
  fabButton.value?.focus()
}

function closePanel() {
  if (!open.value) return
  open.value = false
  draft.value = ''
  clearReplyTimer()
}

function pushCta(cta: ChatbotCta) {
  messages.value.push({
    id: nextMessageId++,
    role: 'bot',
    kind: 'cta',
    text: cta.text,
    actionLabel: cta.actionLabel,
    to: cta.to,
    query: cta.query,
  })
}

function replyToTopic(topic: ChatbotTopic) {
  if (topic.id === 'booking' && !signedIn.value) {
    pushCta({
      ...chatbotGuestBookingCta,
      query: { redirect_url: route.fullPath },
    })
    return
  }

  if (topic.format === 'message') {
    messages.value.push({ id: nextMessageId++, role: 'bot', kind: 'message', text: topic.text })
    return
  }

  if (topic.format === 'room-type') {
    messages.value.push({
      id: nextMessageId++,
      role: 'bot',
      kind: 'room-type',
      title: topic.title,
      actionLabel: topic.actionLabel,
      roomIds: [...topic.roomIds],
    })
    return
  }

  messages.value.push({
    id: nextMessageId++,
    role: 'bot',
    kind: 'option-with-details',
    title: topic.title,
    options: topic.options,
  })
}

function replyToCancel() {
  pushCta(signedIn.value ? chatbotSignedInCancelCta : chatbotGuestCancelCta)
}

function selectCancel() {
  messages.value.push({ id: nextMessageId++, role: 'user', text: chatbotCancelTopic.label })
  scrollToLatest()
  enqueueBotReply(replyToCancel)
}

function selectTopic(topic: ChatbotTopic) {
  if (!topic.enabled) return

  messages.value.push({ id: nextMessageId++, role: 'user', text: topic.label })
  scrollToLatest()
  enqueueBotReply(() => replyToTopic(topic))
}

function handleSubmit() {
  const text = draft.value.trim()
  if (!text) return

  draft.value = ''
  messages.value.push({ id: nextMessageId++, role: 'user', text })
  scrollToLatest()

  enqueueBotReply(() => {
    if (isChatbotCancelLabel(text)) {
      replyToCancel()
      return
    }
    const topic = chatbot.findTopic(text)
    if (topic)
      replyToTopic(topic)
    else {
      messages.value.push({
        id: nextMessageId++,
        role: 'bot',
        kind: 'message',
        text: chatbot.autoReply,
      })
    }
  })
}

onKeyStroke('Escape', closePanel)

watch(open, (isOpen) => {
  document.body.style.overflow = isOpen ? 'hidden' : ''
})

watch(() => route.fullPath, () => {
  messages.value = []
  draft.value = ''
  clearReplyTimer()
})

onUnmounted(() => {
  document.body.style.overflow = ''
  clearReplyTimer()
})
</script>

<template>
  <div>
    <button
      v-show="showFab"
      ref="fabButton"
      type="button"
      aria-label="Chat with Neatly"
      aria-haspopup="dialog"
      :aria-expanded="open"
      aria-controls="chatbot-panel"
      class="fixed right-4 bottom-4 z-30 flex size-16 items-center justify-center rounded-full bg-white text-green-700 shadow-md outline-none is-hover:bg-green-100 is-focus:ring-2 is-focus:ring-ring lg:right-6 lg:bottom-6"
      @click="openPanel"
    >
      <IconChat class="size-8" />
    </button>

    <Transition
      :duration="200"
      enter-active-class="duration-200 motion-reduce:transition-none chatbot-overlay-motion"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="duration-200 motion-reduce:transition-none chatbot-overlay-motion"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <button
        v-if="open"
        type="button"
        tabindex="-1"
        aria-label="Close chatbot"
        class="fixed inset-0 z-40 bg-black/40"
        @click="closePanel"
      />
    </Transition>

    <Transition
      :duration="300"
      enter-active-class="duration-300 ease-out motion-reduce:transition-none chatbot-panel-motion"
      enter-from-class="translate-y-full opacity-0 lg:translate-x-4 lg:translate-y-0"
      enter-to-class="translate-y-0 opacity-100 lg:translate-x-0"
      leave-active-class="duration-300 ease-out motion-reduce:transition-none chatbot-panel-motion"
      leave-from-class="translate-y-0 opacity-100 lg:translate-x-0"
      leave-to-class="translate-y-full opacity-0 lg:translate-x-4 lg:translate-y-0"
      @after-enter="onPanelEntered"
      @after-leave="onPanelLeft"
    >
      <section
        v-if="open"
        id="chatbot-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chatbot-title"
        class="fixed inset-x-0 top-12 bottom-0 z-50 flex flex-col overflow-hidden rounded-t-sm bg-white shadow-md lg:top-6 lg:right-6 lg:bottom-6 lg:left-auto lg:w-187.5 lg:rounded-sm"
      >
        <header class="flex items-center justify-between bg-white pl-4">
          <div class="flex min-w-0 flex-1 items-center gap-2">
            <span class="flex size-10 shrink-0 items-center justify-center rounded-full bg-green-100 text-green-700 shadow-md">
              <IconChat class="size-6" aria-hidden="true" />
            </span>
            <h2 id="chatbot-title" class="text-h5 text-gray-900">
              Neatly Assistant
            </h2>
          </div>
          <button
            ref="closeButton"
            type="button"
            aria-label="Close chatbot"
            class="flex size-15 items-center justify-center text-gray-700 outline-none is-hover:text-gray-900 is-focus:ring-2 is-focus:ring-ring"
            @click="closePanel"
          >
            <IconClose class="size-10" />
          </button>
        </header>

        <div
          ref="logRegion"
          role="log"
          aria-live="polite"
          aria-relevant="additions"
          class="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto bg-bg px-4 py-6"
        >
          <p class="max-w-63.75 rounded-sm bg-white px-4 py-2 text-body1 whitespace-pre-line text-gray-700 lg:max-w-150">
            {{ chatbot.greeting }}
          </p>

          <TransitionGroup
            tag="div"
            class="flex flex-col gap-4"
            :duration="200"
            enter-active-class="duration-200 ease-out motion-reduce:transition-none chatbot-bubble-motion"
            enter-from-class="translate-y-2 opacity-0 motion-reduce:translate-y-0 motion-reduce:opacity-100"
            enter-to-class="translate-y-0 opacity-100"
          >
            <article
              v-for="message in messages"
              :key="message.id"
              class="flex flex-col gap-2"
            >
              <p
                v-if="message.role === 'user'"
                class="ml-auto max-w-63.75 rounded-sm bg-orange-500 px-4 py-2 text-body1 text-white lg:max-w-150"
              >
                {{ message.text }}
              </p>

              <p
                v-else-if="message.kind === 'message'"
                class="max-w-63.75 rounded-sm bg-white px-4 py-2 text-body1 whitespace-pre-line text-gray-700 lg:max-w-150"
              >
                {{ message.text }}
              </p>

              <template v-else-if="message.kind === 'room-type'">
                <p class="max-w-63.75 rounded-sm bg-white px-4 py-2 text-body1 text-gray-700 lg:max-w-150">
                  {{ message.title }}
                </p>
                <ul class="flex flex-col gap-3">
                  <li
                    v-for="room in roomsFor(message.roomIds)"
                    :key="`${message.id}-${room.id}`"
                    class="overflow-hidden rounded-sm bg-white shadow-md"
                  >
                    <img
                      :src="room.gallery[0]?.src"
                      :alt="room.gallery[0]?.alt ?? room.name"
                      width="375"
                      height="200"
                      class="h-40 w-full object-cover"
                    >
                    <div class="flex flex-col gap-3 px-4 py-3">
                      <h3 class="text-h5 text-gray-900">
                        {{ room.name }}
                      </h3>
                      <p class="text-body1 text-gray-700">
                        {{ formatPrice(room.currentPrice) }}
                      </p>
                      <Button as-child class="w-full">
                        <RouterLink :to="{ name: 'room-detail', params: { roomId: room.id } }">
                          {{ message.actionLabel }}
                        </RouterLink>
                      </Button>
                    </div>
                  </li>
                </ul>
              </template>

              <div
                v-else-if="message.kind === 'cta'"
                class="flex max-w-63.75 flex-col gap-2 lg:max-w-150"
              >
                <p class="rounded-sm bg-white px-4 py-2 text-body1 text-gray-700">
                  {{ message.text }}
                </p>
                <Button as-child class="w-full">
                  <RouterLink :to="{ path: message.to, query: message.query }">
                    {{ message.actionLabel }}
                  </RouterLink>
                </Button>
              </div>

              <template v-else>
                <p class="max-w-63.75 rounded-sm bg-white px-4 py-2 text-body1 text-gray-700 lg:max-w-150">
                  {{ message.title }}
                </p>
                <ul class="flex flex-col gap-2">
                  <li v-for="option in message.options" :key="`${message.id}-${option.label}`">
                    <details class="rounded-sm bg-white px-4 py-2">
                      <summary class="cursor-pointer text-body1 text-gray-900 outline-none is-focus:ring-2 is-focus:ring-ring">
                        {{ option.label }}
                      </summary>
                      <p class="mt-2 text-body1 text-gray-700">
                        {{ option.detail }}
                      </p>
                    </details>
                  </li>
                </ul>
              </template>
            </article>
          </TransitionGroup>

          <ul class="flex flex-wrap gap-2" aria-label="Suggested topics">
            <li v-for="topic in chatbot.topics" :key="topic.id">
              <button
                type="button"
                :disabled="!topic.enabled"
                :aria-disabled="!topic.enabled"
                :class="cn(
                  'rounded-full border px-4 py-2 text-body1 outline-none',
                  topic.enabled
                    ? 'border-green-400 bg-green-200 text-green-700 is-hover:bg-green-300 is-focus:ring-2 is-focus:ring-ring'
                    : 'cursor-not-allowed border-gray-400 bg-gray-200 text-gray-600',
                )"
                @click="selectTopic(topic)"
              >
                {{ topic.label }}
              </button>
            </li>
            <li>
              <button
                type="button"
                :class="cn(
                  'rounded-full border border-green-400 bg-green-200 px-4 py-2 text-body1 text-green-700 outline-none',
                  'is-hover:bg-green-300 is-focus:ring-2 is-focus:ring-ring',
                )"
                @click="selectCancel"
              >
                {{ chatbotCancelTopic.label }}
              </button>
            </li>
          </ul>
        </div>

        <form
          class="flex items-center gap-2 bg-white px-4 pt-2 pb-6"
          @submit.prevent="handleSubmit"
        >
          <label class="sr-only" for="chatbot-message">Write your message</label>
          <Input
            id="chatbot-message"
            v-model="draft"
            class="border-0 py-1.5 is-focus:border-transparent"
            placeholder="Write your message"
            autocomplete="off"
          />
          <button
            type="submit"
            aria-label="Send message"
            :disabled="!canSend"
            class="flex size-6 shrink-0 items-center justify-center text-orange-500 outline-none is-focus:ring-2 is-focus:ring-ring disabled:cursor-not-allowed disabled:text-gray-500"
          >
            <svg
              class="size-6"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path d="M3.4 2.25a.75.75 0 0 0-.82 1.02L5.2 11.25H13.5a.75.75 0 0 1 0 1.5H5.2l-2.62 7.98a.75.75 0 0 0 .82 1.02 61 61 0 0 0 17.7-8.85.75.75 0 0 0 0-1.3A61 61 0 0 0 3.4 2.25Z" />
            </svg>
          </button>
        </form>
      </section>
    </Transition>
  </div>
</template>

<style scoped>
.chatbot-overlay-motion {
  transition-property: opacity;
}

.chatbot-panel-motion,
.chatbot-bubble-motion {
  transition-property: opacity, translate;
}
</style>
