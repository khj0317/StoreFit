import { api } from '../lib/api'
import type { Reservation, ReservationCreateRequest } from '../types'

export function createReservation(request: ReservationCreateRequest) {
  return api.post<Reservation>('/reservations', request).then((res) => res.data)
}

export function getMyReservations() {
  return api.get<Reservation[]>('/reservations/me').then((res) => res.data)
}

export function getReservation(reservationId: number) {
  return api.get<Reservation>(`/reservations/${reservationId}`).then((res) => res.data)
}

export function completeReservation(reservationId: number) {
  return api.patch<Reservation>(`/reservations/${reservationId}/complete`).then((res) => res.data)
}

export function cancelReservation(reservationId: number) {
  return api.patch<Reservation>(`/reservations/${reservationId}/cancel`).then((res) => res.data)
}
