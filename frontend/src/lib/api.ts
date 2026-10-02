import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import type { LoginResponse, MemberRole } from '../types'
import { whenServerSettled } from './serverWake'

const AUTH_STORAGE_KEY = 'luggage-storage-auth'

export interface StoredAuth {
  accessToken: string
  refreshToken: string | null
  username: string
  name: string
  role: MemberRole
  demo: boolean
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
      demo: parsed.demo ?? false,
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
    demo: response.demo ?? false,
  }
}

/**
 * 배포에서는 Vercel이 /api 요청을 백엔드(Render)로 넘겨준다(vercel.json). 다른 주소를 쓰고 싶을 때만
 * VITE_API_BASE_URL을 넣는다 (예: https://storefit-api.onrender.com/api).
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
})

api.interceptors.request.use(async (config) => {
  // 잠든 배포 서버가 깨어나는 중이면 요청을 잠깐 미뤘다가 보낸다 (lib/serverWake.ts)
  await whenServerSettled()
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

// 서버가 막 깨어나는 중에는 게이트웨이 오류(502·503·504)나 연결 실패가 잠깐 날 수 있다.
// 다시 보내도 안전한 조회(GET)만 몇 번 더 시도한다.
const RETRYABLE_STATUS = new Set([502, 503, 504])
const MAX_GET_RETRIES = 2

function isRetryableGet(error: unknown): error is AxiosError & { config: AxiosRequestConfig & { _retryCount?: number } } {
  if (!axios.isAxiosError(error) || !error.config) return false
  if ((error.config.method ?? 'get').toLowerCase() !== 'get') return false
  if (error.code === 'ERR_CANCELED') return false
  return !error.response || RETRYABLE_STATUS.has(error.response.status)
}

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (isRetryableGet(error)) {
      const config = error.config
      config._retryCount = (config._retryCount ?? 0) + 1
      if (config._retryCount <= MAX_GET_RETRIES) {
        await new Promise((resolve) => window.setTimeout(resolve, 3000))
        return api.request(config)
      }
    }
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
