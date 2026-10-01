import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOwnerDashboard, getOwnerReservations } from '../../api/owner'
import { EmptyState, Loading } from '../../components/Feedback'
import { BoxIcon, CardIcon, ClockIcon, PlusIcon } from '../../components/Icons'
import { OwnerReservationRow } from '../../components/OwnerReservationRow'
import { getErrorMessage } from '../../lib/api'
import type { OwnerDashboard, OwnerReservation } from '../../types'

export function OwnerDashboardPage() {
  const [dashboard, setDashboard] = useState<OwnerDashboard | null>(null)
  const [reservations, setReservations] = useState<OwnerReservation[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getOwnerDashboard(), getOwnerReservations()])
      .then(([dashboardData, reservationData]) => {
        setDashboard(dashboardData)
        setReservations(reservationData)
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '대시보드를 불러오지 못했습니다.')))
  }, [])

  if (error) return <p className="error-text">{error}</p>
  if (!dashboard) return <Loading />

  if (dashboard.places.length === 0) {
    return (
      <EmptyState
        title="아직 운영하는 지점이 없어요"
        description="스토어핏 정식 지점을 하나 맡으면 이용자가 예약할 수 있어요."
        action={
          <Link to="/owner/places" className="btn btn-primary">
            <PlusIcon size={16} /> 운영할 지점 고르기
          </Link>
        }
      />
    )
  }

  const upcoming = reservations.filter(
    (reservation) => !dashboard.arrivals.some((arrival) => arrival.id === reservation.id)
      && !dashboard.departures.some((departure) => departure.id === reservation.id),
  )

  return (
    <section>
      <div className="page-header">
        <div>
          <span className="page-eyebrow">Owner · {dashboard.today}</span>
          <h1>운영 대시보드</h1>
        </div>
        <div className="page-header-actions">
          <Link to="/owner/scan" className="btn btn-primary btn-sm">
            QR 체크인 열기
          </Link>
        </div>
      </div>

      <div className="stat-grid stat-grid-4">
        <div className="stat-card">
          <span className="stat-icon tone-accent">
            <ClockIcon size={22} />
          </span>
          <div>
            <span className="stat-label">오늘 입고 예정</span>
            <span className="stat-value">
              {dashboard.arrivals.length}
              <small>건</small>
            </span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon tone-peach">
            <ClockIcon size={22} />
          </span>
          <div>
            <span className="stat-label">오늘 출고 예정</span>
            <span className="stat-value">
              {dashboard.departures.length}
              <small>건</small>
            </span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon tone-mint">
            <BoxIcon size={22} />
          </span>
          <div>
            <span className="stat-label">보관 중인 짐</span>
            <span className="stat-value">
              {dashboard.storedLuggageCount}
              <small>개</small>
            </span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon tone-butter">
            <CardIcon size={22} />
          </span>
          <div>
            <span className="stat-label">이번 달 매출</span>
            <span className="stat-value">
              {dashboard.monthRevenue.toLocaleString()}
              <small>원</small>
            </span>
          </div>
        </div>
      </div>

      <div className="section-header">
        <h2>지점 현황</h2>
        <Link to="/owner/places" className="link-more">
          관리하기
        </Link>
      </div>
      <ul className="occupancy-list">
        {dashboard.places.map((place) => {
          const ratio = Math.min(100, Math.round((place.occupiedToday / place.capacity) * 100))
          return (
            <li key={place.id} className="occupancy-card">
              <div className="occupancy-head">
                <strong>{place.name}</strong>
                <span>
                  오늘 {place.occupiedToday} / {place.capacity}자리
                </span>
              </div>
              <div className="meter" role="meter" aria-valuemin={0} aria-valuemax={place.capacity} aria-valuenow={place.occupiedToday}>
                <span className={ratio >= 90 ? 'meter-fill meter-fill-full' : 'meter-fill'} style={{ width: `${ratio}%` }} />
              </div>
            </li>
          )
        })}
      </ul>

      <div className="section-header">
        <h2>오늘 맡기러 와요</h2>
      </div>
      {dashboard.arrivals.length === 0 ? (
        <p className="place-hint">오늘 입고 예정인 짐이 없어요.</p>
      ) : (
        <ul className="list">
          {dashboard.arrivals.map((reservation) => (
            <OwnerReservationRow key={reservation.id} reservation={reservation} />
          ))}
        </ul>
      )}

      <div className="section-header">
        <h2>오늘까지 찾아가요</h2>
      </div>
      {dashboard.departures.length === 0 ? (
        <p className="place-hint">오늘 출고 예정인 짐이 없어요.</p>
      ) : (
        <ul className="list">
          {dashboard.departures.map((reservation) => (
            <OwnerReservationRow key={reservation.id} reservation={reservation} />
          ))}
        </ul>
      )}

      <div className="section-header">
        <h2>다가오는 예약</h2>
      </div>
      {upcoming.length === 0 ? (
        <p className="place-hint">이후 예약이 아직 없어요.</p>
      ) : (
        <ul className="list">
          {upcoming.map((reservation) => (
            <OwnerReservationRow key={reservation.id} reservation={reservation} />
          ))}
        </ul>
      )}
    </section>
  )
}
