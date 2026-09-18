import { type FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { beginStorage, completeStore, deleteStore, getMyStores, pickUpStore } from '../api/stores'
import { createReview } from '../api/reviews'
import { StatusBadge } from '../components/StatusBadge'
import { STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import type { StoreRecord } from '../types'

function ReviewForm({ storeId, onSubmitted }: { storeId: number; onSubmitted: () => void }) {
  const [rating, setRating] = useState('5')
  const [content, setContent] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await createReview({ storeId, rating: Number(rating), content })
      onSubmitted()
    } catch (err) {
      setError(getErrorMessage(err, '리뷰 등록에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form form-inline" onSubmit={handleSubmit}>
      <label className="form-group">
        <span>평점</span>
        <select value={rating} onChange={(e) => setRating(e.target.value)}>
          {[5, 4, 3, 2, 1].map((value) => (
            <option key={value} value={value}>
              {value}점
            </option>
          ))}
        </select>
      </label>
      <label className="form-group">
        <span>리뷰 내용</span>
        <input value={content} onChange={(e) => setContent(e.target.value)} />
      </label>
      {error && <p className="error-text">{error}</p>}
      <button type="submit" className="btn btn-ghost" disabled={submitting}>
        {submitting ? '등록 중...' : '리뷰 작성'}
      </button>
    </form>
  )
}

function ReviewDisplay({ review }: { review: NonNullable<StoreRecord['review']> }) {
  return (
    <div className="review-item">
      <div className="review-item-header">
        <span>{'★'.repeat(review.rating)}</span>
      </div>
      {review.content && <p>{review.content}</p>}
    </div>
  )
}

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
    if (!window.confirm('이용을 완료 처리하시겠습니까? 완료 후에는 되돌릴 수 없습니다.')) return
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
      </div>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {actionError && <p className="error-text">{actionError}</p>}
      {status === 'ready' && stores.length === 0 && <p>등록한 짐 보관 정보가 없습니다.</p>}

      <ul className="list">
        {stores.map((store) => (
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
            {store.status === 'COMPLETED' &&
              (store.review ? <ReviewDisplay review={store.review} /> : <ReviewForm storeId={store.id} onSubmitted={load} />)}
          </li>
        ))}
      </ul>
    </section>
  )
}
