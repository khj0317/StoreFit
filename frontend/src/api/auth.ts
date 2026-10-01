import { api } from '../lib/api'
import type {
  FindUsernameRequest,
  FindUsernameResponse,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
  SendCodeResponse,
  SignupRequest,
  SignupResponse,
  VerificationPurpose,
} from '../types'

export function signup(request: SignupRequest) {
  return api.post<SignupResponse>('/auth/signup', request).then((res) => res.data)
}

export function login(request: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', request).then((res) => res.data)
}

/** 다른 기기에 남은 로그인은 그대로 두고, 이 기기의 리프레시 토큰만 끊는다 */
export function logout(refreshToken: string | null) {
  return api.post<void>('/auth/logout', { refreshToken }).then(() => undefined)
}

export function sendPhoneCode(phoneNumber: string, purpose: VerificationPurpose) {
  return api.post<SendCodeResponse>('/auth/phone/send', { phoneNumber, purpose }).then((res) => res.data)
}

export function verifyPhoneCode(phoneNumber: string, purpose: VerificationPurpose, code: string) {
  return api
    .post<{ verificationToken: string }>('/auth/phone/verify', { phoneNumber, purpose, code })
    .then((res) => res.data.verificationToken)
}

export function findUsername(request: FindUsernameRequest) {
  return api.post<FindUsernameResponse>('/auth/find-username', request).then((res) => res.data)
}

export function resetPassword(request: ResetPasswordRequest) {
  return api.post<void>('/auth/reset-password', request).then(() => undefined)
}
