import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { LogoMark, Wordmark } from './Logo'

export function Navbar() {
  const { user, isAuthenticated, isOwner, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-pill nav-pill-active' : 'nav-pill')

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <LogoMark />
          <Wordmark />
          {isOwner && <span className="owner-tag">사장님</span>}
          {isAdmin && <span className="owner-tag">본사</span>}
        </Link>
        <nav className="navbar-links">
          {isAuthenticated && isOwner && (
            <>
              <NavLink to="/owner" end className={navClass}>
                운영 대시보드
              </NavLink>
              <NavLink to="/owner/scan" className={navClass}>
                QR 체크인
              </NavLink>
              <NavLink to="/owner/places" className={navClass}>
                내 지점
              </NavLink>
            </>
          )}
          {isAuthenticated && isAdmin && (
            <NavLink to="/admin" className={navClass}>
              본사 관리
            </NavLink>
          )}
          {isAuthenticated && !isOwner && !isAdmin && (
            <>
              <NavLink to="/my/stores/new" className={navClass}>
                짐 보관하기
              </NavLink>
              <NavLink to="/my/stores" end className={navClass}>
                보관 현황
              </NavLink>
              <NavLink to="/my/payments" className={navClass}>
                결제 내역
              </NavLink>
            </>
          )}
        </nav>
        <div className="navbar-actions">
          {isAuthenticated ? (
            <>
              <Link to="/my/profile" className="navbar-user">
                <span className="avatar avatar-sm">{user?.name?.slice(0, 1)}</span>
                <span className="navbar-user-name">{user?.name}님</span>
              </Link>
              <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">
                로그인
              </Link>
              <Link to="/signup" className="btn btn-primary btn-sm">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
