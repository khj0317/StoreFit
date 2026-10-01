import { api } from '../lib/api'
import type { Place } from '../types'

/** 기간을 주면 보관소마다 그 기간에 남은 자리(remaining)를 함께 받는다 */
export function getPlaces(startDate?: string, endDate?: string) {
  const params = startDate && endDate ? { startDate, endDate } : undefined
  return api.get<Place[]>('/places', { params }).then((res) => res.data)
}
