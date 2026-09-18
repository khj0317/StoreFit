import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyStores } from '../api/stores'
import { useAuth } from '../auth/AuthContext'
import { StatusBadge } from '../components/StatusBadge'
import { getErrorMessage } from '../lib/api'
import type { StoreRecord } from '../types'

function daysSince(dateStr: string): number {
  const start = new Date(dateStr)
  start.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.round((today.getTime() - start.getTime()) / 86_400_000)) + 1
}

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

  const pending = stores.filter((store) => store.status === 'PENDING')
  const completed = stores.filter((store) => store.status === 'COMPLETED')
  const activePending = pending[0] ?? null

  return (
    <section className="dashboard">
      <h2>내 보관 현황</h2>
      <div className="stat-grid">
        <div className="stat-tile">
          <span className="stat-label">보관 중인 짐</span>
          <span className="stat-value">{pending.length}개</span>
        </div>
        <div className="stat-tile">
          <span className="stat-label">보관 시작일</span>
          <span className="stat-value">{activePending?.startDate ?? '-'}</span>
        </div>
        <div className="stat-tile">
          <span className="stat-label">보관 기간</span>
          <span className="stat-value">{activePending ? `${daysSince(activePending.startDate)}일째` : '-'}</span>
        </div>
        <div className="stat-tile">
          <span className="stat-label">완료한 보관</span>
          <span className="stat-value">{completed.length}건</span>
        </div>
      </div>

      <div className="page-header">
        <h2>보관 물품 상태</h2>
        <Link to="/my/stores">전체보기</Link>
      </div>
      {stores.length === 0 ? (
        <p>등록한 짐 보관 정보가 없습니다.</p>
      ) : (
        <ul className="status-list">
          {stores.slice(0, 5).map((store) => (
            <li key={store.id} className="status-list-item">
              <span>{store.name}</span>
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
        <div className="hero-content">
          <h1>
            이동은 <span className="hero-highlight-blue">가볍게</span>,
            <br />
            짐은 <span className="hero-highlight-yellow">안전하게</span>!
          </h1>
          <p className="hero-subtitle">
            대학생 · 1인 가구를 위한 맞춤형 짐 보관 솔루션,
            <br />
            짐을 맡길 곳과 기간을 한 번에 기록하세요.
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
      </section>

      {isAuthenticated && <Dashboard />}
    </>
  )
}
