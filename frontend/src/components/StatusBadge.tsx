import type { ReservationStatus } from '../types'

const LABELS: Record<ReservationStatus, string> = {
  PENDING: '대기중',
  CONFIRMED: '확정됨',
  IN_PROGRESS: '보관중',
  COMPLETED: '완료됨',
  CANCELLED: '취소됨',
}

export function StatusBadge({ status }: { status: ReservationStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>
}
