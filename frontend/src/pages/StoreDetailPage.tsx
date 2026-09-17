import { type FormEvent, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { createReservation } from '../api/reservations'
import { getStoreReviews } from '../api/reviews'
import { getStore } from '../api/stores'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../lib/api'
import type { Reservation, Review, StoreDetail } from '../types'

export function StoreDetailPage() {
  const { storeId } = useParams<{ storeId: string }>()
  const { isAuthenticated } = useAuth()
  const id = Number(storeId)

  const [store, setStore] = useState<StoreDetail | null>(null)
  const [reviews, setReviews] = useState<Review[]>([])
  const [loadError, setLoadError] = useState('')

  const [luggageCount, setLuggageCount] = useState('1')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [reserveError, setReserveError] = useState('')
  const [reserveResult, setReserveResult] = useState<Reservation | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    Promise.all([getStore(id), getStoreReviews(id)])
      .then(([storeData, reviewData]) => {
        setStore(storeData)
        setReviews(reviewData)
      })
      .catch((err: unknown) => setLoadError(getErrorMessage(err, '보관소 정보를 불러오지 못했습니다.')))
  }, [id])

  const handleReserve = async (event: FormEvent) => {
    event.preventDefault()
    setReserveError('')
    setReserveResult(null)
    setSubmitting(true)
    try {
      const reservation = await createReservation({
        storeId: id,
        luggageCount: Number(luggageCount),
        startTime,
        endTime,
      })
      setReserveResult(reservation)
    } catch (err) {
      setReserveError(getErrorMessage(err, '예약에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (loadError) {
    return <p className="error-text">{loadError}</p>
  }

  if (!store) {
    return <p>불러오는 중...</p>
  }

  return (
    <section>
      <div className="store-detail-header">
        <h1>{store.name}</h1>
        <p className="store-card-address">{store.address}</p>
        <p>
          시간당 {store.pricePerHour.toLocaleString()}원 · 수용 {store.capacity}개 · 호스트 {store.hostName}
        </p>
        {store.openTime && store.closeTime && (
          <p>
            운영 시간 {store.openTime.slice(0, 5)} ~ {store.closeTime.slice(0, 5)}
          </p>
        )}
        {store.description && <p>{store.description}</p>}
      </div>

      {store.imageUrls.length > 0 && (
        <div className="store-detail-images">
          {store.imageUrls.map((url) => (
            <img key={url} src={url} alt={store.name} />
          ))}
        </div>
      )}

      <div className="store-detail-columns">
        <div className="card">
          <h2>예약하기</h2>
          {!isAuthenticated && <p>예약하려면 로그인이 필요합니다.</p>}
          {isAuthenticated && (
            <form className="form" onSubmit={handleReserve}>
              <label className="form-group">
                <span>짐 개수</span>
                <input type="number" min={1} value={luggageCount} onChange={(e) => setLuggageCount(e.target.value)} required />
              </label>
              <label className="form-group">
                <span>시작 시간</span>
                <input type="datetime-local" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
              </label>
              <label className="form-group">
                <span>종료 시간</span>
                <input type="datetime-local" value={endTime} onChange={(e) => setEndTime(e.target.value)} required />
              </label>

              {reserveError && <p className="error-text">{reserveError}</p>}
              {reserveResult && (
                <p className="success-text">
                  예약이 생성되었습니다. 총 금액: {reserveResult.totalPrice.toLocaleString()}원
                </p>
              )}

              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? '예약 중...' : '예약하기'}
              </button>
            </form>
          )}
        </div>

        <div className="card">
          <h2>리뷰 ({reviews.length})</h2>
          {reviews.length === 0 && <p>아직 작성된 리뷰가 없습니다.</p>}
          <ul className="review-list">
            {reviews.map((review) => (
              <li key={review.id} className="review-item">
                <div className="review-item-header">
                  <span>{'★'.repeat(review.rating)}</span>
                  <strong>{review.memberName}</strong>
                </div>
                {review.content && <p>{review.content}</p>}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}
