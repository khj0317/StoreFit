import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { findUsername } from '../api/auth'
import { getErrorMessage } from '../lib/api'

export function FindUsernamePage() {
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [foundUsername, setFoundUsername] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const result = await findUsername({ email, name })
      setFoundUsername(result.username)
    } catch (err) {
      setError(getErrorMessage(err, '아이디를 찾지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>아이디 찾기</h1>

        {foundUsername ? (
          <>
            <p className="success-text">회원님의 아이디는 다음과 같습니다.</p>
            <p className="found-username">{foundUsername}</p>
            <Link to="/login" className="btn btn-primary">
              로그인하기
            </Link>
          </>
        ) : (
          <>
            <form className="form" onSubmit={handleSubmit}>
              <label className="form-group">
                <span>이메일</span>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </label>
              <label className="form-group">
                <span>이름</span>
                <input value={name} onChange={(e) => setName(e.target.value)} required />
              </label>

              {error && <p className="error-text">{error}</p>}

              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? '확인 중...' : '아이디 찾기'}
              </button>
            </form>
            <p className="auth-switch">
              가입할 때 이메일을 입력하지 않았다면 아이디를 찾을 수 없어요.
            </p>
          </>
        )}
      </div>
    </div>
  )
}
