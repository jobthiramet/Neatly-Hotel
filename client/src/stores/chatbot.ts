import { isAxiosError } from 'axios'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api/client'
import {
  chatbotAutoReply,
  chatbotGreeting,
  chatbotTopics,
  findChatbotTopic,
  type ChatbotTopic,
} from '@/data/chatbot'
import { apiErrorMessage } from '@/stores/hotel'

interface ChatbotScriptResponse {
  greeting: string
  autoReply: string
  topics: Array<Record<string, unknown>>
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

function cloneTopics(): ChatbotTopic[] {
  return structuredClone(chatbotTopics)
}

function toTopic(raw: Record<string, unknown>): ChatbotTopic | null {
  const id = String(raw.id ?? '')
  const label = String(raw.label ?? '')
  if (!id || !label)
    return null
  if (raw.format === 'message' && typeof raw.text === 'string' && raw.text.trim()) {
    return { id, label, enabled: true, format: 'message', text: raw.text }
  }
  if (raw.format === 'room-type' && typeof raw.title === 'string' && typeof raw.actionLabel === 'string') {
    const roomIds = Array.isArray(raw.roomIds) ? raw.roomIds.map(String) : []
    return { id, label, enabled: true, format: 'room-type', title: raw.title, actionLabel: raw.actionLabel, roomIds }
  }
  if (raw.format === 'option-with-details' && typeof raw.title === 'string' && Array.isArray(raw.options)) {
    const options = raw.options.flatMap((option) => {
      if (!option || typeof option !== 'object')
        return []
      const item = option as Record<string, unknown>
      if (typeof item.label !== 'string' || typeof item.detail !== 'string')
        return []
      return [{ label: item.label, detail: item.detail }]
    })
    if (!raw.title.trim() || options.length === 0)
      return null
    return { id, label, enabled: true, format: 'option-with-details', title: raw.title, options }
  }
  return null
}

function scriptIsValid(greeting: string, autoReply: string, topics: ChatbotTopic[]) {
  if (!greeting.trim() || !autoReply.trim() || topics.length === 0)
    return false
  const ids = new Set<string>()
  for (const topic of topics) {
    if (!topic.id.trim() || !topic.label.trim() || ids.has(topic.id))
      return false
    ids.add(topic.id)
    if (topic.format === 'message' && !topic.text.trim())
      return false
    if (topic.format === 'room-type' && (!topic.title.trim() || !topic.actionLabel.trim()))
      return false
    if (topic.format === 'option-with-details') {
      if (!topic.title.trim() || topic.options.length === 0 || topic.options.some(option => !option.label.trim() || !option.detail.trim()))
        return false
    }
  }
  return true
}

export const useChatbotStore = defineStore('chatbot', () => {
  const greeting = ref(chatbotGreeting)
  const autoReply = ref(chatbotAutoReply)
  const topics = ref<ChatbotTopic[]>(cloneTopics())
  const loading = ref(false)
  const loadError = ref('')
  const saveError = ref('')
  const ready = ref(false)
  let pending: Promise<void> | null = null

  function apply(script: ChatbotScriptResponse) {
    const next = script.topics.map(toTopic).filter((topic): topic is ChatbotTopic => topic !== null)
    if (!script.greeting.trim() || !script.autoReply.trim() || next.length === 0)
      return false
    greeting.value = script.greeting
    autoReply.value = script.autoReply
    topics.value = next
    return true
  }

  async function ensureLoaded() {
    if (ready.value)
      return
    pending ??= load()
    await pending
  }

  async function load() {
    loading.value = true
    loadError.value = ''
    try {
      const { data } = await api.get<ApiResponse<ChatbotScriptResponse>>('/chatbot')
      if (!apply(data.data))
        loadError.value = 'Could not read the chatbot script.'
      else
        ready.value = true
    }
    catch (error) {
      loadError.value = apiErrorMessage(error, 'Could not load the chatbot script.')
    }
    finally {
      loading.value = false
      pending = null
    }
  }

  async function persist() {
    if (!ready.value || !scriptIsValid(greeting.value, autoReply.value, topics.value))
      return
    saveError.value = ''
    try {
      const token = await window.Clerk?.session?.getToken()
      if (!token)
        throw new Error('Please log in as an agent before updating the chatbot.')
      const { data } = await api.put<ApiResponse<ChatbotScriptResponse>>('/chatbot', {
        greeting: greeting.value.trim(),
        autoReply: autoReply.value.trim(),
        topics: topics.value,
      }, { headers: { Authorization: `Bearer ${token}` } })
      apply(data.data)
    }
    catch (error) {
      saveError.value = isAxiosError(error) || error instanceof Error
        ? apiErrorMessage(error, 'Could not save the chatbot script.')
        : 'Could not save the chatbot script.'
    }
  }

  function addTopic(topic: ChatbotTopic) {
    topics.value = [...topics.value, topic]
  }

  function replaceTopic(id: string, topic: ChatbotTopic) {
    const index = topics.value.findIndex(item => item.id === id)
    if (index < 0)
      return
    const next = [...topics.value]
    next[index] = topic
    topics.value = next
  }

  function removeTopic(id: string) {
    if (topics.value.length <= 1)
      return
    topics.value = topics.value.filter(topic => topic.id !== id)
  }

  function moveTopic(from: number, to: number) {
    if (to < 0 || to >= topics.value.length || from === to)
      return
    const next = [...topics.value]
    next.splice(to, 0, ...next.splice(from, 1))
    topics.value = next
  }

  function findTopic(text: string) {
    return findChatbotTopic(topics.value, text)
  }

  function hasTopicId(id: string) {
    return topics.value.some(topic => topic.id === id)
  }

  return {
    greeting,
    autoReply,
    topics,
    loading,
    loadError,
    saveError,
    ensureLoaded,
    persist,
    addTopic,
    replaceTopic,
    removeTopic,
    moveTopic,
    findTopic,
    hasTopicId,
  }
})
