import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api/client'
import { apiErrorMessage } from '@/stores/hotel'

export type DiscountType = 'FIXED' | 'PERCENT'

export interface PromotionRoomType {
  id: string
  name: string
}

/** Matches `PromotionCodeResponse` in docs/API.md. */
export interface PromotionCode {
  id: string
  code: string
  discountType: DiscountType
  amountOff: number | null
  percentOff: number | null
  minPurchaseAmount: number
  /** Empty means every room type. */
  roomTypes: PromotionRoomType[]
}

export interface PromotionCodePayload {
  code: string
  discountType: DiscountType
  amountOff: number | null
  percentOff: number | null
  minPurchaseAmount: number
  roomTypeIds: string[]
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export const usePromotionCodesStore = defineStore('promotionCodes', () => {
  const codes = ref<PromotionCode[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      const headers = await agentHeaders()
      const { data } = await api.get<ApiResponse<PromotionCode[]>>('/promotion-codes', { headers })
      codes.value = data.data ?? []
    }
    catch (e) {
      error.value = apiErrorMessage(e, 'Could not load promo codes.')
      throw e
    }
    finally {
      loading.value = false
    }
  }

  async function create(payload: PromotionCodePayload) {
    const headers = await agentHeaders()
    const { data } = await api.post<ApiResponse<PromotionCode>>('/promotion-codes', payload, { headers })
    codes.value = [...codes.value, data.data].sort((a, b) => a.code.localeCompare(b.code))
    return data.data
  }

  async function update(id: string, payload: PromotionCodePayload) {
    const headers = await agentHeaders()
    const { data } = await api.put<ApiResponse<PromotionCode>>(`/promotion-codes/${id}`, payload, { headers })
    codes.value = codes.value
      .map(code => (code.id === id ? data.data : code))
      .sort((a, b) => a.code.localeCompare(b.code))
    return data.data
  }

  async function remove(id: string) {
    const headers = await agentHeaders()
    await api.delete(`/promotion-codes/${id}`, { headers })
    codes.value = codes.value.filter(code => code.id !== id)
  }

  return { codes, loading, error, fetchAll, create, update, remove }
})

async function agentHeaders() {
  const token = await window.Clerk?.session?.getToken()
  if (!token)
    throw new Error('Please log in as an agent before managing promo codes.')
  return { Authorization: `Bearer ${token}` }
}
