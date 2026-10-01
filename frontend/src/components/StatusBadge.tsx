import type { StoreStatus } from '../types'
import { CheckIcon } from './Icons'

function label(status: StoreStatus, paid: boolean): string {
  switch (status) {
    case 'PENDING':
      return paid ? '체크인 대기' : '결제 대기'
    case 'IN_USE':
      return '보관중'
    case 'COMPLETED':
      return '완료됨'
    case 'CANCELED':
      return '취소됨'
    case 'EXPIRED':
      return '결제 시간 초과'
    case 'NO_SHOW':
      return '노쇼'
  }
}

const STEPS = ['예약', '결제', '보관중', '완료']

/** 예약 → 결제 → (운영자 QR 체크인) 보관중 → (운영자 QR 체크아웃) 완료 */
function stepIndex(status: StoreStatus, paid: boolean): number {
  switch (status) {
    case 'PENDING':
      return paid ? 1 : 0
    case 'IN_USE':
      return 2
    case 'COMPLETED':
      return 4
    case 'CANCELED':
    case 'EXPIRED':
    case 'NO_SHOW':
      return -1
  }
}

export function StatusBadge({ status, paid = false }: { status: StoreStatus; paid?: boolean }) {
  const tone =
    status === 'PENDING' && paid
      ? 'badge-ready'
      : status === 'EXPIRED' || status === 'NO_SHOW'
        ? 'badge-canceled'
        : `badge-${status.toLowerCase()}`
  return <span className={`badge badge-dot ${tone}`}>{label(status, paid)}</span>
}

export function StatusStepper({ status, paid }: { status: StoreStatus; paid: boolean }) {
  const currentIndex = stepIndex(status, paid)
  if (currentIndex < 0) return null

  return (
    <ol className="stepper" aria-label={`진행 단계: ${label(status, paid)}`}>
      {STEPS.map((step, index) => {
        const state = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'todo'
        return (
          <li key={step} className={`stepper-step stepper-${state}`}>
            <span className="stepper-dot">{state === 'done' ? <CheckIcon size={12} /> : index + 1}</span>
            <span className="stepper-label">{step}</span>
          </li>
        )
      })}
    </ol>
  )
}
