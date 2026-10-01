import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changePassword, changePhone, deleteAccount, getMyProfile, updateProfile } from '../api/members'
import { useAuth } from '../auth/AuthContext'
import { Loading } from '../components/Feedback'
import { PhoneVerifyField } from '../components/PhoneVerifyField'
import { getErrorMessage } from '../lib/api'
import type { MemberProfile } from '../types'

function ProfileSection({ profile, onUpdated }: { profile: MemberProfile; onUpdated: (profile: MemberProfile) => void }) {
  const { updateDisplayName } = useAuth()
  const [name, setName] = useState(profile.name)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const updated = await updateProfile({ name })
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
          <span className="form-label">아이디</span>
          <input value={profile.username} disabled />
        </label>
        <label className="form-group">
          <span className="form-label">이름</span>
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={30} required />
        </label>
        {error && <p className="error-text">{error}</p>}
        {success && <p className="success-text">{success}</p>}
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting}>
          {submitting ? '저장 중...' : '저장'}
        </button>
      </form>
    </section>
  )
}

/** 예약·보관 알림 문자가 가는 번호. 새 번호로 인증해야 바뀐다 */
function PhoneSection({ profile, onUpdated }: { profile: MemberProfile; onUpdated: (profile: MemberProfile) => void }) {
  const [editing, setEditing] = useState(false)
  const [phone, setPhone] = useState('')
  const [verificationToken, setVerificationToken] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!verificationToken) return
    setError('')
    setSubmitting(true)
    try {
      const updated = await changePhone({ phoneNumber: phone, verificationToken })
      onUpdated(updated)
      setEditing(false)
      setPhone('')
      setVerificationToken(null)
      setSuccess('휴대폰 번호를 바꿨어요. 이제 이 번호로 알림을 보내드려요.')
    } catch (err) {
      setError(getErrorMessage(err, '휴대폰 번호를 바꾸지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mypage-section">
      <h2>휴대폰 번호</h2>
      <p>예약 확정, 체크인, 만료 안내 같은 알림을 이 번호로 문자·카카오톡으로 보내드려요.</p>
      {!editing ? (
        <div className="phone-row">
          <strong>{profile.phoneNumber ?? '등록된 번호가 없어요'}</strong>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}>
            번호 변경
          </button>
        </div>
      ) : (
        <form className="form" onSubmit={handleSubmit}>
          <PhoneVerifyField
            purpose="CHANGE_PHONE"
            label="새 휴대폰 번호"
            phone={phone}
            onPhoneChange={setPhone}
            onVerified={setVerificationToken}
          />
          {error && <p className="error-text">{error}</p>}
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setEditing(false)}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting || !verificationToken}>
              {submitting ? '변경 중...' : '이 번호로 변경'}
            </button>
          </div>
        </form>
      )}
      {success && <p className="success-text">{success}</p>}
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
          <span className="form-label">현재 비밀번호</span>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
        </label>
        <label className="form-group">
          <span className="form-label">새 비밀번호</span>
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
          <span className="form-label">새 비밀번호 확인</span>
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
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting}>
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
      <p>탈퇴하면 등록한 모든 짐 보관 정보와 결제 내역이 함께 삭제되며 되돌릴 수 없어요. 결제했거나 맡겨둔 짐이 있으면 보관을 마친 뒤 탈퇴할 수 있어요.</p>
      <form className="form form-inline" onSubmit={handleSubmit}>
        <label className="form-group">
          <span className="form-label">비밀번호 확인</span>
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
      {status === 'loading' && <Loading />}
      {status === 'error' && <p className="error-text">{error}</p>}
      {status === 'ready' && profile && (
        <>
          <div className="profile-hero">
            <span className="avatar avatar-lg">{profile.name.slice(0, 1)}</span>
            <div>
              <strong>{profile.name}님</strong>
              <span>@{profile.username}</span>
            </div>
          </div>
          <ProfileSection profile={profile} onUpdated={setProfile} />
          <PhoneSection profile={profile} onUpdated={setProfile} />
          <PasswordSection />
          <DeleteAccountSection />
        </>
      )}
    </section>
  )
}
