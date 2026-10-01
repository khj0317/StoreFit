import { Link } from 'react-router-dom'
import { daysUntil, formatDday } from '../lib/date'
import type { StoreRecord } from '../types'
import { BellIcon } from './Icons'

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
      <span className="expiry-banner-icon">
        <BellIcon size={20} />
      </span>
      <div className="expiry-banner-body">
        <strong>보관 기간이 곧 끝나요</strong>
        <ul className="expiry-banner-list">
          {expiring.map(({ store, days }) => (
            <li key={store.id}>
              <Link to="/my/stores">{store.name}</Link>
              <span className={`badge ${days < 0 ? 'badge-danger' : 'badge-warning'}`}>{formatDday(days)}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
