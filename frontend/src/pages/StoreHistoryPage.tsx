import { useEffect, useState } from 'react'
import { getMyStores } from '../api/stores'
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

  const completedStores = stores.filter((store) => store.status === 'COMPLETED')

  return (
    <section>
      <div className="page-header">
        <h1>짐 보관 내역</h1>
      </div>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && completedStores.length === 0 && (
        <p className="empty-state">완료된 짐 보관 내역이 없습니다.</p>
      )}

      <ul className="list">
        {completedStores.map((store) => (
          <li key={store.id} className="list-item">
            <div className="store-record-info">
              {store.imageUrls[0] && (
                <img className="store-record-thumb" src={store.imageUrls[0]} alt={store.name} />
              )}
              <div>
                <strong>{store.name}</strong>
                <span className="badge badge-category">{STORE_CATEGORY_LABELS[store.category]}</span>
                <p className="store-card-address">{store.address}</p>
                <p>
                  {store.startDate} ~ {store.endDate}
                </p>
                <p>
                  짐 {store.luggageCount}개 · {store.totalPrice.toLocaleString()}원
                </p>
                {store.description && <p>{store.description}</p>}
              </div>
            </div>
            <span className="badge badge-completed">완료됨</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
