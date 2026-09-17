import { api } from '../lib/api'
import type { StoreDetail, StoreMutationRequest, StoreSummary } from '../types'

export function getStores() {
  return api.get<StoreSummary[]>('/stores').then((res) => res.data)
}

export function getMyStores() {
  return api.get<StoreSummary[]>('/stores/me').then((res) => res.data)
}

export function getStore(storeId: number) {
  return api.get<StoreDetail>(`/stores/${storeId}`).then((res) => res.data)
}

export function createStore(request: StoreMutationRequest) {
  return api.post<StoreDetail>('/stores', request).then((res) => res.data)
}

export function updateStore(storeId: number, request: StoreMutationRequest) {
  return api.put<StoreDetail>(`/stores/${storeId}`, request).then((res) => res.data)
}

export function deleteStore(storeId: number) {
  return api.delete(`/stores/${storeId}`).then(() => undefined)
}
