import type { StoreCategory } from '../types'

function Illustration({ category }: { category: StoreCategory }) {
  switch (category) {
    case 'LIGHT':
      return (
        <>
          <rect x="18" y="6" width="12" height="9" rx="3.5" stroke="currentColor" strokeWidth="3" />
          <rect x="10" y="12" width="28" height="28" rx="7" fill="currentColor" />
          <rect x="17" y="12" width="3" height="28" fill="var(--tile)" opacity="0.45" />
          <rect x="28" y="12" width="3" height="28" fill="var(--tile)" opacity="0.45" />
          <circle cx="16" cy="43" r="2.6" fill="currentColor" />
          <circle cx="32" cy="43" r="2.6" fill="currentColor" />
        </>
      )
    case 'MEDIUM':
      return (
        <g stroke="var(--tile)" strokeWidth="2">
          <rect x="5" y="24" width="19" height="17" rx="3.5" fill="currentColor" />
          <rect x="24" y="24" width="19" height="17" rx="3.5" fill="currentColor" />
          <rect x="14.5" y="8" width="19" height="17" rx="3.5" fill="currentColor" />
          <rect x="12.5" y="24" width="4" height="6" rx="1" fill="var(--tile)" stroke="none" opacity="0.55" />
          <rect x="31.5" y="24" width="4" height="6" rx="1" fill="var(--tile)" stroke="none" opacity="0.55" />
          <rect x="22" y="8" width="4" height="6" rx="1" fill="var(--tile)" stroke="none" opacity="0.55" />
        </g>
      )
    case 'CLOTHES':
      return (
        <>
          <path
            d="M17 7 9.5 10.5 4.5 19l6.5 4 3-2.5V40a2.5 2.5 0 0 0 2.5 2.5h15A2.5 2.5 0 0 0 34 40V20.5l3 2.5 6.5-4-5-8.5L31 7c-1 3.4-3.6 5.5-7 5.5S18 10.4 17 7Z"
            fill="currentColor"
          />
          <path
            d="M24 32.5s-5-3-5-6.2a2.6 2.6 0 0 1 5-1 2.6 2.6 0 0 1 5 1c0 3.2-5 6.2-5 6.2Z"
            fill="var(--tile)"
            opacity="0.6"
          />
        </>
      )
    case 'OTHER':
      return (
        <>
          <path d="M18.5 11a5.5 5.5 0 0 1 11 0" stroke="currentColor" strokeWidth="3" />
          <rect x="10.5" y="10" width="27" height="33" rx="11" fill="currentColor" />
          <rect x="16" y="26" width="16" height="12" rx="4.5" fill="var(--tile)" opacity="0.5" />
          <rect x="21" y="29.5" width="6" height="2.4" rx="1.2" fill="currentColor" />
        </>
      )
  }
}

export function CategoryIcon({ category, size = 'md' }: { category: StoreCategory; size?: 'sm' | 'md' | 'lg' }) {
  return (
    <span className={`category-tile category-tile-${size}`} data-tone={category}>
      <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <Illustration category={category} />
      </svg>
    </span>
  )
}
