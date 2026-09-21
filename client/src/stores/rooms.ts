import { isAxiosError } from 'axios'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api/client'

/** Types below match the Rooms section of docs/API.md. */
export type BedType = 'SINGLE' | 'DOUBLE' | 'KING' | 'TWIN'

export const BED_TYPE_LABELS: Record<BedType, string> = {
  SINGLE: 'Single bed',
  DOUBLE: 'Double bed',
  KING: 'Double bed (king size)',
  TWIN: 'Twin bed',
}

export interface RoomImage {
  id: string
  url: string
}

export interface RoomResponse {
  id: string
  name: string
  bedType: BedType
  sizeSqm: number
  capacity: number
  pricePerNight: number
  promotionPrice: number | null
  description: string
  amenities: string[]
  mainImage: RoomImage | null
  gallery: RoomImage[]
  createdAt: string
  updatedAt: string
}

export interface RoomSummary {
  id: string
  name: string
  mainImageUrl: string | null
  pricePerNight: number
  promotionPrice: number | null
  capacity: number
  bedType: BedType
  sizeSqm: number
}

/** Row of `GET /api/rooms/available`. */
export interface AvailableRoom extends RoomSummary {
  description: string
  availableUnits: number
}

export interface PageResponse<T> {
  content: T[]
  /** Zero-based. */
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface RoomRequest {
  name: string
  bedType: BedType
  sizeSqm: number
  capacity: number
  pricePerNight: number
  promotionPrice: number | null
  description: string
  amenities: string[]
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// Override the instance's JSON default so axios sends real multipart data.
const multipart = { headers: { 'Content-Type': 'multipart/form-data' } }

/**
 * Field errors from a 400 `Validation failed` response, keyed by request field
 * (`amenities[0]` becomes `amenities`, the promotion price check becomes `promotionPrice`).
 */
export function apiFieldErrors(error: unknown): Record<string, string> {
  const details: unknown = isAxiosError(error) ? error.response?.data?.details : undefined
  const errors: Record<string, string> = {}
  if (!Array.isArray(details))
    return errors
  for (const detail of details) {
    const match = /^([\w.]+?)(?:\[\d+\])?: (.+)$/.exec(String(detail))
    if (!match)
      continue
    const field = match[1] === 'promotionPriceValid' ? 'promotionPrice' : match[1]!
    errors[field] ??= match[2]!
  }
  return errors
}

export const useRoomsStore = defineStore('rooms', () => {
  // Room type options for the search filter; the list is small and rarely changes, so fetch it once.
  const roomTypes = ref<RoomSummary[]>([])
  let roomTypesRequest: Promise<RoomSummary[]> | null = null

  function types() {
    roomTypesRequest ??= list({ search: '', page: 0, size: 50 })
      .then((page) => {
        roomTypes.value = page.content
        return page.content
      })
      .catch((error) => {
        roomTypesRequest = null
        throw error
      })
    return roomTypesRequest
  }

  /** Pass `signal` to cancel a superseded request (e.g. while typing a search). */
  async function list(params: { search: string, page: number, size: number }, options: { signal?: AbortSignal } = {}) {
    const { data } = await api.get<ApiResponse<PageResponse<RoomSummary>>>('/rooms', { params, signal: options.signal })
    return data.data
  }

  /** Dates are ISO `YYYY-MM-DD`. Empty array when nothing is available. */
  async function available(params: { checkIn: string, checkOut: string, rooms: number, guests: number, roomTypeIds?: string[] }) {
    // Repeatable param: ?roomTypeIds=<id>&roomTypeIds=<id>
    const { data } = await api.get<ApiResponse<AvailableRoom[]>>('/rooms/available', {
      params,
      paramsSerializer: { indexes: null },
    })
    return data.data
  }

  async function get(id: string) {
    const { data } = await api.get<ApiResponse<RoomResponse>>(`/rooms/${id}`)
    return data.data
  }

  async function create(room: RoomRequest, mainImage: File, gallery: File[]) {
    const body = new FormData()
    body.append('room', new Blob([JSON.stringify(room)], { type: 'application/json' }))
    body.append('mainImage', mainImage)
    gallery.forEach(file => body.append('gallery', file))
    const { data } = await api.post<ApiResponse<RoomResponse>>('/rooms', body, multipart)
    return data.data
  }

  async function update(id: string, room: RoomRequest) {
    const { data } = await api.put<ApiResponse<RoomResponse>>(`/rooms/${id}`, room)
    return data.data
  }

  async function remove(id: string) {
    await api.delete(`/rooms/${id}`)
  }

  async function uploadImage(id: string, file: File, main: boolean) {
    const body = new FormData()
    body.append('file', file)
    const { data } = await api.post<ApiResponse<RoomResponse>>(`/rooms/${id}/images`, body, { ...multipart, params: { main } })
    return data.data
  }

  async function removeImage(id: string, imageId: string) {
    const { data } = await api.delete<ApiResponse<RoomResponse>>(`/rooms/${id}/images/${imageId}`)
    return data.data
  }

  async function reorderImages(id: string, imageIds: string[]) {
    const { data } = await api.put<ApiResponse<RoomResponse>>(`/rooms/${id}/images/order`, { imageIds })
    return data.data
  }

  return { roomTypes, types, list, available, get, create, update, remove, uploadImage, removeImage, reorderImages }
})
