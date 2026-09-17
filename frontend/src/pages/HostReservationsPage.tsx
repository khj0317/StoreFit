import { useEffect, useState } from 'react'
import {
  cancelReservation,
  completeReservation,
  confirmReservation,
  getHostReservations,
  startReservation,
} from '../api/reservations'
import { StatusBadge } from '../components/StatusBadge'
import { getErrorMessage } from '../lib/api'
import type { Reservation } from '../types'

type Action = (reservationId: number) => Promise<Reservation>

export function HostReservationsPage() {
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  const load = () => {
    setStatus('loading')
    getHostReservations()
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

  const runAction = async (action: Action, reservationId: number) => {
    setActionError('')
    try {
      await action(reservationId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '처리에 실패했습니다.'))
    }
  }

  return (
    <section>
      <h1>호스트 예약 관리</h1>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {actionError && <p className="error-text">{actionError}</p>}
      {status === 'ready' && reservations.length === 0 && <p>들어온 예약이 없습니다.</p>}

      <ul className="list">
        {reservations.map((reservation) => (
          <li key={reservation.id} className="list-item">
            <div>
              <strong>{reservation.storeName}</strong>
              <p>예약자: {reservation.memberName}</p>
              <p>
                {reservation.startTime.replace('T', ' ')} ~ {reservation.endTime.replace('T', ' ')}
              </p>
              <p>
                짐 {reservation.luggageCount}개 · {reservation.totalPrice.toLocaleString()}원
              </p>
            </div>
            <div className="list-item-actions">
              <StatusBadge status={reservation.status} />
              {reservation.status === 'PENDING' && (
                <>
                  <button type="button" className="btn btn-primary" onClick={() => runAction(confirmReservation, reservation.id)}>
                    확정
                  </button>
                  <button type="button" className="btn btn-danger" onClick={() => runAction(cancelReservation, reservation.id)}>
                    취소
                  </button>
                </>
              )}
              {reservation.status === 'CONFIRMED' && (
                <>
                  <button type="button" className="btn btn-primary" onClick={() => runAction(startReservation, reservation.id)}>
                    보관 시작
                  </button>
                  <button type="button" className="btn btn-danger" onClick={() => runAction(cancelReservation, reservation.id)}>
                    취소
                  </button>
                </>
              )}
              {reservation.status === 'IN_PROGRESS' && (
                <button type="button" className="btn btn-primary" onClick={() => runAction(completeReservation, reservation.id)}>
                  보관 완료
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
