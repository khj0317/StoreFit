import { api } from '../lib/api'
import type { RefundPreview, StoreMutationRequest, StoreRecord } from '../types'

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

/** 결제 전 예약 삭제 */
export function deleteStore(storeId: number) {
  return api.delete(`/stores/${storeId}`).then(() => undefined)
}

export function getRefundPreview(storeId: number) {
  return api.get<RefundPreview>(`/stores/${storeId}/refund-preview`).then((res) => res.data)
}

/** 결제한 예약 취소 + 환불 */
export function cancelStore(storeId: number) {
  return api.post<StoreRecord>(`/stores/${storeId}/cancel`).then((res) => res.data)
}
