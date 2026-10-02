import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { DemoEntry } from '../components/DemoEntry'
import { AuthCard } from '../components/Feedback'
import { getErrorMessage } from '../lib/api'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(username, password)
      const from = (location.state as { from?: { pathname: string; search: string } } | null)?.from
      navigate(from ? `${from.pathname}${from.search}` : '/', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '로그인에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="다시 만나서 반가워요" subtitle="맡긴 짐, 잘 지내고 있는지 확인해 볼까요?">
        <form className="form" onSubmit={handleSubmit}>
          <label className="form-group">
            <span className="form-label">아이디</span>
            <input value={username} onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label className="form-group">
            <span className="form-label">비밀번호</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting}>
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <div className="auth-links">
          <Link to="/find-username">아이디 찾기</Link>
          <Link to="/reset-password">비밀번호 찾기</Link>
        </div>
        <p className="auth-switch">
          아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
        <DemoEntry />
    </AuthCard>
  )
}
