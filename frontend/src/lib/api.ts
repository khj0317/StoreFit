import axios, { type AxiosRequestConfig } from 'axios'
import type { LoginResponse, MemberRole } from '../types'

const AUTH_STORAGE_KEY = 'luggage-storage-auth'

export interface StoredAuth {
  accessToken: string
  refreshToken: string | null
  username: string
  name: string
  role: MemberRole
}

export function loadStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as Partial<StoredAuth>
    if (!parsed.accessToken || !parsed.username) return null
    // 역할·리프레시 토큰이 생기기 전에 저장된 로그인 정보도 읽을 수 있게 기본값을 채운다
    return {
      accessToken: parsed.accessToken,
      refreshToken: parsed.refreshToken ?? null,
      username: parsed.username,
      name: parsed.name ?? '',
      role: parsed.role ?? 'USER',
    }
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

export function toStoredAuth(response: LoginResponse): StoredAuth {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    username: response.username,
    name: response.name,
    role: response.role,
  }
}

/**
 * 배포에서는 Vercel이 /api 요청을 백엔드(Render)로 넘겨준다(vercel.json). 다른 주소를 쓰고 싶을 때만
 * VITE_API_BASE_URL을 넣는다 (예: https://storefit-api.onrender.com/api).
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
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
  code: string
  message: string
}

// 액세스 토큰이 만료되면 리프레시 토큰으로 한 번만 새로 받고 원래 요청을 다시 보낸다.
// 여러 요청이 동시에 만료돼도 갱신은 한 번만 하도록 진행 중인 요청을 함께 기다린다.
let refreshing: Promise<StoredAuth | null> | null = null

async function refreshAuth(): Promise<StoredAuth | null> {
  const current = loadStoredAuth()
  if (!current?.refreshToken) return null
  try {
    const { data } = await axios.post<LoginResponse>(`${api.defaults.baseURL}/auth/refresh`, {
      refreshToken: current.refreshToken,
    })
    const next = toStoredAuth(data)
    saveStoredAuth(next)
    window.dispatchEvent(new Event('storefit-auth-refreshed'))
    return next
  } catch {
    return null
  }
}

function forceLogout() {
  clearStoredAuth()
  if (window.location.pathname !== '/login') {
    window.alert('로그인이 만료되었습니다. 다시 로그인해주세요.')
    window.location.href = '/login'
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (axios.isAxiosError<ApiErrorResponse>(error) && error.response?.data?.code === 'INVALID_TOKEN') {
      const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
      if (original && !original._retried) {
        original._retried = true
        refreshing ??= refreshAuth().finally(() => {
          refreshing = null
        })
        const next = await refreshing
        if (next) {
          original.headers = { ...original.headers, Authorization: `Bearer ${next.accessToken}` }
          return api.request(original)
        }
      }
      forceLogout()
    }
    return Promise.reject(error)
  },
)

const BACKEND_UNREACHABLE_MESSAGE = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'

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
