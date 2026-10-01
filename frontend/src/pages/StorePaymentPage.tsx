import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getStore } from '../api/stores'
import { readyPayment } from '../api/payments'
import { useAuth } from '../auth/AuthContext'
import { CategoryIcon } from '../components/CategoryIcon'
import { AuthCard, Loading } from '../components/Feedback'
import { CardIcon } from '../components/Icons'
import { STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import { requestTossPayment } from '../lib/tossPayments'
import type { StoreRecord } from '../types'

export function StorePaymentPage() {
  const { storeId } = useParams<{ storeId: string }>()
  const { user } = useAuth()

  const [store, setStore] = useState<StoreRecord | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    getStore(Number(storeId))
      .then(setStore)
      .catch((err: unknown) => setError(getErrorMessage(err, '짐 보관 정보를 불러오지 못했습니다.')))
  }, [storeId])

  const handlePay = async () => {
    if (!store) return
    setError('')
    setSubmitting(true)
    try {
      const order = await readyPayment(store.id)
      const origin = window.location.origin
      await requestTossPayment({
        amount: order.amount,
        orderId: order.orderId,
        orderName: order.orderName,
        customerName: user?.name,
        successUrl: `${origin}/my/stores/${store.id}/pay/success`,
        failUrl: `${origin}/my/stores/${store.id}/pay/fail`,
      })
    } catch (err) {
      setError(getErrorMessage(err, '결제를 시작하지 못했습니다.'))
      setSubmitting(false)
    }
  }

  if (error && !store) {
    return <p className="error-text">{error}</p>
  }

  if (!store) {
    return <Loading />
  }

  if (store.paymentStatus === 'DONE') {
    return (
      <AuthCard title="이미 결제가 끝났어요" subtitle="이 짐 보관은 결제가 완료된 상태예요.">
        <div className="result-actions">
          <Link to="/my/stores" className="btn btn-primary btn-lg">
            보관 현황으로
          </Link>
        </div>
      </AuthCard>
    )
  }

  const days = Math.round((new Date(store.endDate).getTime() - new Date(store.startDate).getTime()) / 86_400_000) + 1

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>결제하기</h1>
        <div className="receipt">
          <div className="receipt-head" data-tone={store.category}>
            <CategoryIcon category={store.category} size="sm" />
            <div>
              <strong>{store.name}</strong>
              <span>{STORE_CATEGORY_LABELS[store.category]}</span>
            </div>
          </div>
          <div className="payment-summary-row">
            <span>지점</span>
            <span>{store.placeName}</span>
          </div>
          <div className="payment-summary-row">
            <span>보관 기간</span>
            <span>
              {store.startDate} ~ {store.endDate} ({days}일)
            </span>
          </div>
          <div className="payment-summary-row">
            <span>짐 개수</span>
            <span>{store.luggageCount}개</span>
          </div>
          <div className="payment-summary-row payment-summary-total">
            <span>결제 금액</span>
            <span>{store.totalPrice.toLocaleString()}원</span>
          </div>
        </div>

        {error && <p className="error-text">{error}</p>}

        <button type="button" className="btn btn-primary btn-lg btn-block" onClick={handlePay} disabled={submitting}>
          <CardIcon size={18} />
          {submitting ? '결제 진행 중...' : `${store.totalPrice.toLocaleString()}원 결제하기`}
        </button>
        <p className="secure-note">안전한 결제창에서 결제가 진행돼요</p>
        {store.paymentDeadline && (
          <p className="secure-note">
            {new Date(store.paymentDeadline).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}까지 결제하지
            않으면 예약이 자동 취소돼요
          </p>
        )}
      </div>
    </div>
  )
}
