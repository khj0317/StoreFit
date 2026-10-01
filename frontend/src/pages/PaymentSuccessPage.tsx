import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { confirmPayment } from '../api/payments'
import { AuthCard, Loading } from '../components/Feedback'
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

  if (status === 'confirming') {
    return <Loading label="결제를 확인하는 중이에요" />
  }

  if (status === 'error') {
    return (
      <AuthCard title="결제 승인에 실패했어요" mood="sad">
        <p className="error-text">{error}</p>
        <div className="result-actions">
          <Link to="/my/stores" className="btn btn-ghost btn-lg">
            보관 현황으로
          </Link>
        </div>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="결제가 완료됐어요!" subtitle="체크인 QR이 발급됐어요. 맡기는 날 보관소에서 QR을 보여주세요.">
      <div className="result-actions">
        <Link to="/my/stores" className="btn btn-primary btn-lg">
          체크인 QR 보기
        </Link>
        <Link to="/my/payments" className="btn btn-ghost">
          결제 내역 확인
        </Link>
      </div>
    </AuthCard>
  )
}
