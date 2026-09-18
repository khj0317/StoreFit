import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <section className="hero-section">
      <div className="hero-content">
        <h1>
          이동은 <span className="hero-highlight-blue">가볍게</span>,
          <br />
          짐은 <span className="hero-highlight-yellow">안전하게</span>!
        </h1>
        <p className="hero-subtitle">
          대학생 · 1인 가구를 위한 맞춤형 짐 보관 솔루션,
          <br />
          짐을 맡길 곳과 기간을 한 번에 기록하세요.
        </p>
        {isAuthenticated ? (
          <Link to="/my/stores/new" className="btn btn-secondary btn-lg">
            내 짐 보관하기 →
          </Link>
        ) : (
          <Link to="/signup" className="btn btn-secondary btn-lg">
            지금 시작하기 →
          </Link>
        )}
      </div>
    </section>
  )
}
