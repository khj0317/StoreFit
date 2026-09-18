import { api } from '../lib/api'
import type { ReviewCreateRequest, ReviewSummary } from '../types'

export function createReview(request: ReviewCreateRequest) {
  return api.post<ReviewSummary>('/reviews', request).then((res) => res.data)
}
