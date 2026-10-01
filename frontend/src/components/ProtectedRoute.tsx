import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { MemberRole } from '../types'

/** role을 주면 그 역할만 들어올 수 있다 (서버도 같은 규칙으로 막는다) */
export function ProtectedRoute({ children, role }: { children: ReactNode; role?: MemberRole }) {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (role && user?.role !== role) {
    return <Navigate to="/" replace />
  }

  return children
}
