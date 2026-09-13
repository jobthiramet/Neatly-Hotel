import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function fetchHealth() {
  const { data } = await api.get<{ status: string; service: string }>('/health')
  return data
}
