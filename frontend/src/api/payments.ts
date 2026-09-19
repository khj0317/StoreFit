import { api } from '../lib/api'
import type { PaymentConfirmRequest, PaymentReadyResponse, PaymentResponse } from '../types'

export function getMyPayments() {
  return api.get<PaymentResponse[]>('/payments/me').then((res) => res.data)
}

export function readyPayment(storeId: number) {
  return api.post<PaymentReadyResponse>(`/payments/${storeId}/ready`).then((res) => res.data)
}

export function confirmPayment(request: PaymentConfirmRequest) {
  return api.post<PaymentResponse>('/payments/confirm', request).then((res) => res.data)
}
