import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../lib/api'
import { BoxIcon, SparkleIcon } from './Icons'

/** 휴대폰 인증 없이 체험 계정으로 둘러보기 (로그인 화면) */
export function DemoEntry() {
  const { demoLogin } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState<'USER' | 'OWNER' | null>(null)
  const [error, setError] = useState('')

  const enter = async (role: 'USER' | 'OWNER') => {
    setError('')
    setLoading(role)
    try {
      await demoLogin(role)
      navigate(role === 'OWNER' ? '/owner' : '/my/stores', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '체험 계정으로 들어가지 못했습니다.'))
    } finally {
      setLoading(null)
    }
  }

  return (
    <section className="demo-entry" aria-labelledby="demo-entry-title">
      <div className="demo-entry-divider">
        <span id="demo-entry-title">회원가입 없이 둘러보기</span>
      </div>
      <div className="demo-entry-buttons">
        <button type="button" className="demo-entry-button" onClick={() => enter('USER')} disabled={loading !== null}>
          <span className="demo-entry-icon">
            <BoxIcon size={20} />
          </span>
          <span className="demo-entry-text">
            <strong>{loading === 'USER' ? '들어가는 중...' : '이용자로 둘러보기'}</strong>
            <small>보관 중인 짐 · 체크인 QR · 예약</small>
          </span>
        </button>
        <button type="button" className="demo-entry-button" onClick={() => enter('OWNER')} disabled={loading !== null}>
          <span className="demo-entry-icon">
            <SparkleIcon size={20} />
          </span>
          <span className="demo-entry-text">
            <strong>{loading === 'OWNER' ? '들어가는 중...' : '사장님으로 둘러보기'}</strong>
            <small>운영 대시보드 · QR 체크인</small>
          </span>
        </button>
      </div>
      {error && <p className="error-text">{error}</p>}
      <p className="demo-entry-note">체험 계정은 여러 사람이 함께 쓰고, 데이터는 매일 처음 상태로 돌아가요. 문자는 보내지 않아요.</p>
    </section>
  )
}

/** 체험 계정으로 둘러보는 중일 때 화면 위에 띄우는 안내 */
export function DemoBanner() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user?.demo) return null

  return (
    <div className="demo-banner" role="status">
      <span>
        {user.role === 'OWNER' ? (
          <>
            <strong>체험 사장님</strong>으로
          </>
        ) : (
          <>
            <strong>체험 이용자</strong>로
          </>
        )}{' '}
        둘러보는 중이에요 · 데이터는 매일 초기화돼요
      </span>
      <button
        type="button"
        className="btn btn-sm btn-secondary"
        onClick={() => {
          logout()
          navigate('/signup')
        }}
      >
        직접 가입하기
      </button>
    </div>
  )
}
