import { useEffect, useState } from 'react'
import { getMyPayments } from '../api/payments'
import { EmptyState, Loading } from '../components/Feedback'
import { CardIcon } from '../components/Icons'
import { getErrorMessage } from '../lib/api'
import type { PaymentResponse, PaymentStatus } from '../types'

const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  READY: '결제 대기',
  DONE: '결제 완료',
  FAILED: '결제 시간 초과',
  CANCELED: '전액 환불',
  PARTIAL_CANCELED: '부분 환불',
}

const PAYMENT_STATUS_TONES: Record<PaymentStatus, { badge: string; icon: string }> = {
  READY: { badge: 'badge-pending', icon: 'tone-neutral' },
  DONE: { badge: 'badge-completed', icon: 'tone-mint' },
  FAILED: { badge: 'badge-danger', icon: 'tone-danger' },
  CANCELED: { badge: 'badge-canceled', icon: 'tone-neutral' },
  PARTIAL_CANCELED: { badge: 'badge-canceled', icon: 'tone-neutral' },
}

function paidAt(payment: PaymentResponse): string {
  return payment.approvedAt ?? payment.createdAt
}

export function PaymentHistoryPage() {
  const [payments, setPayments] = useState<PaymentResponse[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')

  useEffect(() => {
    getMyPayments()
      .then((data) => {
        setPayments(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '결제 내역을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }, [])

  const totalPaid = payments
    .filter((payment) => payment.status !== 'READY' && payment.status !== 'FAILED')
    .reduce((sum, payment) => sum + payment.amount - payment.canceledAmount, 0)

  return (
    <section>
      <div className="page-header">
        <div>
          <span className="page-eyebrow">Payments</span>
          <h1>결제 내역</h1>
        </div>
      </div>

      {status === 'loading' && <Loading />}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && payments.length === 0 && (
        <EmptyState title="결제 내역이 없어요" description="짐 보관을 결제하면 여기에서 확인할 수 있어요." />
      )}

      {status === 'ready' && payments.length > 0 && (
        <div className="stat-card stat-card-spaced">
          <span className="stat-icon tone-accent">
            <CardIcon size={22} />
          </span>
          <div>
            <span className="stat-label">지금까지 결제한 금액</span>
            <span className="stat-value">
              {totalPaid.toLocaleString()}
              <small>원</small>
            </span>
          </div>
        </div>
      )}

      <ul className="list">
        {payments.map((payment) => (
          <li key={payment.id} className="row-card">
            <span className={`row-card-icon ${PAYMENT_STATUS_TONES[payment.status].icon}`}>
              <CardIcon size={22} />
            </span>
            <div className="row-card-body">
              <strong>{payment.storeName}</strong>
              <span className="row-card-sub">
                {paidAt(payment).slice(0, 16).replace('T', ' ')}
                {payment.method && ` · ${payment.method}`}
              </span>
              <p className="row-card-mono">주문번호 {payment.orderId}</p>
            </div>
            <div className="row-card-end">
              <span className="row-card-amount">{payment.amount.toLocaleString()}원</span>
              {payment.canceledAmount > 0 && (
                <span className="row-card-refund">-{payment.canceledAmount.toLocaleString()}원 환불</span>
              )}
              <span className={`badge badge-dot ${PAYMENT_STATUS_TONES[payment.status].badge}`}>
                {PAYMENT_STATUS_LABELS[payment.status]}
              </span>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
