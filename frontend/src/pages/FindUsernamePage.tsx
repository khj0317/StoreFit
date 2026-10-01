import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { findUsername } from '../api/auth'
import { AuthCard } from '../components/Feedback'
import { PhoneVerifyField } from '../components/PhoneVerifyField'
import { getErrorMessage } from '../lib/api'

export function FindUsernamePage() {
  const [phone, setPhone] = useState('')
  const [verificationToken, setVerificationToken] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [foundUsername, setFoundUsername] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!verificationToken) return
    setError('')
    setSubmitting(true)
    try {
      const result = await findUsername({ phoneNumber: phone, verificationToken })
      setFoundUsername(result.username)
    } catch (err) {
      setError(getErrorMessage(err, '아이디를 찾지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="아이디 찾기" subtitle="가입할 때 인증한 휴대폰 번호로 찾아드려요.">
      {foundUsername ? (
        <>
          <p className="auth-switch">회원님의 아이디를 찾았어요!</p>
          <p className="found-username">{foundUsername}</p>
          <Link to="/login" className="btn btn-primary btn-lg btn-block">
            로그인하기
          </Link>
        </>
      ) : (
        <form className="form" onSubmit={handleSubmit}>
          <PhoneVerifyField purpose="FIND_USERNAME" phone={phone} onPhoneChange={setPhone} onVerified={setVerificationToken} />

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting || !verificationToken}>
            {submitting ? '확인 중...' : '아이디 찾기'}
          </button>
        </form>
      )}
      <div className="auth-links">
        <Link to="/reset-password">비밀번호 찾기</Link>
        <Link to="/login">로그인</Link>
      </div>
    </AuthCard>
  )
}
