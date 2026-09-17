import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../lib/api'

export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await signup({ email, password, name, phoneNumber: phoneNumber || undefined })
      navigate('/', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '회원가입에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>회원가입</h1>
        <form className="form" onSubmit={handleSubmit}>
          <label className="form-group">
            <span>이름</span>
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          <label className="form-group">
            <span>이메일</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="form-group">
            <span>비밀번호</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              maxLength={64}
              required
            />
          </label>
          <label className="form-group">
            <span>전화번호 (선택)</span>
            <input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? '가입 중...' : '회원가입'}
          </button>
        </form>
        <p className="auth-switch">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </div>
  )
}
