import { api } from '../lib/api'
import type { Review, ReviewCreateRequest } from '../types'

export function createReview(request: ReviewCreateRequest) {
  return api.post<Review>('/reviews', request).then((res) => res.data)
}

export function getStoreReviews(storeId: number) {
  return api.get<Review[]>(`/stores/${storeId}/reviews`).then((res) => res.data)
}
