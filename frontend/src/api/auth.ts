import { api } from '../lib/api'
import type {
  FindUsernameRequest,
  FindUsernameResponse,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
  SignupRequest,
  SignupResponse,
} from '../types'

export function signup(request: SignupRequest) {
  return api.post<SignupResponse>('/auth/signup', request).then((res) => res.data)
}

export function login(request: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', request).then((res) => res.data)
}

export function findUsername(request: FindUsernameRequest) {
  return api.post<FindUsernameResponse>('/auth/find-username', request).then((res) => res.data)
}

export function resetPassword(request: ResetPasswordRequest) {
  return api.post<void>('/auth/reset-password', request).then(() => undefined)
}
