import type { ReactNode } from 'react'
import { Mascot } from './Logo'

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <Mascot size={88} mood="sleepy" className="float-slow" />
      <strong>{title}</strong>
      {description && <p>{description}</p>}
      {action}
    </div>
  )
}

export function Loading({ label = '불러오는 중이에요' }: { label?: string }) {
  return (
    <div className="loading" role="status">
      <Mascot size={52} className="bounce" />
      <span>{label}</span>
    </div>
  )
}

export function AuthCard({
  title,
  subtitle,
  mood = 'happy',
  children,
}: {
  title: string
  subtitle?: string
  mood?: 'happy' | 'sleepy' | 'sad'
  children: ReactNode
}) {
  return (
    <div className="centered-layout">
      <div className="centered-card">
        <div className="auth-head">
          <Mascot size={72} mood={mood} className="float-slow" />
          <h1>{title}</h1>
          {subtitle && <p className="auth-subtitle">{subtitle}</p>}
        </div>
        {children}
      </div>
    </div>
  )
}
