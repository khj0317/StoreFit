import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { resetPassword } from '../api/auth'
import { getErrorMessage } from '../lib/api'

export function ResetPasswordPage() {
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 일치하지 않습니다.')
      return
    }

    setSubmitting(true)
    try {
      await resetPassword({ username, email, name, newPassword })
      navigate('/login', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '비밀번호를 재설정하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>비밀번호 찾기</h1>
        <form className="form" onSubmit={handleSubmit}>
          <label className="form-group">
            <span>아이디</span>
            <input value={username} onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label className="form-group">
            <span>이메일</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="form-group">
            <span>이름</span>
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          <label className="form-group">
            <span>새 비밀번호</span>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={8}
              maxLength={64}
              required
            />
          </label>
          <label className="form-group">
            <span>새 비밀번호 확인</span>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              minLength={8}
              maxLength={64}
              required
            />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
        <p className="auth-switch">
          가입할 때 이메일을 입력하지 않았다면 비밀번호를 재설정할 수 없어요.
        </p>
        <p className="auth-switch">
          <Link to="/login">로그인으로 돌아가기</Link>
        </p>
      </div>
    </div>
  )
}
