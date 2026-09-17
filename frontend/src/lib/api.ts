import axios from 'axios'

const AUTH_STORAGE_KEY = 'luggage-storage-auth'

export interface StoredAuth {
  accessToken: string
  email: string
  name: string
}

export function loadStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredAuth
  } catch {
    return null
  }
}

export function saveStoredAuth(auth: StoredAuth) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
}

export function clearStoredAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

export const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const auth = loadStoredAuth()
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

export interface ApiErrorResponse {
  status: number
  message: string
}

export function getErrorMessage(error: unknown, fallback = '요청 처리 중 오류가 발생했습니다.'): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return fallback
}
