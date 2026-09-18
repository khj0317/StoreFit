import { api } from '../lib/api'
import type { StoreMutationRequest, StoreRecord } from '../types'

export function getMyStores() {
  return api.get<StoreRecord[]>('/stores/me').then((res) => res.data)
}

export function getStore(storeId: number) {
  return api.get<StoreRecord>(`/stores/${storeId}`).then((res) => res.data)
}

export function createStore(request: StoreMutationRequest) {
  return api.post<StoreRecord>('/stores', request).then((res) => res.data)
}

export function updateStore(storeId: number, request: StoreMutationRequest) {
  return api.put<StoreRecord>(`/stores/${storeId}`, request).then((res) => res.data)
}

export function deleteStore(storeId: number) {
  return api.delete(`/stores/${storeId}`).then(() => undefined)
}

export function completeStore(storeId: number) {
  return api.patch<StoreRecord>(`/stores/${storeId}/complete`).then((res) => res.data)
}

export function cancelStore(storeId: number) {
  return api.patch<StoreRecord>(`/stores/${storeId}/cancel`).then((res) => res.data)
}
