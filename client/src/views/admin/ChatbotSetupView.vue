<!-- Figma: (admin) chatbot setup -->
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { IconEdit, IconGrip, IconTrash } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import {
  chatbotDefaultRoomIds,
  type ChatbotPaymentOption,
  type ChatbotReplyFormat,
  type ChatbotTopic,
} from '@/data/chatbot'
import { roomDetails } from '@/data/rooms'
import { cn } from '@/lib/utils'
import { useChatbotStore } from '@/stores/chatbot'

const FORMAT_LABELS: Record<ChatbotReplyFormat, string> = {
  message: 'Message',
  'room-type': 'Room type',
  'option-with-details': 'Option with details',
}

const chatbot = useChatbotStore()
onMounted(() => {
  void chatbot.ensureLoaded()
})

function saveScript() {
  void chatbot.persist()
}
const allRooms = Object.values(roomDetails)
const dragIndex = ref<number | null>(null)
const adding = ref(false)
const draftErrors = reactive<Record<string, string>>({})

interface DraftTopic {
  label: string
  format: ChatbotReplyFormat | ''
  text: string
  title: string
  actionLabel: string
  roomIds: string[]
  options: ChatbotPaymentOption[]
}

function emptyDraft(): DraftTopic {
  return {
    label: '',
    format: '',
    text: '',
    title: '',
    actionLabel: 'View Details',
    roomIds: [...chatbotDefaultRoomIds],
    options: [{ label: '', detail: '' }],
  }
}

const draft = reactive<DraftTopic>(emptyDraft())

function roomName(id: string) {
  return roomDetails[id]?.name ?? id
}

function remainingRooms(roomIds: string[]) {
  return allRooms.filter(room => !roomIds.includes(room.id))
}

function slugFromLabel(label: string) {
  const base = label.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'topic'
  let id = base
  let n = 2
  while (id === 'cancel-booking' || chatbot.hasTopicId(id)) {
    id = `${base}-${n++}`
  }
  return id
}

function topicFromDraft(id: string, source: DraftTopic): ChatbotTopic | null {
  const label = source.label.trim()
  if (!label || !source.format)
    return null
  if (source.format === 'message') {
    if (!source.text.trim())
      return null
    return { id, label, enabled: true, format: 'message', text: source.text }
  }
  if (source.format === 'room-type') {
    if (!source.title.trim() || !source.actionLabel.trim())
      return null
    return {
      id,
      label,
      enabled: true,
      format: 'room-type',
      title: source.title,
      actionLabel: source.actionLabel,
      roomIds: [...source.roomIds],
    }
  }
  const options = source.options
    .map(option => ({ label: option.label.trim(), detail: option.detail.trim() }))
    .filter(option => option.label && option.detail)
  if (!source.title.trim() || options.length === 0)
    return null
  return { id, label, enabled: true, format: 'option-with-details', title: source.title, options }
}

function changeFormat(topic: ChatbotTopic, format: string) {
  if (format !== 'message' && format !== 'room-type' && format !== 'option-with-details')
    return
  if (topic.format === format)
    return
  const label = topic.label
  const id = topic.id
  if (format === 'message') {
    chatbot.replaceTopic(id, { id, label, enabled: true, format, text: '' })
    saveScript()
    return
  }
  if (format === 'room-type') {
    chatbot.replaceTopic(id, {
      id,
      label,
      enabled: true,
      format,
      title: '',
      actionLabel: 'View Details',
      roomIds: [...chatbotDefaultRoomIds],
    })
    saveScript()
    return
  }
  chatbot.replaceTopic(id, {
    id,
    label,
    enabled: true,
    format,
    title: '',
    options: [{ label: '', detail: '' }],
  })
  saveScript()
}

function addRoom(topic: Extract<ChatbotTopic, { format: 'room-type' }>, roomId: string) {
  if (!roomId || topic.roomIds.includes(roomId))
    return
  topic.roomIds.push(roomId)
  saveScript()
}

function removeRoom(topic: Extract<ChatbotTopic, { format: 'room-type' }>, roomId: string) {
  topic.roomIds = topic.roomIds.filter(id => id !== roomId)
  saveScript()
}

function addOption(topic: Extract<ChatbotTopic, { format: 'option-with-details' }>) {
  topic.options.push({ label: '', detail: '' })
  saveScript()
}

function removeOption(topic: Extract<ChatbotTopic, { format: 'option-with-details' }>, index: number) {
  if (topic.options.length <= 1)
    return
  topic.options.splice(index, 1)
  saveScript()
}

function focusTopic(id: string) {
  document.getElementById(`topic-${id}`)?.focus()
}

function onDragStart(index: number) {
  dragIndex.value = index
}

function onDrop(index: number) {
  if (dragIndex.value !== null)
    chatbot.moveTopic(dragIndex.value, index)
  dragIndex.value = null
  saveScript()
}

function onReorderKey(event: KeyboardEvent, index: number) {
  if (!event.altKey || (event.key !== 'ArrowUp' && event.key !== 'ArrowDown'))
    return
  event.preventDefault()
  const to = index + (event.key === 'ArrowUp' ? -1 : 1)
  chatbot.moveTopic(index, to)
  saveScript()
  void nextTick(() => {
    document.getElementById(`reorder-${chatbot.topics[to]?.id}`)?.focus()
  })
}

function deleteTopic(id: string) {
  chatbot.removeTopic(id)
  saveScript()
}

function startAdd() {
  Object.assign(draft, emptyDraft())
  Object.keys(draftErrors).forEach(key => delete draftErrors[key])
  adding.value = true
}

function cancelAdd() {
  adding.value = false
  Object.assign(draft, emptyDraft())
  Object.keys(draftErrors).forEach(key => delete draftErrors[key])
}

function validateDraft() {
  Object.keys(draftErrors).forEach(key => delete draftErrors[key])
  if (!draft.label.trim())
    draftErrors.label = 'Topic is required.'
  if (!draft.format)
    draftErrors.format = 'Reply format is required.'
  else if (draft.format === 'message' && !draft.text.trim())
    draftErrors.text = 'Reply message is required.'
  else if (draft.format === 'room-type') {
    if (!draft.title.trim())
      draftErrors.title = 'Reply title is required.'
    if (!draft.actionLabel.trim())
      draftErrors.actionLabel = 'Button name is required.'
  }
  else if (draft.format === 'option-with-details') {
    if (!draft.title.trim())
      draftErrors.title = 'Reply title is required.'
    if (!draft.options.some(option => option.label.trim() && option.detail.trim()))
      draftErrors.options = 'Add at least one option with details.'
  }
  return Object.keys(draftErrors).length === 0
}

function saveDraft() {
  if (!validateDraft())
    return
  const topic = topicFromDraft(slugFromLabel(draft.label), draft)
  if (!topic)
    return
  chatbot.addTopic(topic)
  saveScript()
  cancelAdd()
}

function onDraftFormat(format: string) {
  if (format === 'message' || format === 'room-type' || format === 'option-with-details')
    draft.format = format
}
</script>

<template>
  <div class="flex flex-col gap-10 rounded-sm bg-white px-6 py-10 lg:px-20">
    <p v-if="chatbot.loading" class="text-body1 text-gray-700" role="status">
      Loading chatbot script…
    </p>
    <p v-if="chatbot.loadError" class="text-body1 text-red" role="alert">
      {{ chatbot.loadError }} The guest panel is still using the built-in script.
    </p>
    <p v-if="chatbot.saveError" class="text-body1 text-red" role="alert">
      {{ chatbot.saveError }}
    </p>
    <section class="flex flex-col gap-6" aria-labelledby="default-messages-title">
      <h2 id="default-messages-title" class="text-body1 font-semibold text-gray-600">
        Default Chatbot Messages
      </h2>

      <FormField label="Greeting message *" for="chatbot-greeting">
        <Textarea id="chatbot-greeting" v-model="chatbot.greeting" rows="3" @blur="saveScript" />
      </FormField>

      <FormField label="Auto-reply message *" for="chatbot-auto-reply">
        <Textarea id="chatbot-auto-reply" v-model="chatbot.autoReply" rows="3" @blur="saveScript" />
      </FormField>
    </section>

    <hr class="border-gray-300">

    <section class="flex flex-col gap-6" aria-labelledby="suggestion-menu-title">
      <h2 id="suggestion-menu-title" class="text-body1 font-semibold text-gray-600">
        Suggestion menu &amp; Response
      </h2>

      <article
        v-for="(topic, index) in chatbot.topics"
        :key="topic.id"
        :class="cn(
          'flex flex-col gap-6 rounded-sm bg-gray-100 p-4',
          dragIndex === index && 'opacity-50',
        )"
        @dragover.prevent
        @drop.prevent="onDrop(index)"
      >
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
          <FormField class="lg:flex-1" label="Topic *" :for="`topic-${topic.id}`">
            <Input :id="`topic-${topic.id}`" v-model="topic.label" @blur="saveScript" />
          </FormField>

          <FormField class="lg:flex-1" label="Reply format" :for="`format-${topic.id}`">
            <Select :model-value="topic.format" @update:model-value="value => changeFormat(topic, String(value ?? ''))">
              <SelectTrigger :id="`format-${topic.id}`">
                <SelectValue :placeholder="FORMAT_LABELS[topic.format]" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="(label, value) in FORMAT_LABELS" :key="value" :value="value">
                  {{ label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </FormField>

          <div class="flex items-center gap-2 lg:pt-7">
            <button
              :id="`reorder-${topic.id}`"
              type="button"
              draggable="true"
              class="flex size-8 cursor-grab items-center justify-center text-gray-600 outline-none is-hover:text-gray-800 is-focus:ring-2 is-focus:ring-ring"
              :aria-label="`Reorder ${topic.label}. Drag or press Alt and arrow keys to move.`"
              @dragstart="onDragStart(index)"
              @dragend="dragIndex = null"
              @keydown="onReorderKey($event, index)"
            >
              <IconGrip class="size-5" />
            </button>
            <button
              type="button"
              class="flex size-8 items-center justify-center text-gray-600 outline-none is-hover:text-gray-800 is-focus:ring-2 is-focus:ring-ring"
              :aria-label="`Edit ${topic.label}`"
              @click="focusTopic(topic.id)"
            >
              <IconEdit class="size-5" />
            </button>
            <button
              type="button"
              class="flex size-8 items-center justify-center text-gray-600 outline-none is-hover:text-red is-focus:ring-2 is-focus:ring-ring disabled:cursor-not-allowed disabled:text-gray-400"
              :disabled="chatbot.topics.length <= 1"
              :aria-label="`Delete ${topic.label}`"
              @click="deleteTopic(topic.id)"
            >
              <IconTrash class="size-5" />
            </button>
          </div>
        </div>

        <FormField
          v-if="topic.format === 'message'"
          label="Reply message"
          :for="`text-${topic.id}`"
        >
          <Textarea :id="`text-${topic.id}`" v-model="topic.text" rows="4" @blur="saveScript" />
        </FormField>

        <template v-else-if="topic.format === 'room-type'">
          <FormField label="Reply title" :for="`title-${topic.id}`">
            <Input :id="`title-${topic.id}`" v-model="topic.title" @blur="saveScript" />
          </FormField>

          <div class="flex flex-col gap-2">
            <p class="text-body1 font-medium text-gray-800">
              Room type
            </p>
            <ul class="flex flex-wrap gap-2">
              <li
                v-for="roomId in topic.roomIds"
                :key="roomId"
                class="flex items-center gap-1 rounded-full bg-gray-200 py-1 pr-2 pl-3 text-body2 text-gray-800"
              >
                {{ roomName(roomId) }}
                <button
                  type="button"
                  class="flex size-4 items-center justify-center text-gray-600 outline-none is-hover:text-gray-900 is-focus:ring-2 is-focus:ring-ring"
                  :aria-label="`Remove ${roomName(roomId)}`"
                  @click="removeRoom(topic, roomId)"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </li>
            </ul>
            <Select
              v-if="remainingRooms(topic.roomIds).length"
              :model-value="''"
              @update:model-value="value => addRoom(topic, String(value ?? ''))"
            >
              <SelectTrigger :id="`add-room-${topic.id}`" :aria-label="`Add a room to ${topic.label}`">
                <SelectValue placeholder="Add room type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="room in remainingRooms(topic.roomIds)"
                  :key="room.id"
                  :value="room.id"
                >
                  {{ room.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <FormField label="Button name" :for="`action-${topic.id}`">
            <Input :id="`action-${topic.id}`" v-model="topic.actionLabel" @blur="saveScript" />
          </FormField>
        </template>

        <template v-else>
          <FormField label="Reply title" :for="`title-${topic.id}`">
            <Input :id="`title-${topic.id}`" v-model="topic.title" @blur="saveScript" />
          </FormField>

          <div
            v-for="(option, optionIndex) in topic.options"
            :key="optionIndex"
            class="grid gap-4 lg:grid-cols-2"
          >
            <FormField label="Option" :for="`option-${topic.id}-${optionIndex}`">
              <Input :id="`option-${topic.id}-${optionIndex}`" v-model="option.label" @blur="saveScript" />
            </FormField>
            <div class="flex items-end gap-2">
              <FormField class="flex-1" label="Details" :for="`detail-${topic.id}-${optionIndex}`">
                <Input :id="`detail-${topic.id}-${optionIndex}`" v-model="option.detail" @blur="saveScript" />
              </FormField>
              <Button
                type="button"
                variant="ghost"
                class="mb-0.5"
                :disabled="topic.options.length <= 1"
                :aria-label="`Remove option ${optionIndex + 1}`"
                @click="removeOption(topic, optionIndex)"
              >
                Remove
              </Button>
            </div>
          </div>
          <Button type="button" variant="ghost" @click="addOption(topic)">
            Add option
          </Button>
        </template>
      </article>

      <article v-if="adding" class="flex flex-col gap-6 rounded-sm bg-gray-100 p-4">
        <div class="flex flex-col gap-4 lg:flex-row">
          <FormField class="lg:flex-1" label="Topic *" for="draft-topic" :error="draftErrors.label">
            <Input
              id="draft-topic"
              v-model="draft.label"
              :aria-invalid="!!draftErrors.label"
              aria-describedby="draft-topic-error"
            />
          </FormField>
          <FormField class="lg:flex-1" label="Reply format" for="draft-format" :error="draftErrors.format">
            <Select :model-value="draft.format" @update:model-value="value => onDraftFormat(String(value ?? ''))">
              <SelectTrigger id="draft-format" :aria-invalid="!!draftErrors.format" aria-describedby="draft-format-error">
                <SelectValue placeholder="Select reply format" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="(label, value) in FORMAT_LABELS" :key="value" :value="value">
                  {{ label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </FormField>
        </div>

        <FormField
          v-if="draft.format === 'message'"
          label="Reply message"
          for="draft-text"
          :error="draftErrors.text"
        >
          <Textarea
            id="draft-text"
            v-model="draft.text"
            rows="4"
            :aria-invalid="!!draftErrors.text"
            aria-describedby="draft-text-error"
          />
        </FormField>

        <template v-else-if="draft.format === 'room-type'">
          <FormField label="Reply title" for="draft-title" :error="draftErrors.title">
            <Input
              id="draft-title"
              v-model="draft.title"
              :aria-invalid="!!draftErrors.title"
              aria-describedby="draft-title-error"
            />
          </FormField>
          <div class="flex flex-col gap-2">
            <p class="text-body1 font-medium text-gray-800">
              Room type
            </p>
            <ul class="flex flex-wrap gap-2">
              <li
                v-for="roomId in draft.roomIds"
                :key="roomId"
                class="flex items-center gap-1 rounded-full bg-gray-200 py-1 pr-2 pl-3 text-body2 text-gray-800"
              >
                {{ roomName(roomId) }}
                <button
                  type="button"
                  class="flex size-4 items-center justify-center text-gray-600 outline-none is-hover:text-gray-900 is-focus:ring-2 is-focus:ring-ring"
                  :aria-label="`Remove ${roomName(roomId)}`"
                  @click="draft.roomIds = draft.roomIds.filter(id => id !== roomId)"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </li>
            </ul>
            <Select
              v-if="remainingRooms(draft.roomIds).length"
              :model-value="''"
              @update:model-value="value => { const id = String(value ?? ''); if (id && !draft.roomIds.includes(id)) draft.roomIds.push(id) }"
            >
              <SelectTrigger id="draft-add-room" aria-label="Add a room to this topic">
                <SelectValue placeholder="Add room type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="room in remainingRooms(draft.roomIds)"
                  :key="room.id"
                  :value="room.id"
                >
                  {{ room.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <FormField label="Button name" for="draft-action" :error="draftErrors.actionLabel">
            <Input
              id="draft-action"
              v-model="draft.actionLabel"
              :aria-invalid="!!draftErrors.actionLabel"
              aria-describedby="draft-action-error"
            />
          </FormField>
        </template>

        <template v-else-if="draft.format === 'option-with-details'">
          <FormField label="Reply title" for="draft-title" :error="draftErrors.title">
            <Input
              id="draft-title"
              v-model="draft.title"
              :aria-invalid="!!draftErrors.title"
              aria-describedby="draft-title-error"
            />
          </FormField>
          <div
            v-for="(option, optionIndex) in draft.options"
            :key="optionIndex"
            class="grid gap-4 lg:grid-cols-2"
          >
            <FormField label="Option" :for="`draft-option-${optionIndex}`">
              <Input :id="`draft-option-${optionIndex}`" v-model="option.label" />
            </FormField>
            <FormField label="Details" :for="`draft-detail-${optionIndex}`">
              <Input :id="`draft-detail-${optionIndex}`" v-model="option.detail" />
            </FormField>
          </div>
          <p v-if="draftErrors.options" class="text-body2 text-red">
            {{ draftErrors.options }}
          </p>
          <Button type="button" variant="ghost" @click="draft.options.push({ label: '', detail: '' })">
            Add option
          </Button>
        </template>

        <div class="flex flex-wrap gap-4">
          <Button type="button" @click="saveDraft">
            Save
          </Button>
          <Button type="button" variant="ghost" @click="cancelAdd">
            Cancel
          </Button>
        </div>
      </article>

      <Button
        v-if="!adding"
        type="button"
        variant="secondary"
        class="self-start"
        @click="startAdd"
      >
        + Add Suggestion menu
      </Button>
    </section>
  </div>
</template>
