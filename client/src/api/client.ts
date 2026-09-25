import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Attach the Clerk session token when there is one. Callers that set their own
// Authorization header (e.g. the agent role check) keep it.
api.interceptors.request.use(async (config) => {
  if (config.headers.Authorization)
    return config
  const token = await window.Clerk?.session?.getToken().catch(() => null)
  if (token)
    config.headers.Authorization = `Bearer ${token}`
  return config
})

export async function fetchHealth() {
  const { data } = await api.get<{ status: string; service: string }>('/health')
  return data
}
