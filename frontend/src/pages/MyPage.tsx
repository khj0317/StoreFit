import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changePassword, deleteAccount, getMyProfile, updateProfile } from '../api/members'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../lib/api'
import type { MemberProfile } from '../types'

function ProfileSection({ profile, onUpdated }: { profile: MemberProfile; onUpdated: (profile: MemberProfile) => void }) {
  const { updateDisplayName } = useAuth()
  const [name, setName] = useState(profile.name)
  const [email, setEmail] = useState(profile.email ?? '')
  const [phoneNumber, setPhoneNumber] = useState(profile.phoneNumber ?? '')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const updated = await updateProfile({ name, email: email || undefined, phoneNumber: phoneNumber || undefined })
      onUpdated(updated)
      updateDisplayName(updated.name)
      setSuccess('회원정보를 수정했습니다.')
    } catch (err) {
      setError(getErrorMessage(err, '회원정보 수정에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mypage-section">
      <h2>회원정보</h2>
      <form className="form" onSubmit={handleSubmit}>
        <label className="form-group">
          <span>아이디</span>
          <input value={profile.username} disabled />
        </label>
        <label className="form-group">
          <span>이름</span>
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label className="form-group">
          <span>이메일</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label className="form-group">
          <span>전화번호</span>
          <input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
        </label>
        {error && <p className="error-text">{error}</p>}
        {success && <p className="success-text">{success}</p>}
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '저장 중...' : '저장'}
        </button>
      </form>
    </section>
  )
}

function PasswordSection() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 일치하지 않습니다.')
      return
    }

    setSubmitting(true)
    try {
      await changePassword({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSuccess('비밀번호를 변경했습니다.')
    } catch (err) {
      setError(getErrorMessage(err, '비밀번호 변경에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mypage-section">
      <h2>비밀번호 변경</h2>
      <form className="form" onSubmit={handleSubmit}>
        <label className="form-group">
          <span>현재 비밀번호</span>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
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
        {success && <p className="success-text">{success}</p>}
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '변경 중...' : '비밀번호 변경'}
        </button>
      </form>
    </section>
  )
}

function DeleteAccountSection() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')

    if (!window.confirm('정말 회원을 탈퇴하시겠습니까? 모든 짐 보관 정보가 함께 삭제되며 되돌릴 수 없습니다.')) {
      return
    }

    setSubmitting(true)
    try {
      await deleteAccount({ password })
      logout()
      navigate('/', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '회원 탈퇴에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mypage-section danger-zone">
      <h2>회원 탈퇴</h2>
      <p className="page-subtitle">탈퇴하면 등록한 모든 짐 보관 정보와 결제 내역이 함께 삭제됩니다.</p>
      <form className="form form-inline" onSubmit={handleSubmit}>
        <label className="form-group">
          <span>비밀번호 확인</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <p className="error-text">{error}</p>}
        <button type="submit" className="btn btn-danger" disabled={submitting}>
          {submitting ? '처리 중...' : '회원 탈퇴'}
        </button>
      </form>
    </section>
  )
}

export function MyPage() {
  const [profile, setProfile] = useState<MemberProfile | null>(null)
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')

  useEffect(() => {
    getMyProfile()
      .then((data) => {
        setProfile(data)
        setStatus('ready')
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '회원정보를 불러오지 못했습니다.'))
        setStatus('error')
      })
  }, [])

  return (
    <section className="mypage">
      <div className="page-header">
        <h1>마이페이지</h1>
      </div>

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && profile && (
        <>
          <ProfileSection profile={profile} onUpdated={setProfile} />
          <PasswordSection />
          <DeleteAccountSection />
        </>
      )}
    </section>
  )
}
