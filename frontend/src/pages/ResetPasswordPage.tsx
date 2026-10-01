import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { resetPassword } from '../api/auth'
import { AuthCard } from '../components/Feedback'
import { PhoneVerifyField } from '../components/PhoneVerifyField'
import { getErrorMessage } from '../lib/api'

export function ResetPasswordPage() {
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [phone, setPhone] = useState('')
  const [verificationToken, setVerificationToken] = useState<string | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!verificationToken) return
    setError('')

    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 일치하지 않습니다.')
      return
    }

    setSubmitting(true)
    try {
      await resetPassword({ username, phoneNumber: phone, verificationToken, newPassword })
      window.alert('비밀번호를 바꿨어요. 새 비밀번호로 로그인해주세요.')
      navigate('/login', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '비밀번호를 재설정하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="비밀번호 재설정" subtitle="아이디와 가입한 휴대폰 번호로 본인 확인 후 바꿔 드릴게요.">
      <form className="form" onSubmit={handleSubmit}>
        <label className="form-group">
          <span className="form-label">아이디</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>

        <PhoneVerifyField purpose="RESET_PASSWORD" phone={phone} onPhoneChange={setPhone} onVerified={setVerificationToken} />

        <label className="form-group">
          <span className="form-label">새 비밀번호</span>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            maxLength={64}
            autoComplete="new-password"
            required
          />
        </label>
        <label className="form-group">
          <span className="form-label">새 비밀번호 확인</span>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            minLength={8}
            maxLength={64}
            autoComplete="new-password"
            required
          />
        </label>

        {error && <p className="error-text">{error}</p>}

        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting || !verificationToken}>
          {submitting ? '변경 중...' : '비밀번호 변경'}
        </button>
      </form>
      <p className="auth-switch">
        <Link to="/login">로그인으로 돌아가기</Link>
      </p>
    </AuthCard>
  )
}
