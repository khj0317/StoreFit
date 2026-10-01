import { useEffect, useState } from 'react'
import { sendPhoneCode, verifyPhoneCode } from '../api/auth'
import { getErrorMessage } from '../lib/api'
import type { VerificationPurpose } from '../types'
import { CheckIcon } from './Icons'

/** 010-1234-5678 형태로 보여준다 (서버에는 숫자만 보내도 된다) */
function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length < 4) return digits
  if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, digits.length - 4)}-${digits.slice(-4)}`
}

const RESEND_SECONDS = 60

/**
 * 휴대폰 번호 입력 + 인증번호 받기 + 확인을 한 번에 처리한다.
 * 인증에 성공하면 onVerified(토큰)을 부르고, 번호를 다시 고치면 onVerified(null)로 인증을 취소한다.
 */
export function PhoneVerifyField({
  purpose,
  phone,
  onPhoneChange,
  onVerified,
  label = '휴대폰 번호',
}: {
  purpose: VerificationPurpose
  phone: string
  onPhoneChange: (phone: string) => void
  onVerified: (token: string | null) => void
  label?: string
}) {
  const [code, setCode] = useState('')
  const [sent, setSent] = useState(false)
  const [devCode, setDevCode] = useState<string | null>(null)
  const [verified, setVerified] = useState(false)
  const [cooldown, setCooldown] = useState(0)
  const [expiresAt, setExpiresAt] = useState<number | null>(null)
  const [now, setNow] = useState(() => Date.now())
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (cooldown <= 0 && expiresAt === null) return
    const timer = window.setInterval(() => {
      setNow(Date.now())
      setCooldown((value) => Math.max(0, value - 1))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [cooldown, expiresAt])

  const digits = phone.replace(/\D/g, '')
  const remaining = expiresAt ? Math.max(0, Math.round((expiresAt - now) / 1000)) : 0

  const handlePhoneChange = (value: string) => {
    onPhoneChange(formatPhone(value))
    if (verified || sent) {
      setVerified(false)
      setSent(false)
      setCode('')
      setDevCode(null)
      setExpiresAt(null)
      onVerified(null)
    }
  }

  const handleSend = async () => {
    setError('')
    setBusy(true)
    try {
      const response = await sendPhoneCode(digits, purpose)
      setSent(true)
      setDevCode(response.devCode)
      setCode('')
      setCooldown(RESEND_SECONDS)
      setExpiresAt(Date.now() + response.expiresInSeconds * 1000)
      setNow(Date.now())
    } catch (err) {
      setError(getErrorMessage(err, '인증번호를 보내지 못했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  const handleVerify = async () => {
    setError('')
    setBusy(true)
    try {
      const token = await verifyPhoneCode(digits, purpose, code)
      setVerified(true)
      setExpiresAt(null)
      onVerified(token)
    } catch (err) {
      setError(getErrorMessage(err, '인증에 실패했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="form-group">
      <span className="form-label">{label}</span>
      <div className="address-input-row">
        <input
          type="tel"
          inputMode="numeric"
          autoComplete="tel"
          value={phone}
          onChange={(e) => handlePhoneChange(e.target.value)}
          placeholder="010-0000-0000"
          disabled={verified}
          required
        />
        {verified ? (
          <span className="verified-chip">
            <CheckIcon size={14} /> 인증 완료
          </span>
        ) : (
          <button
            type="button"
            className="btn btn-soft"
            onClick={handleSend}
            disabled={busy || digits.length < 10 || cooldown > 0}
          >
            {cooldown > 0 ? `재전송 ${cooldown}초` : sent ? '다시 받기' : '인증번호 받기'}
          </button>
        )}
      </div>

      {sent && !verified && (
        <div className="address-input-row">
          <input
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            placeholder="인증번호 6자리"
            aria-label="인증번호"
          />
          <button type="button" className="btn btn-primary" onClick={handleVerify} disabled={busy || code.length !== 6}>
            확인
          </button>
        </div>
      )}

      {sent && !verified && (
        <span className="form-hint">
          {remaining > 0
            ? `문자로 받은 인증번호를 ${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')} 안에 입력해주세요.`
            : '인증 시간이 지났어요. 인증번호를 다시 받아주세요.'}
        </span>
      )}
      {devCode && !verified && <span className="dev-code">개발 모드 · 실제 문자 대신 인증번호를 보여드려요: {devCode}</span>}
      {error && <p className="error-text">{error}</p>}
    </div>
  )
}
