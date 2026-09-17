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

const BACKEND_UNREACHABLE_MESSAGE = '백엔드 서버에 연결할 수 없습니다. 백엔드(http://localhost:8080)가 실행 중인지 확인해주세요.'

export function getErrorMessage(error: unknown, fallback = '요청 처리 중 오류가 발생했습니다.'): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const message = error.response?.data?.message
    if (message) return message
    if (!error.response || error.response.status >= 500) {
      return BACKEND_UNREACHABLE_MESSAGE
    }
    return `${fallback} (HTTP ${error.response.status})`
  }
  if (error instanceof Error) {
    return `${fallback} (${error.message})`
  }
  return fallback
}
