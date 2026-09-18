import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { confirmPayment } from '../api/payments'
import { getErrorMessage } from '../lib/api'

export function PaymentSuccessPage() {
  const [searchParams] = useSearchParams()
  const [status, setStatus] = useState<'confirming' | 'done' | 'error'>('confirming')
  const [error, setError] = useState('')

  useEffect(() => {
    const paymentKey = searchParams.get('paymentKey')
    const orderId = searchParams.get('orderId')
    const amount = searchParams.get('amount')

    if (!paymentKey || !orderId || !amount) {
      setError('결제 정보가 올바르지 않습니다.')
      setStatus('error')
      return
    }

    confirmPayment({ paymentKey, orderId, amount: Number(amount) })
      .then(() => setStatus('done'))
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '결제 승인에 실패했습니다.'))
        setStatus('error')
      })
  }, [searchParams])

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>결제 확인</h1>
        {status === 'confirming' && <p>결제를 확인하는 중입니다...</p>}
        {status === 'done' && (
          <>
            <p className="success-text">결제가 완료되었습니다.</p>
            <Link to="/my/stores" className="btn btn-primary">
              내 짐 보관으로 이동
            </Link>
          </>
        )}
        {status === 'error' && (
          <>
            <p className="error-text">{error}</p>
            <Link to="/my/stores" className="btn btn-ghost">
              내 짐 보관으로 이동
            </Link>
          </>
        )}
      </div>
    </div>
  )
}
