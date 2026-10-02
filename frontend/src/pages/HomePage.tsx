import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyStores } from '../api/stores'
import { useAuth } from '../auth/AuthContext'
import { CategoryIcon } from '../components/CategoryIcon'
import { ExpiryBanner } from '../components/ExpiryBanner'
import { EmptyState, Loading } from '../components/Feedback'
import { ArrowRightIcon, BellIcon, BoxIcon, CardIcon, ClockIcon, PlusIcon, SparkleIcon } from '../components/Icons'
import { Mascot } from '../components/Logo'
import { StatusBadge } from '../components/StatusBadge'
import { STORE_CATEGORIES } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import type { StoreRecord } from '../types'

function Dashboard() {
  const { user } = useAuth()
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
    return <Loading />
  }

  if (status === 'error') {
    return <p className="error-text">{error}</p>
  }

  const activeStores = stores.filter((store) => store.status === 'PENDING' || store.status === 'IN_USE')
  const inUseCount = activeStores.filter((store) => store.status === 'IN_USE').length
  const unpaidCount = activeStores.filter((store) => store.status === 'PENDING' && store.paymentStatus !== 'DONE').length
  const reservedCount = activeStores.length - inUseCount - unpaidCount

  return (
    <section>
      <div className="greeting">
        <h2>{user?.name}님의 짐 현황</h2>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <span className="stat-icon tone-accent">
            <BoxIcon size={22} />
          </span>
          <div>
            <span className="stat-label">보관중</span>
            <span className="stat-value">
              {inUseCount}
              <small>건</small>
            </span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon tone-mint">
            <ClockIcon size={22} />
          </span>
          <div>
            <span className="stat-label">체크인 대기</span>
            <span className="stat-value">
              {reservedCount}
              <small>건</small>
            </span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon tone-peach">
            <CardIcon size={22} />
          </span>
          <div>
            <span className="stat-label">결제 대기</span>
            <span className="stat-value">
              {unpaidCount}
              <small>건</small>
            </span>
          </div>
        </div>
      </div>

      <ExpiryBanner stores={activeStores} />

      <div className="section-header">
        <h2>진행 중인 보관</h2>
        <Link to="/my/stores" className="link-more">
          전체보기 <ArrowRightIcon size={14} />
        </Link>
      </div>
      {activeStores.length === 0 ? (
        <EmptyState
          title="아직 맡긴 짐이 없어요"
          description="짐 종류만 고르면 요금은 자동으로 계산해 드려요."
          action={
            <Link to="/my/stores/new" className="btn btn-primary">
              <PlusIcon size={16} /> 짐 보관하기
            </Link>
          }
        />
      ) : (
        <ul className="status-list">
          {activeStores.slice(0, 5).map((store) => (
            <li key={store.id}>
              <Link to="/my/stores" className="status-list-item">
                <CategoryIcon category={store.category} size="sm" />
                <div className="status-list-body">
                  <strong>{store.name}</strong>
                  <span>
                    {store.placeName} · {store.startDate} ~ {store.endDate}
                  </span>
                </div>
                <StatusBadge status={store.status} paid={store.paymentStatus === 'DONE'} />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function Intro() {
  return (
    <>
      <div className="feature-grid">
        <div className="feature-card">
          <span className="feature-icon tone-accent">
            <SparkleIcon size={22} />
          </span>
          <h3>요금은 자동으로</h3>
          <p>짐 종류와 기간만 고르면 하루 단위 요금이 바로 계산돼요.</p>
        </div>
        <div className="feature-card">
          <span className="feature-icon tone-peach">
            <BellIcon size={22} />
          </span>
          <h3>문자·카톡으로 알려드려요</h3>
          <p>예약 확정, 체크인, 찾는 날 하루 전까지 휴대폰으로 바로 알려드려요.</p>
        </div>
        <div className="feature-card">
          <span className="feature-icon tone-mint">
            <CardIcon size={22} />
          </span>
          <h3>카드로 간편 결제</h3>
          <p>예약하고 바로 결제까지, 결제 내역도 한곳에서 확인해요.</p>
        </div>
      </div>

      <div className="section-header">
        <h2>이런 짐을 맡길 수 있어요</h2>
      </div>
      <div className="category-grid">
        {STORE_CATEGORIES.map((category) => (
          <Link key={category.value} to="/signup" className="category-card" data-tone={category.value}>
            <CategoryIcon category={category.value} />
            <strong>{category.label}</strong>
            <span className="category-card-desc">{category.description}</span>
            <span className="category-card-price">
              {category.dailyRate.toLocaleString()}원 <small>/ 일</small>
            </span>
          </Link>
        ))}
      </div>
    </>
  )
}

function OwnerHome() {
  const { user } = useAuth()
  return (
    <section>
      <div className="greeting">
        <h2>{user?.name}님, 오늘도 잘 부탁드려요</h2>
      </div>
      <div className="feature-grid">
        <Link to="/owner" className="feature-card feature-link">
          <span className="feature-icon tone-accent">
            <ClockIcon size={22} />
          </span>
          <h3>오늘 입고·출고</h3>
          <p>오늘 맡기러 오거나 찾으러 올 손님과 이번 달 매출을 한눈에 봐요.</p>
        </Link>
        <Link to="/owner/scan" className="feature-card feature-link">
          <span className="feature-icon tone-peach">
            <SparkleIcon size={22} />
          </span>
          <h3>QR 체크인</h3>
          <p>시작일에 손님 QR을 스캔하면 보관이 시작되고, 찾아갈 때 한 번 더 스캔하면 끝나요.</p>
        </Link>
        <Link to="/owner/places" className="feature-card feature-link">
          <span className="feature-icon tone-mint">
            <BoxIcon size={22} />
          </span>
          <h3>지점 관리</h3>
          <p>스토어핏 정식 지점 운영을 신청하고, 하루에 받을 수 있는 짐 개수를 정해요.</p>
        </Link>
      </div>
    </section>
  )
}

export function HomePage() {
  const { isAuthenticated, isOwner, isAdmin } = useAuth()

  return (
    <>
      <section className="hero-section">
        <div className="hero-grid">
          <div className="hero-content">
            <span className="hero-eyebrow">
              <SparkleIcon size={14} /> 대학생 · 1인 가구를 위한 짐 보관
            </span>
            <h1>
              이동은 <span className="hero-highlight">가볍게</span>,
              <br />
              짐은 <span className="hero-highlight hero-highlight-warm">안전하게</span>
            </h1>
            <p className="hero-subtitle">
              자취방 이사, 방학 귀향, 계절 옷 정리까지.
              <br />
              맡긴 짐은 StoreFit이 꼼꼼하게 챙길게요.
            </p>
            <div className="hero-actions">
              {isAuthenticated && isAdmin ? (
                <Link to="/admin" className="btn btn-primary btn-lg">
                  본사 관리 <ArrowRightIcon size={18} />
                </Link>
              ) : isAuthenticated && isOwner ? (
                <>
                  <Link to="/owner" className="btn btn-primary btn-lg">
                    운영 대시보드 <ArrowRightIcon size={18} />
                  </Link>
                  <Link to="/owner/scan" className="btn btn-ghost btn-lg">
                    QR 체크인
                  </Link>
                </>
              ) : isAuthenticated ? (
                <>
                  <Link to="/my/stores/new" className="btn btn-primary btn-lg">
                    내 짐 보관하기 <ArrowRightIcon size={18} />
                  </Link>
                  <Link to="/my/stores" className="btn btn-ghost btn-lg">
                    보관 현황
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/signup" className="btn btn-primary btn-lg">
                    지금 시작하기 <ArrowRightIcon size={18} />
                  </Link>
                  <Link to="/login" className="btn btn-ghost btn-lg">
                    가입 없이 둘러보기
                  </Link>
                </>
              )}
            </div>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <div className="hero-mascot-wrap">
              <Mascot size={170} className="float-slow" />
            </div>
            <span className="hero-chip hero-chip-1">
              <span className="hero-chip-icon">
                <SparkleIcon size={15} />
              </span>
              하루 3,000원부터
            </span>
            <span className="hero-chip hero-chip-2">
              <span className="hero-chip-icon">
                <BellIcon size={15} />
              </span>
              찾기 전날 문자 알림
            </span>
            <span className="hero-chip hero-chip-3">
              <span className="hero-chip-icon">
                <CardIcon size={15} />
              </span>
              카드 간편 결제
            </span>
          </div>
        </div>
      </section>

      {!isAuthenticated ? <Intro /> : isAdmin ? null : isOwner ? <OwnerHome /> : <Dashboard />}
    </>
  )
}
