import { Link } from 'react-router-dom'
import type { StoreSummary } from '../types'

export function StoreCard({ store }: { store: StoreSummary }) {
  return (
    <Link to={`/stores/${store.id}`} className="store-card">
      <div className="store-card-thumb">
        {store.thumbnailUrl ? (
          <img src={store.thumbnailUrl} alt={store.name} />
        ) : (
          <div className="store-card-thumb-placeholder">이미지 없음</div>
        )}
      </div>
      <div className="store-card-body">
        <h3>{store.name}</h3>
        <p className="store-card-address">{store.address}</p>
      </div>
    </Link>
  )
}
