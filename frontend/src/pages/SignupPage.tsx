import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthCard } from '../components/Feedback'
import { PhoneVerifyField } from '../components/PhoneVerifyField'
import { getErrorMessage } from '../lib/api'
import type { MemberRole } from '../types'

export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  const [role, setRole] = useState<MemberRole>('USER')
  const [phone, setPhone] = useState('')
  const [verificationToken, setVerificationToken] = useState<string | null>(null)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!verificationToken) {
      setError('휴대폰 인증을 먼저 완료해주세요.')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      await signup({ username, password, name, phoneNumber: phone, verificationToken, role })
      navigate(role === 'OWNER' ? '/owner/places' : '/', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '회원가입에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="StoreFit 시작하기" subtitle="휴대폰 인증 한 번이면 가입 끝! 예약·보관 소식도 문자로 알려드려요.">
      <form className="form" onSubmit={handleSubmit}>
        <div className="form-group">
          <span className="form-label">회원 유형</span>
          <div className="segmented" role="radiogroup" aria-label="회원 유형">
            <button
              type="button"
              role="radio"
              aria-checked={role === 'USER'}
              className={role === 'USER' ? 'segmented-active' : ''}
              onClick={() => setRole('USER')}
            >
              <strong>짐 맡기는 이용자</strong>
              <span>지점을 예약하고 QR로 맡겨요</span>
            </button>
            <button
              type="button"
              role="radio"
              aria-checked={role === 'OWNER'}
              className={role === 'OWNER' ? 'segmented-active' : ''}
              onClick={() => setRole('OWNER')}
            >
              <strong>지점 사장님</strong>
              <span>지점 운영을 신청하고 QR로 받아요</span>
            </button>
          </div>
        </div>

        <PhoneVerifyField purpose="SIGNUP" phone={phone} onPhoneChange={setPhone} onVerified={setVerificationToken} />

        <label className="form-group">
          <span className="form-label">아이디</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            minLength={4}
            maxLength={20}
            placeholder="영문, 숫자, 밑줄(_) 4~20자"
            autoComplete="username"
            required
          />
        </label>
        <label className="form-group">
          <span className="form-label">비밀번호</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            maxLength={64}
            placeholder="8자 이상"
            autoComplete="new-password"
            required
          />
        </label>
        <label className="form-group">
          <span className="form-label">이름</span>
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={30} autoComplete="name" required />
        </label>

        {error && <p className="error-text">{error}</p>}

        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting || !verificationToken}>
          {submitting ? '가입 중...' : verificationToken ? '회원가입' : '휴대폰 인증 후 가입할 수 있어요'}
        </button>
      </form>
      <p className="auth-switch">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </AuthCard>
  )
}
