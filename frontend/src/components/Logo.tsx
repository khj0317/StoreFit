type MascotMood = 'happy' | 'sleepy' | 'sad'

const INK = '#3a2e2a'

function Eyes({ mood }: { mood: MascotMood }) {
  if (mood === 'sleepy') {
    return (
      <g stroke={INK} strokeWidth="2.4" strokeLinecap="round" fill="none">
        <path d="M20 38 Q23.5 40.5 27 38" />
        <path d="M37 38 Q40.5 40.5 44 38" />
      </g>
    )
  }
  return (
    <g>
      <circle cx="23.5" cy="38" r="3" fill={INK} />
      <circle cx="40.5" cy="38" r="3" fill={INK} />
      <circle cx="24.6" cy="36.9" r="1" fill="#fff" />
      <circle cx="41.6" cy="36.9" r="1" fill="#fff" />
    </g>
  )
}

function Mouth({ mood }: { mood: MascotMood }) {
  const d = mood === 'sad' ? 'M28.5 46 Q32 43 35.5 46' : mood === 'sleepy' ? 'M30 45 H34' : 'M28.5 43.5 Q32 47 35.5 43.5'
  return <path d={d} stroke={INK} strokeWidth="2.4" strokeLinecap="round" fill="none" />
}

/** 스토어핏 마스코트 — 테이프를 붙인 상자 친구 */
export function Mascot({ size = 64, mood = 'happy', className }: { size?: number; mood?: MascotMood; className?: string }) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect x="10" y="20" width="44" height="36" rx="10" fill="#f6cd97" />
      <rect x="10" y="44" width="44" height="12" rx="6" fill="#efbf86" opacity="0.6" />
      <rect x="6" y="14" width="52" height="12" rx="6" fill="#ebb57a" />
      <rect x="28" y="14" width="8" height="15" rx="2" fill="#7b68ee" />
      <Eyes mood={mood} />
      <ellipse cx="18" cy="44.5" rx="3.6" ry="2.2" fill="#ff8e80" opacity="0.75" />
      <ellipse cx="46" cy="44.5" rx="3.6" ry="2.2" fill="#ff8e80" opacity="0.75" />
      <Mouth mood={mood} />
    </svg>
  )
}

export function LogoMark({ size = 30 }: { size?: number }) {
  return <Mascot size={size} />
}

export function Wordmark() {
  return (
    <span className="wordmark">
      Store<span className="wordmark-accent">Fit</span>
    </span>
  )
}
