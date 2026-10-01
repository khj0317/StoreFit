import type { ReactNode } from 'react'
import { STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import type { OwnerReservation } from '../types'
import { CategoryIcon } from './CategoryIcon'
import { StatusBadge } from './StatusBadge'

export function OwnerReservationRow({ reservation, action }: { reservation: OwnerReservation; action?: ReactNode }) {
  return (
    <li className="row-card">
      <CategoryIcon category={reservation.category} size="sm" />
      <div className="row-card-body">
        <strong>
          {reservation.customerName} · {reservation.name}
        </strong>
        <span className="row-card-sub">
          {reservation.placeName} · {STORE_CATEGORY_LABELS[reservation.category]} {reservation.luggageCount}개 ·{' '}
          {reservation.startDate} ~ {reservation.endDate}
        </span>
        {reservation.customerPhone && <p className="row-card-mono">{reservation.customerPhone}</p>}
        {reservation.overdueDays > 0 && (
          <p className="deadline-note">
            {reservation.overdueDays}일 연체 · 연체료 {reservation.overdueFee.toLocaleString()}원 (현장 결제)
          </p>
        )}
      </div>
      <div className="row-card-end">
        <StatusBadge status={reservation.status} paid={reservation.paid} />
        {action}
      </div>
    </li>
  )
}
