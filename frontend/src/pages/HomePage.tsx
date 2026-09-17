import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getStores } from '../api/stores'
import { useAuth } from '../auth/AuthContext'
import { StoreCard } from '../components/StoreCard'
import { getErrorMessage } from '../lib/api'
import type { StoreSummary } from '../types'

export function HomePage() {
  const { isAuthenticated } = useAuth()
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
        setError(getErrorMessage(err, '짐 보관 목록을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }, [])

  return (
    <>
      <section className="hero-section">
        <div className="hero-content">
          <h1>
            이동은 <span className="hero-highlight-blue">가볍게</span>,
            <br />
            짐은 <span className="hero-highlight-yellow">안전하게</span>!
          </h1>
          <p className="hero-subtitle">
            대학생 · 1인 가구를 위한 맞춤형 짐 보관 솔루션,
            <br />
            합리적인 비용과 편리한 예약 경험을 한 번에 제공합니다.
          </p>
          {!isAuthenticated && (
            <Link to="/signup" className="btn btn-secondary btn-lg">
              지금 시작하기 →
            </Link>
          )}
        </div>
      </section>

      <section>
        <h2>짐 보관 둘러보기</h2>

        {status === 'loading' && <p>불러오는 중...</p>}
        {status === 'error' && <p className="error-text">{error}</p>}
        {status === 'ready' && stores.length === 0 && <p>등록된 짐 보관 정보가 없습니다.</p>}

        <div className="store-grid">
          {stores.map((store) => (
            <StoreCard key={store.id} store={store} />
          ))}
        </div>
      </section>
    </>
  )
}
