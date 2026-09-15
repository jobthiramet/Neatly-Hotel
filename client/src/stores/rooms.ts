import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/api/client'
import type { RoomStatus } from '@/components/ui/badge'
import { apiErrorMessage } from '@/stores/hotel'

/** Matches `RoomResponse` in docs/API.md. */
export interface RoomResponse {
  id: string
  roomNumber: string
  roomType: string
  bedType: string
  status: RoomStatus | string
  pricePerNight: number
  capacity: number
  active: boolean
}

export interface CreateRoomPayload {
  roomNumber: string
  roomType: string
  bedType: string
  status: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export const useRoomsStore = defineStore('rooms', () => {
  const rooms = ref<RoomResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const sortedRooms = computed(() =>
    [...rooms.value].sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true })),
  )

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get<ApiResponse<RoomResponse[]>>('/rooms')
      rooms.value = data.data ?? []
    }
    catch (e) {
      error.value = apiErrorMessage(e, 'Could not load rooms.')
      throw e
    }
    finally {
      loading.value = false
    }
  }

  async function create(payload: CreateRoomPayload) {
    const { data } = await api.post<ApiResponse<RoomResponse>>('/rooms', payload)
    rooms.value = [...rooms.value, data.data]
    return data.data
  }

  return { rooms, sortedRooms, loading, error, fetchAll, create }
})
