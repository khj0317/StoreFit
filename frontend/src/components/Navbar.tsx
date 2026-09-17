import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { LogoMark, Wordmark } from './Logo'

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <Link to="/" className="navbar-brand">
        <LogoMark />
        <Wordmark />
      </Link>
      <nav className="navbar-links">
        <Link to="/">짐 보관 찾기</Link>
        {isAuthenticated && <Link to="/my/reservations">내 예약</Link>}
        {isAuthenticated && <Link to="/my/stores">내 짐 보관</Link>}
      </nav>
      <div className="navbar-actions">
        {isAuthenticated ? (
          <>
            <span className="navbar-user">{user?.name}님</span>
            <button type="button" className="btn btn-ghost" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-ghost">
              로그인
            </Link>
            <Link to="/signup" className="btn btn-primary">
              회원가입
            </Link>
          </>
        )}
      </div>
    </header>
  )
}
