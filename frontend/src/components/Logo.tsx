export function LogoMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M4 15L16 5L28 15" stroke="var(--accent)" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="9" y="16" width="14" height="11" rx="2.5" fill="var(--accent)" />
      <rect x="13" y="20" width="6" height="1.6" rx="0.8" fill="var(--bg)" />
    </svg>
  )
}

export function Wordmark() {
  return (
    <span className="wordmark">
      STORE<span className="wordmark-accent">FIT</span>
    </span>
  )
}
