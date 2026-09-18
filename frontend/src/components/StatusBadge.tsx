import type { StoreStatus } from '../types'

const LABELS: Record<StoreStatus, string> = {
  PENDING: '예약중',
  PICKED_UP: '픽업중',
  IN_USE: '이용중',
  COMPLETED: '완료됨',
  CANCELLED: '취소됨',
}

export function StatusBadge({ status }: { status: StoreStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>
}
