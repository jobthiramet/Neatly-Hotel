import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  chatbotAutoReply,
  chatbotGreeting,
  chatbotTopics,
  findChatbotTopic,
  type ChatbotTopic,
} from '@/data/chatbot'

function cloneTopics(): ChatbotTopic[] {
  return structuredClone(chatbotTopics)
}

export const useChatbotStore = defineStore('chatbot', () => {
  const greeting = ref(chatbotGreeting)
  const autoReply = ref(chatbotAutoReply)
  const topics = ref<ChatbotTopic[]>(cloneTopics())

  function updateDefaults(patch: { greeting?: string, autoReply?: string }) {
    if (patch.greeting !== undefined)
      greeting.value = patch.greeting
    if (patch.autoReply !== undefined)
      autoReply.value = patch.autoReply
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
    updateDefaults,
    addTopic,
    replaceTopic,
    removeTopic,
    moveTopic,
    findTopic,
    hasTopicId,
  }
})
