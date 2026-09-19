import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { clearStoredAuth, loadStoredAuth, saveStoredAuth, type StoredAuth } from '../lib/api'
import type { SignupRequest } from '../types'

interface AuthContextValue {
  user: StoredAuth | null
  isAuthenticated: boolean
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
    const auth: StoredAuth = {
      accessToken: response.accessToken,
      username: response.username,
      name: response.name,
    }
    saveStoredAuth(auth)
    setUser(auth)
  }

  const signup = async (request: SignupRequest) => {
    await authApi.signup(request)
    await login(request.username, request.password)
  }

  const logout = () => {
    clearStoredAuth()
    setUser(null)
  }

  const updateDisplayName = (name: string) => {
    setUser((current) => {
      if (!current) return current
      const updated = { ...current, name }
      saveStoredAuth(updated)
      return updated
    })
  }

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, login, signup, logout, updateDisplayName }),
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
