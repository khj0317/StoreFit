import { api } from '../lib/api'
import type { ChangePasswordRequest, DeleteAccountRequest, MemberProfile, UpdateProfileRequest } from '../types'

export function getMyProfile() {
  return api.get<MemberProfile>('/members/me').then((res) => res.data)
}

export function updateProfile(request: UpdateProfileRequest) {
  return api.patch<MemberProfile>('/members/me', request).then((res) => res.data)
}

export function changePassword(request: ChangePasswordRequest) {
  return api.post<void>('/members/me/password', request).then(() => undefined)
}

export function deleteAccount(request: DeleteAccountRequest) {
  return api.delete('/members/me', { data: request }).then(() => undefined)
}
