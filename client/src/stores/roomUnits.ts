import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/api/client'
import type { BedType } from '@/stores/rooms'
import { apiErrorMessage } from '@/stores/hotel'

/** Matches `RoomUnitResponse` in docs/API.md. */
export interface RoomUnitResponse {
  id: string
  roomNumber: string
  floor: number
  roomTypeId: string
  roomTypeName: string
  bedType: BedType
  statusCode: string
  statusLabel: string
  occupied: boolean
  displayStatus: string
}

export interface RoomStatusOption {
  id: string
  code: string
  label: string
  sortOrder: number
}

export interface RoomUnitPayload {
  roomNumber: string
  roomTypeId: string
  statusCode: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export const useRoomUnitsStore = defineStore('roomUnits', () => {
  const units = ref<RoomUnitResponse[]>([])
  const statuses = ref<RoomStatusOption[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const sortedUnits = computed(() =>
    [...units.value].sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true })),
  )

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get<ApiResponse<RoomUnitResponse[]>>('/room-units')
      units.value = data.data ?? []
    }
    catch (e) {
      error.value = apiErrorMessage(e, 'Could not load rooms.')
      throw e
    }
    finally {
      loading.value = false
    }
  }

  async function fetchStatuses() {
    const { data } = await api.get<ApiResponse<RoomStatusOption[]>>('/room-units/statuses')
    statuses.value = data.data ?? []
  }

  async function create(payload: RoomUnitPayload) {
    const { data } = await api.post<ApiResponse<RoomUnitResponse>>('/room-units', payload)
    units.value = [...units.value, data.data]
    return data.data
  }

  async function update(id: string, payload: RoomUnitPayload) {
    const { data } = await api.put<ApiResponse<RoomUnitResponse>>(`/room-units/${id}`, payload)
    units.value = units.value.map(unit => (unit.id === id ? data.data : unit))
    return data.data
  }

  async function remove(id: string) {
    await api.delete(`/room-units/${id}`)
    units.value = units.value.filter(unit => unit.id !== id)
  }

  return {
    units,
    sortedUnits,
    statuses,
    loading,
    error,
    fetchAll,
    fetchStatuses,
    create,
    update,
    remove,
  }
})
