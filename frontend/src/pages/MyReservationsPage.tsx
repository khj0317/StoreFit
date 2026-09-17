import { type FormEvent, useEffect, useState } from 'react'
import { cancelReservation, getMyReservations } from '../api/reservations'
import { createReview } from '../api/reviews'
import { StatusBadge } from '../components/StatusBadge'
import { getErrorMessage } from '../lib/api'
import type { Reservation } from '../types'

function ReviewForm({ reservation }: { reservation: Reservation }) {
  const [rating, setRating] = useState('5')
  const [content, setContent] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await createReview({ reservationId: reservation.id, rating: Number(rating), content })
      setMessage('리뷰가 등록되었습니다.')
      setDone(true)
    } catch (err) {
      setError(getErrorMessage(err, '리뷰 등록에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return <p className="success-text">{message}</p>
  }

  return (
    <form className="form form-inline" onSubmit={handleSubmit}>
      <label className="form-group">
        <span>평점</span>
        <select value={rating} onChange={(e) => setRating(e.target.value)}>
          {[5, 4, 3, 2, 1].map((value) => (
            <option key={value} value={value}>
              {value}점
            </option>
          ))}
        </select>
      </label>
      <label className="form-group">
        <span>리뷰 내용</span>
        <input value={content} onChange={(e) => setContent(e.target.value)} />
      </label>
      {error && <p className="error-text">{error}</p>}
      <button type="submit" className="btn btn-ghost" disabled={submitting}>
        {submitting ? '등록 중...' : '리뷰 작성'}
      </button>
    </form>
  )
}

export function MyReservationsPage() {
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  const load = () => {
    setStatus('loading')
    getMyReservations()
      .then((data) => {
        setReservations(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '예약 목록을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }

  useEffect(load, [])

  const handleCancel = async (reservationId: number) => {
    setActionError('')
    try {
      await cancelReservation(reservationId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '취소에 실패했습니다.'))
    }
  }

  return (
    <section>
      <h1>내 예약</h1>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {actionError && <p className="error-text">{actionError}</p>}
      {status === 'ready' && reservations.length === 0 && <p>예약 내역이 없습니다.</p>}

      <ul className="list">
        {reservations.map((reservation) => (
          <li key={reservation.id} className="list-item list-item-column">
            <div className="list-item-row">
              <div>
                <strong>{reservation.storeName}</strong>
                <p>
                  {reservation.startTime.replace('T', ' ')} ~ {reservation.endTime.replace('T', ' ')}
                </p>
                <p>
                  짐 {reservation.luggageCount}개
                </p>
              </div>
              <div className="list-item-actions">
                <StatusBadge status={reservation.status} />
                {(reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') && (
                  <button type="button" className="btn btn-danger" onClick={() => handleCancel(reservation.id)}>
                    예약 취소
                  </button>
                )}
              </div>
            </div>
            {reservation.status === 'COMPLETED' && <ReviewForm reservation={reservation} />}
          </li>
        ))}
      </ul>
    </section>
  )
}
