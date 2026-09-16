import { api } from '@/api/client'

/** Reads the Supabase-backed profile belonging to the authenticated Clerk subject. */
export async function hasAgentRole(token: string) {
  const { data } = await api.get<{ success: boolean, data: { role: string } }>('/profiles/me', {
    headers: { Authorization: `Bearer ${token}` },
    timeout: 10_000,
  })
  return data.success && data.data?.role === 'agent'
}
