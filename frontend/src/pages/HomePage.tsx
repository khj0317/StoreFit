import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyStores } from '../api/stores'
import { useAuth } from '../auth/AuthContext'
import { ExpiryBanner } from '../components/ExpiryBanner'
import { LogoMark } from '../components/Logo'
import { StatusBadge } from '../components/StatusBadge'
import { getErrorMessage } from '../lib/api'
import type { StoreRecord } from '../types'

function Dashboard() {
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
        setError(getErrorMessage(err, '짐 보관 현황을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }, [])

  if (status === 'loading') {
    return null
  }

  if (status === 'error') {
    return <p className="error-text">{error}</p>
  }

  const activeStores = stores.filter((store) => store.status !== 'COMPLETED')

  return (
    <section className="dashboard">
      <ExpiryBanner stores={activeStores} />
      <div className="page-header">
        <h2>내 보관 현황</h2>
        <Link to="/my/stores">전체보기</Link>
      </div>
      {activeStores.length === 0 ? (
        <p className="empty-state">등록한 짐 보관 정보가 없습니다.</p>
      ) : (
        <ul className="status-list">
          {activeStores.slice(0, 5).map((store) => (
            <li key={store.id} className="status-list-item">
              <div>
                <strong>{store.name}</strong>
                <p className="store-card-address">
                  {store.startDate} ~ {store.endDate}
                </p>
              </div>
              <StatusBadge status={store.status} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <>
      <section className="hero-section">
        <div className="hero-grid">
          <div className="hero-content">
            <h1>
              이동은 <span className="hero-highlight-blue">가볍게</span>,
              <br />
              짐은 <span className="hero-highlight-yellow">안전하게</span>!
            </h1>
            <p className="hero-subtitle">
              대학생 · 1인 가구를 위한 맞춤형 짐 보관 솔루션,
              <br />
              StoreFit에서 안전하게 짐을 보관하세요.
            </p>
            {isAuthenticated ? (
              <Link to="/my/stores/new" className="btn btn-secondary btn-lg">
                내 짐 보관하기 →
              </Link>
            ) : (
              <Link to="/signup" className="btn btn-secondary btn-lg">
                지금 시작하기 →
              </Link>
            )}
          </div>

          <div className="hero-preview">
            <span className="hero-badge hero-badge-top">카테고리별 요금 자동 계산</span>
            <div className="phone-mock">
              <div className="phone-notch" />
              <div className="phone-header">
                <LogoMark size={18} />
                <span>StoreFit</span>
              </div>
              <p className="phone-greeting">안녕하세요 👋</p>
              <div className="phone-highlight">
                <span className="phone-highlight-label">5~10kg · 가벼운 짐</span>
                <strong>일 3,000원부터</strong>
              </div>
              <p className="phone-section-label">내 보관 현황</p>
              <div className="phone-list-item">
                <span>캐리어 보관</span>
                <span className="phone-badge phone-badge-active">이용중</span>
              </div>
              <div className="phone-list-item">
                <span>겨울옷 보관</span>
                <span className="phone-badge phone-badge-pending">예약중</span>
              </div>
            </div>
            <span className="hero-badge hero-badge-bottom">실시간 상태 관리</span>
          </div>
        </div>
      </section>

      {isAuthenticated && <Dashboard />}
    </>
  )
}
