import { api } from '../lib/api'
import type { LoginRequest, LoginResponse, SignupRequest, SignupResponse } from '../types'

export function signup(request: SignupRequest) {
  return api.post<SignupResponse>('/auth/signup', request).then((res) => res.data)
}

export function login(request: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', request).then((res) => res.data)
}
