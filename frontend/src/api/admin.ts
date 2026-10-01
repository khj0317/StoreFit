import { api } from '../lib/api'
import type { AdminBranch, AdminNotification, ApplicationStatus, BranchApplication, BranchRequest } from '../types'

export function getAdminBranches() {
  return api.get<AdminBranch[]>('/admin/branches').then((res) => res.data)
}

export function createBranch(request: BranchRequest) {
  return api.post<AdminBranch>('/admin/branches', request).then((res) => res.data)
}

export function updateBranch(placeId: number, request: BranchRequest) {
  return api.put<AdminBranch>(`/admin/branches/${placeId}`, request).then((res) => res.data)
}

export function getApplications(status?: ApplicationStatus) {
  return api.get<BranchApplication[]>('/admin/applications', { params: status ? { status } : undefined }).then((res) => res.data)
}

export function approveApplication(applicationId: number) {
  return api.post<BranchApplication>(`/admin/applications/${applicationId}/approve`).then((res) => res.data)
}

export function rejectApplication(applicationId: number, reason: string) {
  return api.post<BranchApplication>(`/admin/applications/${applicationId}/reject`, { reason }).then((res) => res.data)
}

export function getNotifications() {
  return api.get<AdminNotification[]>('/admin/notifications').then((res) => res.data)
}
