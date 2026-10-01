import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cancelStore, deleteStore, getMyStores, getRefundPreview } from '../api/stores'
import { CategoryIcon } from '../components/CategoryIcon'
import { ExpiryBanner } from '../components/ExpiryBanner'
import { EmptyState, Loading } from '../components/Feedback'
import { BoxIcon, CalendarIcon, PinIcon, PlusIcon } from '../components/Icons'
import { QrModal } from '../components/QrModal'
import { StatusBadge, StatusStepper } from '../components/StatusBadge'
import { STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import { daysUntil, formatDday } from '../lib/date'
import type { StoreRecord } from '../types'

function QrIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <rect x="3.5" y="3.5" width="6" height="6" rx="1.2" />
      <rect x="14.5" y="3.5" width="6" height="6" rx="1.2" />
      <rect x="3.5" y="14.5" width="6" height="6" rx="1.2" />
      <path d="M14.5 14.5h2.5v2.5M20.5 14.5v6h-3M14.5 20.5v-2" strokeLinecap="round" />
    </svg>
  )
}

function useNow(intervalMs: number) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs)
    return () => window.clearInterval(timer)
  }, [intervalMs])
  return now
}

function formatRemaining(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000))
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

export function MyStoresPage() {
  const now = useNow(1000)
  const [stores, setStores] = useState<StoreRecord[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const [qrStore, setQrStore] = useState<StoreRecord | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const load = () => {
    getMyStores()
      .then((data) => {
        setStores(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '짐 보관 목록을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }

  useEffect(load, [])

  const activeStores = stores.filter((store) => store.status === 'PENDING' || store.status === 'IN_USE')

  const handleDelete = async (storeId: number) => {
    if (!window.confirm('결제 전 예약을 삭제할까요? 삭제하면 되돌릴 수 없어요.')) return
    setActionError('')
    setNotice('')
    try {
      await deleteStore(storeId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '삭제에 실패했습니다.'))
    }
  }

  const handleCancel = async (store: StoreRecord) => {
    setActionError('')
    setNotice('')
    setBusyId(store.id)
    try {
      const preview = await getRefundPreview(store.id)
      const message = `${preview.policy}\n\n결제 ${preview.paidAmount.toLocaleString()}원 중 ${preview.refundAmount.toLocaleString()}원(${preview.refundRate}%)을 환불받아요. 예약을 취소할까요?`
      if (!window.confirm(message)) return
      const canceled = await cancelStore(store.id)
      setNotice(`예약을 취소하고 ${canceled.refundedAmount.toLocaleString()}원을 환불했어요.`)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '취소에 실패했습니다.'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <span className="page-eyebrow">My Storage</span>
          <h1>짐 보관 현황</h1>
        </div>
        <div className="page-header-actions">
          <Link to="/my/stores/history" className="btn btn-ghost btn-sm">
            지난 보관 내역
          </Link>
          <Link to="/my/stores/new" className="btn btn-primary btn-sm">
            <PlusIcon size={15} /> 짐 보관하기
          </Link>
        </div>
      </div>

      <ExpiryBanner stores={activeStores} />

      {status === 'loading' && <Loading />}
      {status === 'error' && <p className="error-text">{error}</p>}
      {actionError && <p className="error-text">{actionError}</p>}
      {notice && <p className="success-text">{notice}</p>}
      {status === 'ready' && activeStores.length === 0 && (
        <EmptyState
          title="진행 중인 보관이 없어요"
          description="새로 맡길 짐이 있다면 지금 바로 예약해 보세요."
          action={
            <Link to="/my/stores/new" className="btn btn-primary">
              <PlusIcon size={16} /> 짐 보관하기
            </Link>
          }
        />
      )}

      {status === 'ready' && (
        <ul className="list">
          {activeStores.map((store) => {
            const paid = store.paymentStatus === 'DONE'
            const days = daysUntil(store.endDate)
            return (
              <li key={store.id} className="store-card">
                <div className="store-card-main">
                  {store.imageUrls[0] ? (
                    <img className="store-thumb" src={store.imageUrls[0]} alt={store.name} />
                  ) : (
                    <CategoryIcon category={store.category} size="lg" />
                  )}
                  <div className="store-card-body">
                    <div className="store-card-title">
                      <h3>{store.name}</h3>
                      <span className="chip" data-tone={store.category}>
                        {STORE_CATEGORY_LABELS[store.category]}
                      </span>
                      <StatusBadge status={store.status} paid={paid} />
                      {store.status === 'IN_USE' && (
                        <span className={`badge ${days < 0 ? 'badge-danger' : 'badge-warning'}`}>{formatDday(days)}</span>
                      )}
                    </div>
                    <ul className="meta-list">
                      <li>
                        <PinIcon size={15} />
                        <strong className="meta-strong">{store.placeName}</strong> {store.placeAddress}
                      </li>
                      <li>
                        <CalendarIcon size={15} />
                        {store.startDate} ~ {store.endDate}
                      </li>
                      <li>
                        <BoxIcon size={15} />짐 {store.luggageCount}개
                      </li>
                    </ul>
                    {store.description && <p className="store-card-desc">{store.description}</p>}
                    {store.status === 'PENDING' && !paid && store.paymentDeadline && (
                      <p className="deadline-note">
                        {new Date(store.paymentDeadline).getTime() > now
                          ? `${formatRemaining(new Date(store.paymentDeadline).getTime() - now)} 안에 결제하지 않으면 예약이 자동 취소돼요`
                          : '결제 시간이 지나 곧 자동 취소돼요'}
                      </p>
                    )}
                    {store.status === 'PENDING' && paid && (
                      <p className="info-note">{store.startDate} 당일에 지점에서 체크인 QR을 보여주세요. 이날 오지 않으면 노쇼로 처리돼요.</p>
                    )}
                    {store.status === 'IN_USE' && store.overdueDays > 0 && (
                      <p className="deadline-note">
                        찾는 날이 {store.overdueDays}일 지났어요 · 연체료 {store.overdueFee.toLocaleString()}원 (찾을 때 지점에서 결제)
                      </p>
                    )}
                  </div>
                  <div className="store-card-price">
                    <span>총 금액</span>
                    <strong>{store.totalPrice.toLocaleString()}원</strong>
                  </div>
                </div>

                <div className="store-card-foot">
                  <StatusStepper status={store.status} paid={paid} />
                  <div className="list-item-actions">
                    {store.status === 'PENDING' && !paid && (
                      <>
                        <button type="button" className="btn btn-sm btn-text-danger" onClick={() => handleDelete(store.id)}>
                          삭제
                        </button>
                        <Link to={`/my/stores/${store.id}/edit`} className="btn btn-ghost btn-sm">
                          수정
                        </Link>
                        <Link to={`/my/stores/${store.id}/pay`} className="btn btn-secondary btn-sm">
                          결제하기
                        </Link>
                      </>
                    )}
                    {store.status === 'PENDING' && paid && (
                      <button
                        type="button"
                        className="btn btn-sm btn-text-danger"
                        onClick={() => handleCancel(store)}
                        disabled={busyId === store.id}
                      >
                        {busyId === store.id ? '확인 중...' : '예약 취소'}
                      </button>
                    )}
                    {store.checkInCode && (
                      <button type="button" className="btn btn-primary btn-sm" onClick={() => setQrStore(store)}>
                        <QrIcon /> {store.status === 'IN_USE' ? '찾을 때 QR' : '체크인 QR'}
                      </button>
                    )}
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {qrStore && <QrModal store={qrStore} onClose={() => setQrStore(null)} />}
    </section>
  )
}
