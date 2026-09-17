import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../lib/api'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
      const from = (location.state as { from?: { pathname: string; search: string } } | null)?.from
      navigate(from ? `${from.pathname}${from.search}` : '/', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '로그인에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>로그인</h1>
        <form className="form" onSubmit={handleSubmit}>
          <label className="form-group">
            <span>이메일</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="form-group">
            <span>비밀번호</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <p className="auth-switch">
          아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
      </div>
    </div>
  )
}
