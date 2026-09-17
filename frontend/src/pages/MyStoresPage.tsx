import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteStore, getMyStores } from '../api/stores'
import { getErrorMessage } from '../lib/api'
import type { StoreSummary } from '../types'

export function MyStoresPage() {
  const [stores, setStores] = useState<StoreSummary[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')

  const load = () => {
    setStatus('loading')
    getMyStores()
      .then((data) => {
        setStores(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '보관소 목록을 불러오지 못했습니다.'))
        setStatus('error')
      })
  }

  useEffect(load, [])

  const handleDelete = async (storeId: number) => {
    if (!window.confirm('이 보관소를 삭제하시겠습니까?')) return
    try {
      await deleteStore(storeId)
      load()
    } catch (err) {
      window.alert(getErrorMessage(err, '삭제에 실패했습니다.'))
    }
  }

  return (
    <section>
      <div className="page-header">
        <h1>내 보관소</h1>
        <Link to="/my/stores/new" className="btn btn-primary">
          보관소 등록
        </Link>
      </div>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && stores.length === 0 && <p>등록한 보관소가 없습니다.</p>}

      <ul className="list">
        {stores.map((store) => (
          <li key={store.id} className="list-item">
            <div>
              <strong>{store.name}</strong>
              <p className="store-card-address">{store.address}</p>
              <p>시간당 {store.pricePerHour.toLocaleString()}원 · 수용 {store.capacity}개</p>
            </div>
            <div className="list-item-actions">
              <Link to={`/stores/${store.id}`} className="btn btn-ghost">
                보기
              </Link>
              <Link to={`/my/stores/${store.id}/edit`} className="btn btn-ghost">
                수정
              </Link>
              <button type="button" className="btn btn-danger" onClick={() => handleDelete(store.id)}>
                삭제
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
