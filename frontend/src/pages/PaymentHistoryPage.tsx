import { useEffect, useState } from 'react'
import { getMyPayments } from '../api/payments'
import { getErrorMessage } from '../lib/api'
import type { PaymentResponse, PaymentStatus } from '../types'

const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  READY: '결제 대기',
  DONE: '결제 완료',
  FAILED: '결제 실패',
  CANCELED: '결제 취소',
}

function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  const badgeClass = status === 'DONE' ? 'badge-completed' : status === 'FAILED' ? 'badge-danger' : 'badge-pending'
  return <span className={`badge ${badgeClass}`}>{PAYMENT_STATUS_LABELS[status]}</span>
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

  return (
    <section>
      <div className="page-header">
        <h1>결제 내역</h1>
      </div>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && payments.length === 0 && <p className="empty-state">결제 내역이 없습니다.</p>}

      <ul className="list">
        {payments.map((payment) => (
          <li key={payment.id} className="list-item">
            <div>
              <strong>{payment.storeName}</strong>
              <p>주문번호 {payment.orderId}</p>
              <p>
                {payment.amount.toLocaleString()}원{payment.method && ` · ${payment.method}`}
              </p>
              <p className="store-card-address">
                {(payment.approvedAt ?? payment.createdAt).slice(0, 16).replace('T', ' ')}
              </p>
            </div>
            <PaymentStatusBadge status={payment.status} />
          </li>
        ))}
      </ul>
    </section>
  )
}
