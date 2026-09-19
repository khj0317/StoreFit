import { Link } from 'react-router-dom'
import { daysUntil, formatDday } from '../lib/date'
import type { StoreRecord } from '../types'

const EXPIRY_THRESHOLD_DAYS = 2

export function ExpiryBanner({ stores }: { stores: StoreRecord[] }) {
  const expiring = stores
    .filter((store) => store.status === 'IN_USE')
    .map((store) => ({ store, days: daysUntil(store.endDate) }))
    .filter(({ days }) => days <= EXPIRY_THRESHOLD_DAYS)
    .sort((a, b) => a.days - b.days)

  if (expiring.length === 0) return null

  return (
    <div className="expiry-banner">
      <strong>보관 만료 임박</strong>
      <ul className="expiry-banner-list">
        {expiring.map(({ store, days }) => (
          <li key={store.id}>
            <Link to="/my/stores">{store.name}</Link>
            <span className={`badge ${days < 0 ? 'badge-danger' : 'badge-warning'}`}>{formatDday(days)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
