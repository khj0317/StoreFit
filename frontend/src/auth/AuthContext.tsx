import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { clearStoredAuth, loadStoredAuth, saveStoredAuth, toStoredAuth, type StoredAuth } from '../lib/api'
import type { SignupRequest } from '../types'

interface AuthContextValue {
  user: StoredAuth | null
  isAuthenticated: boolean
  isOwner: boolean
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  signup: (request: SignupRequest) => Promise<void>
  logout: () => void
  updateDisplayName: (name: string) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredAuth | null>(() => loadStoredAuth())

  const login = async (username: string, password: string) => {
    const response = await authApi.login({ username, password })
    const auth = toStoredAuth(response)
    saveStoredAuth(auth)
    setUser(auth)
  }

  const signup = async (request: SignupRequest) => {
    await authApi.signup(request)
    await login(request.username, request.password)
  }

  const logout = () => {
    // 서버의 리프레시 토큰도 끊는다 (실패해도 이 기기에서는 로그아웃된다)
    authApi.logout(loadStoredAuth()?.refreshToken ?? null).catch(() => undefined)
    clearStoredAuth()
    setUser(null)
  }

  // 토큰이 조용히 갱신되면(lib/api) 화면이 들고 있는 로그인 정보도 맞춘다
  useEffect(() => {
    const sync = () => setUser(loadStoredAuth())
    window.addEventListener('storefit-auth-refreshed', sync)
    return () => window.removeEventListener('storefit-auth-refreshed', sync)
  }, [])

  const updateDisplayName = (name: string) => {
    setUser((current) => {
      if (!current) return current
      const updated = { ...current, name }
      saveStoredAuth(updated)
      return updated
    })
  }

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, isOwner: user?.role === 'OWNER', isAdmin: user?.role === 'ADMIN', login, signup, logout, updateDisplayName }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
