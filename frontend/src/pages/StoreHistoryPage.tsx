import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyStores } from '../api/stores'
import { CategoryIcon } from '../components/CategoryIcon'
import { EmptyState, Loading } from '../components/Feedback'
import { StatusBadge } from '../components/StatusBadge'
import { STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import type { StoreRecord } from '../types'

export function StoreHistoryPage() {
  const [stores, setStores] = useState<StoreRecord[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')

  useEffect(() => {
    getMyStores()
      .then((data) => {
        setStores(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '짐 보관 내역을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }, [])

  const pastStores = stores.filter((store) => store.status !== 'PENDING' && store.status !== 'IN_USE')

  return (
    <section>
      <div className="page-header">
        <div>
          <span className="page-eyebrow">History</span>
          <h1>지난 보관 내역</h1>
          <p className="page-subtitle">보관을 마쳤거나 취소·만료된 예약이 여기에 모여요.</p>
        </div>
        <Link to="/my/stores" className="btn btn-ghost btn-sm">
          보관 현황으로
        </Link>
      </div>

      {status === 'loading' && <Loading />}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && pastStores.length === 0 && (
        <EmptyState title="아직 지난 보관이 없어요" description="보관을 마치면 이곳에 기록이 남아요." />
      )}

      <ul className="list">
        {pastStores.map((store) => (
          <li key={store.id} className="row-card">
            {store.imageUrls[0] ? (
              <img className="store-thumb store-thumb-sm" src={store.imageUrls[0]} alt={store.name} />
            ) : (
              <CategoryIcon category={store.category} size="sm" />
            )}
            <div className="row-card-body">
              <strong>{store.name}</strong>
              <span className="row-card-sub">
                {STORE_CATEGORY_LABELS[store.category]} · 짐 {store.luggageCount}개 · {store.startDate} ~ {store.endDate}
              </span>
              <p className="row-card-mono">{store.placeName}</p>
            </div>
            <div className="row-card-end">
              <span className="row-card-amount">
                {store.status === 'CANCELED'
                  ? `${store.refundedAmount.toLocaleString()}원 환불`
                  : store.status === 'EXPIRED'
                    ? '결제 안 함'
                    : `${store.totalPrice.toLocaleString()}원`}
              </span>
              <StatusBadge status={store.status} />
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
