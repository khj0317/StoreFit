import { useEffect, useState } from 'react'
import { getStores } from '../api/stores'
import { StoreCard } from '../components/StoreCard'
import { getErrorMessage } from '../lib/api'
import type { StoreSummary } from '../types'

export function HomePage() {
  const [stores, setStores] = useState<StoreSummary[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')

  useEffect(() => {
    getStores()
      .then((data) => {
        setStores(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '보관소 목록을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }, [])

  return (
    <section>
      <h1>짐 보관소 찾기</h1>
      <p className="page-subtitle">가까운 짐 보관소를 예약하세요.</p>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && stores.length === 0 && <p>등록된 보관소가 없습니다.</p>}

      <div className="store-grid">
        {stores.map((store) => (
          <StoreCard key={store.id} store={store} />
        ))}
      </div>
    </section>
  )
}
