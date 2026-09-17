import type { ReservationStatus } from '../types'

const LABELS: Record<ReservationStatus, string> = {
  PENDING: '이용중',
  COMPLETED: '완료됨',
  CANCELLED: '취소됨',
}

export function StatusBadge({ status }: { status: ReservationStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>
}
