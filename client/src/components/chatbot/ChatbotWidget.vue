<!-- Figma: bottomsheet-chatbot (13259:21785) + FAB (13220:4990) -->
<script setup lang="ts">
import { onKeyStroke } from '@vueuse/core'
import { nextTick, onUnmounted, ref, useTemplateRef, watch } from 'vue'
import { IconChat, IconClose } from '@/components/icons'
import { Input } from '@/components/ui/input'
import { chatbotGreeting, chatbotTopics } from '@/data/chatbot'

const open = ref(false)
const draft = ref('')
const fabButton = useTemplateRef<HTMLButtonElement>('fabButton')
const closeButton = useTemplateRef<HTMLButtonElement>('closeButton')

function openPanel() {
  open.value = true
  void nextTick(() => closeButton.value?.focus())
}

function closePanel() {
  if (!open.value) return
  open.value = false
  draft.value = ''
  void nextTick(() => fabButton.value?.focus())
}

function handleSubmit() {
  // Round 1: the composer is visual only. Sending is a later round.
}

onKeyStroke('Escape', closePanel)

watch(open, (isOpen) => {
  document.body.style.overflow = isOpen ? 'hidden' : ''
})

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <div>
    <button
      v-show="!open"
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

    <template v-if="open">
      <button
        type="button"
        tabindex="-1"
        aria-label="Close chatbot"
        class="fixed inset-0 z-40 bg-black/40"
        @click="closePanel"
      />

      <section
        id="chatbot-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chatbot-title"
        class="fixed inset-x-0 top-12 bottom-0 z-50 flex flex-col overflow-hidden rounded-t-sm bg-white shadow-md lg:top-6 lg:right-6 lg:bottom-6 lg:left-auto lg:w-93.75 lg:rounded-sm"
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

        <div class="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto bg-bg px-4 py-6">
          <p class="max-w-63.75 rounded-sm bg-white px-4 py-2 text-body1 text-gray-700">
            {{ chatbotGreeting }}
          </p>
          <ul class="flex flex-wrap gap-2" aria-label="Suggested topics">
            <li v-for="topic in chatbotTopics" :key="topic">
              <button
                type="button"
                class="rounded-full border border-green-400 bg-green-200 px-4 py-2 text-body1 text-green-700 outline-none is-hover:bg-green-300 is-focus:ring-2 is-focus:ring-ring"
              >
                {{ topic }}
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
            class="flex size-6 shrink-0 items-center justify-center text-orange-500 outline-none is-focus:ring-2 is-focus:ring-ring"
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
    </template>
  </div>
</template>
