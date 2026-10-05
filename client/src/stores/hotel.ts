import { isAxiosError } from 'axios'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api/client'

/** Matches `HotelInfoResponse` in docs/API.md. */
export interface HotelInfoResponse {
  id: string
  name: string
  /** Paragraphs separated by blank lines. */
  description: string
  logoUrl: string | null
  updatedAt: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/** Server `ErrorResponse.message`, or a fallback. */
export function apiErrorMessage(error: unknown, fallback: string) {
  return (isAxiosError(error) && error.response?.data?.message) || fallback
}

export const useHotelStore = defineStore('hotel', () => {
  const name = ref('')
  const description = ref('')
  const logoUrl = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  function apply(info: HotelInfoResponse) {
    name.value = info.name
    description.value = info.description
    logoUrl.value = info.logoUrl
  }

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get<ApiResponse<HotelInfoResponse>>('/hotel')
      apply(data.data)
    }
    catch (e) {
      error.value = apiErrorMessage(e, 'Could not load hotel information.')
    }
    finally {
      loading.value = false
    }
  }

  async function save(info: { name: string, description: string }) {
    const headers = await agentHeaders()
    const { data } = await api.put<ApiResponse<HotelInfoResponse>>('/hotel', info, { headers })
    apply(data.data)
  }

  async function uploadLogo(file: File) {
    const headers = await agentHeaders()
    const body = new FormData()
    body.append('file', file)
    // Override the instance's JSON default so axios sends real multipart data.
    const { data } = await api.put<ApiResponse<HotelInfoResponse>>('/hotel/logo', body, {
      headers: { ...headers, 'Content-Type': 'multipart/form-data' },
    })
    apply(data.data)
  }

  return { name, description, logoUrl, loading, error, fetch, save, uploadLogo }
})

async function agentHeaders() {
  const token = await window.Clerk?.session?.getToken()
  if (!token) throw new Error('Please log in as an agent before updating hotel information.')
  return { Authorization: `Bearer ${token}` }
}
