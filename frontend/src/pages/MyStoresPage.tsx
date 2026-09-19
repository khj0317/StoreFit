import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { beginStorage, completeStore, deleteStore, getMyStores, pickUpStore } from '../api/stores'
import { ExpiryBanner } from '../components/ExpiryBanner'
import { StatusBadge } from '../components/StatusBadge'
import { STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import { daysUntil, formatDday } from '../lib/date'
import type { StoreRecord } from '../types'

export function MyStoresPage() {
  const [stores, setStores] = useState<StoreRecord[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  const load = () => {
    setStatus('loading')
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

  const activeStores = stores.filter((store) => store.status !== 'COMPLETED')

  const handleCancel = async (storeId: number) => {
    if (!window.confirm('이 짐 보관을 취소하시겠습니까? 취소하면 삭제되어 되돌릴 수 없습니다.')) return
    setActionError('')
    try {
      await deleteStore(storeId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '취소에 실패했습니다.'))
    }
  }

  const handlePickUp = async (storeId: number) => {
    setActionError('')
    try {
      await pickUpStore(storeId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '픽업 처리에 실패했습니다.'))
    }
  }

  const handleBeginStorage = async (storeId: number) => {
    setActionError('')
    try {
      await beginStorage(storeId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '보관 처리에 실패했습니다.'))
    }
  }

  const handleComplete = async (storeId: number) => {
    if (!window.confirm('이용을 완료 처리하시겠습니까? 완료 처리된 항목은 짐 보관 내역으로 이동합니다.')) return
    setActionError('')
    try {
      await completeStore(storeId)
      load()
    } catch (err) {
      setActionError(getErrorMessage(err, '완료 처리에 실패했습니다.'))
    }
  }

  return (
    <section>
      <div className="page-header">
        <h1>짐 보관 현황</h1>
        <Link to="/my/stores/history">짐 보관 내역</Link>
      </div>

      <ExpiryBanner stores={activeStores} />

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {actionError && <p className="error-text">{actionError}</p>}
      {status === 'ready' && activeStores.length === 0 && (
        <p className="empty-state">등록한 짐 보관 정보가 없습니다.</p>
      )}

      <ul className="list">
        {activeStores.map((store) => (
          <li key={store.id} className="list-item list-item-column">
            <div className="list-item-row">
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
                    {store.status === 'IN_USE' && (
                      <span className={`badge ${daysUntil(store.endDate) < 0 ? 'badge-danger' : 'badge-warning'}`}>
                        {' '}
                        {formatDday(daysUntil(store.endDate))}
                      </span>
                    )}
                  </p>
                  <p>
                    짐 {store.luggageCount}개 · {store.totalPrice.toLocaleString()}원
                  </p>
                  {store.description && <p>{store.description}</p>}
                </div>
              </div>
              <div className="list-item-actions">
                <StatusBadge status={store.status} />
                {store.status === 'PENDING' && (
                  <>
                    <Link to={`/my/stores/${store.id}/edit`} className="btn btn-ghost">
                      수정
                    </Link>
                    {store.paymentStatus === 'DONE' ? (
                      <button type="button" className="btn btn-primary" onClick={() => handlePickUp(store.id)}>
                        짐 픽업 완료
                      </button>
                    ) : (
                      <Link to={`/my/stores/${store.id}/pay`} className="btn btn-secondary">
                        결제하기
                      </Link>
                    )}
                  </>
                )}
                {store.status === 'PICKED_UP' && (
                  <button type="button" className="btn btn-primary" onClick={() => handleBeginStorage(store.id)}>
                    보관 완료
                  </button>
                )}
                {store.status === 'IN_USE' && (
                  <button type="button" className="btn btn-primary" onClick={() => handleComplete(store.id)}>
                    이용 완료
                  </button>
                )}
                <button type="button" className="btn btn-danger" onClick={() => handleCancel(store.id)}>
                  취소
                </button>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
