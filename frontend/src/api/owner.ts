import { api } from '../lib/api'
import type { BranchApplication, BranchApplyRequest, OwnerDashboard, OwnerReservation, Place, PlaceRequest } from '../types'

export function getOwnerDashboard() {
  return api.get<OwnerDashboard>('/owner/dashboard').then((res) => res.data)
}

export function getMyPlaces() {
  return api.get<Place[]>('/owner/places').then((res) => res.data)
}

/** 아직 운영자가 없는 정식 지점 */
export function getAvailableBranches() {
  return api.get<Place[]>('/owner/branches').then((res) => res.data)
}

/** 지점 운영 신청 (본사 관리자가 승인하면 운영 시작) */
export function applyForBranch(placeId: number, request: BranchApplyRequest) {
  return api.post<BranchApplication>(`/owner/branches/${placeId}/applications`, request).then((res) => res.data)
}

export function getMyApplications() {
  return api.get<BranchApplication[]>('/owner/applications').then((res) => res.data)
}

export function updatePlace(placeId: number, request: PlaceRequest) {
  return api.put<Place>(`/owner/places/${placeId}`, request).then((res) => res.data)
}

export function getOwnerReservations() {
  return api.get<OwnerReservation[]>('/owner/reservations').then((res) => res.data)
}

export function lookupCheckIn(code: string) {
  return api.get<OwnerReservation>(`/owner/check-ins/${encodeURIComponent(code)}`).then((res) => res.data)
}

export function checkIn(code: string) {
  return api.post<OwnerReservation>(`/owner/check-ins/${encodeURIComponent(code)}/check-in`).then((res) => res.data)
}

export function checkOut(code: string) {
  return api.post<OwnerReservation>(`/owner/check-ins/${encodeURIComponent(code)}/check-out`).then((res) => res.data)
}
