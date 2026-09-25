import { api } from '@/api/client'

/** Matches ProfileResponse in docs/API.md. Fields are null until the guest saves. */
export interface Profile {
  clerkUserId: string
  firstName: string | null
  lastName: string | null
  phoneNumber: string | null
  dateOfBirth: string | null
  country: string | null
  profilePicture: string | null
  role: string
  createdAt: string | null
  updatedAt: string | null
}

export interface ProfileUpdate {
  firstName: string
  lastName: string
  phoneNumber: string
  dateOfBirth: string
  country: string
  profilePicture?: string | null
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export async function getProfile() {
  const { data } = await api.get<ApiResponse<Profile>>('/profiles/me')
  return data.data
}

/** `clerkSynced` is false when the Clerk mirror failed and null when it is not configured. */
export async function updateProfile(body: ProfileUpdate) {
  const { data } = await api.put<ApiResponse<{ profile: Profile, clerkSynced: boolean | null }>>('/profiles/me', body)
  return data.data
}
