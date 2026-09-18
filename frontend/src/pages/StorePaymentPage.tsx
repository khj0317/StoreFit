import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getStore } from '../api/stores'
import { readyPayment } from '../api/payments'
import { useAuth } from '../auth/AuthContext'
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
    return <p>불러오는 중...</p>
  }

  if (store.paymentStatus === 'DONE') {
    return (
      <div className="centered-layout">
        <div className="centered-card">
          <h1>결제 완료</h1>
          <p>이미 결제가 완료된 짐 보관입니다.</p>
        </div>
      </div>
    )
  }

  const days = Math.round((new Date(store.endDate).getTime() - new Date(store.startDate).getTime()) / 86_400_000) + 1

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>결제하기</h1>
        <div className="payment-summary">
          <div className="payment-summary-row">
            <span>제목</span>
            <span>{store.name}</span>
          </div>
          <div className="payment-summary-row">
            <span>짐 종류</span>
            <span>{STORE_CATEGORY_LABELS[store.category]}</span>
          </div>
          <div className="payment-summary-row">
            <span>기간</span>
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

        <button type="button" className="btn btn-primary" onClick={handlePay} disabled={submitting}>
          {submitting ? '결제 진행 중...' : `${store.totalPrice.toLocaleString()}원 결제하기`}
        </button>
      </div>
    </div>
  )
}
