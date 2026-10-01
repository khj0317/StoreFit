import { type FormEvent, useEffect, useState } from 'react'
import {
  approveApplication,
  createBranch,
  getAdminBranches,
  getApplications,
  getNotifications,
  rejectApplication,
  updateBranch,
} from '../../api/admin'
import { EmptyState, Loading } from '../../components/Feedback'
import { PinIcon, PlusIcon } from '../../components/Icons'
import { getErrorMessage } from '../../lib/api'
import { openAddressSearch } from '../../lib/daumPostcode'
import { geocodeAddress, isKakaoMapEnabled } from '../../lib/kakaoMap'
import type { AdminBranch, AdminNotification, BranchApplication, BranchRequest } from '../../types'

type Tab = 'applications' | 'branches' | 'notifications'

const CHANNEL_LABELS: Record<string, string> = { SMS: '문자', ALIMTALK: '알림톡', LOG: '개발 로그' }

// ───────── 운영 신청 심사 ─────────

function ApplicationsTab() {
  const [applications, setApplications] = useState<BranchApplication[] | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)

  const load = () => {
    getApplications()
      .then(setApplications)
      .catch((err: unknown) => setError(getErrorMessage(err, '신청 목록을 불러오지 못했습니다.')))
  }
  useEffect(load, [])

  const decide = async (application: BranchApplication, approve: boolean) => {
    let reason = ''
    if (!approve) {
      const input = window.prompt(`${application.applicantName}님의 ${application.placeName} 신청을 반려할까요? 사유를 적어주세요.`, '')
      if (input === null) return
      reason = input
    } else if (!window.confirm(`${application.applicantName}님에게 ${application.placeName} 운영을 맡길까요? 같은 지점의 다른 신청은 자동으로 반려돼요.`)) {
      return
    }
    setError('')
    setNotice('')
    setBusyId(application.id)
    try {
      if (approve) {
        await approveApplication(application.id)
        setNotice(`${application.placeName}을(를) ${application.applicantName}님에게 맡겼어요. 결과를 문자로 알렸어요.`)
      } else {
        await rejectApplication(application.id, reason)
        setNotice('반려하고 신청자에게 문자로 알렸어요.')
      }
      load()
    } catch (err) {
      setError(getErrorMessage(err, '처리에 실패했습니다.'))
    } finally {
      setBusyId(null)
    }
  }

  if (!applications) return error ? <p className="error-text">{error}</p> : <Loading />

  const pending = applications.filter((application) => application.status === 'PENDING')
  const decided = applications.filter((application) => application.status !== 'PENDING')

  return (
    <>
      {error && <p className="error-text">{error}</p>}
      {notice && <p className="success-text">{notice}</p>}
      <div className="section-header section-header-tight">
        <h2>심사 대기 {pending.length}건</h2>
      </div>
      {pending.length === 0 ? (
        <EmptyState title="심사할 신청이 없어요" description="운영자가 지점 운영을 신청하면 여기에 나타나요." />
      ) : (
        <ul className="list">
          {pending.map((application) => (
            <li key={application.id} className="row-card row-card-top">
              <div className="row-card-body">
                <strong>
                  {application.placeName} · {application.applicantName}
                </strong>
                <span className="row-card-sub">
                  {application.applicantPhone ?? '번호 없음'} · 수용량 {application.capacity}개 · {application.createdAt.slice(0, 16).replace('T', ' ')}
                </span>
                {application.message && <p className="store-card-desc">“{application.message}”</p>}
              </div>
              <div className="list-item-actions">
                <button
                  type="button"
                  className="btn btn-sm btn-text-danger"
                  onClick={() => decide(application, false)}
                  disabled={busyId === application.id}
                >
                  반려
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => decide(application, true)}
                  disabled={busyId === application.id}
                >
                  승인
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {decided.length > 0 && (
        <>
          <div className="section-header">
            <h2>처리한 신청</h2>
          </div>
          <ul className="list">
            {decided.map((application) => (
              <li key={application.id} className="row-card">
                <div className="row-card-body">
                  <strong>
                    {application.placeName} · {application.applicantName}
                  </strong>
                  <span className="row-card-sub">
                    {application.decidedAt?.slice(0, 10)} {application.rejectReason ? `· ${application.rejectReason}` : ''}
                  </span>
                </div>
                <span className={`badge badge-dot ${application.status === 'APPROVED' ? 'badge-completed' : 'badge-canceled'}`}>
                  {application.status === 'APPROVED' ? '승인' : '반려'}
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}

// ───────── 정식 지점 ─────────

const EMPTY_BRANCH: BranchRequest = {
  code: '',
  name: '스토어핏 ',
  address: '',
  description: null,
  capacity: 10,
  latitude: null,
  longitude: null,
}

function BranchForm({
  initial,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initial: BranchRequest
  submitLabel: string
  onSubmit: (request: BranchRequest) => Promise<void>
  onCancel: () => void
}) {
  const [form, setForm] = useState(initial)
  const [error, setError] = useState('')
  const [hint, setHint] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const update = <K extends keyof BranchRequest>(key: K, value: BranchRequest[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const handleAddressSearch = () => {
    openAddressSearch(async (data) => {
      const address = data.roadAddress || data.jibunAddress
      update('address', address)
      // 지도 키가 있으면 주소로 좌표를 자동으로 채운다
      const location = await geocodeAddress(address).catch(() => null)
      if (location) {
        setForm((prev) => ({ ...prev, address, ...location }))
        setHint('주소로 지도 좌표를 채웠어요.')
      } else {
        setHint(isKakaoMapEnabled() ? '좌표를 찾지 못했어요. 직접 입력해주세요.' : '지도 키가 없어 좌표는 직접 입력해야 해요 (비워도 예약은 돼요).')
      }
    }).catch((err: unknown) => setError(getErrorMessage(err, '주소 검색을 열지 못했습니다.')))
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await onSubmit({ ...form, code: form.code.trim().toUpperCase(), description: form.description?.trim() || null })
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  const toNumber = (value: string) => (value.trim() === '' ? null : Number(value))

  return (
    <form className="form branch-form" onSubmit={handleSubmit}>
      <div className="form-row">
        <label className="form-group">
          <span className="form-label">지점 코드</span>
          <input value={form.code} onChange={(e) => update('code', e.target.value)} placeholder="예) MAPO" maxLength={30} required />
        </label>
        <label className="form-group">
          <span className="form-label">기본 수용량</span>
          <input type="number" min={1} max={1000} value={form.capacity} onChange={(e) => update('capacity', Number(e.target.value))} required />
        </label>
      </div>
      <label className="form-group">
        <span className="form-label">지점 이름</span>
        <input value={form.name} onChange={(e) => update('name', e.target.value)} maxLength={100} required />
      </label>
      <div className="form-group">
        <span className="form-label">주소</span>
        <div className="address-input-row">
          <input value={form.address} onClick={handleAddressSearch} readOnly placeholder="주소를 검색해주세요" required />
          <button type="button" className="btn btn-soft" onClick={handleAddressSearch}>
            주소 검색
          </button>
        </div>
        {hint && <span className="form-hint">{hint}</span>}
      </div>
      <div className="form-row">
        <label className="form-group">
          <span className="form-label">위도</span>
          <input inputMode="decimal" value={form.latitude ?? ''} onChange={(e) => update('latitude', toNumber(e.target.value))} placeholder="37.5572" />
        </label>
        <label className="form-group">
          <span className="form-label">경도</span>
          <input inputMode="decimal" value={form.longitude ?? ''} onChange={(e) => update('longitude', toNumber(e.target.value))} placeholder="126.9245" />
        </label>
      </div>
      <label className="form-group">
        <span className="form-label">
          소개 <span className="form-hint">(선택)</span>
        </span>
        <textarea value={form.description ?? ''} onChange={(e) => update('description', e.target.value)} rows={2} maxLength={500} />
      </label>
      {error && <p className="error-text">{error}</p>}
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          닫기
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '저장 중...' : submitLabel}
        </button>
      </div>
    </form>
  )
}

function BranchesTab() {
  const [branches, setBranches] = useState<AdminBranch[] | null>(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<number | 'new' | null>(null)

  const load = () => {
    getAdminBranches()
      .then(setBranches)
      .catch((err: unknown) => setError(getErrorMessage(err, '지점 목록을 불러오지 못했습니다.')))
  }
  useEffect(load, [])

  if (!branches) return error ? <p className="error-text">{error}</p> : <Loading />

  return (
    <>
      <div className="section-header section-header-tight">
        <h2>정식 지점 {branches.length}곳</h2>
        {editing !== 'new' && (
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setEditing('new')}>
            <PlusIcon size={15} /> 지점 등록
          </button>
        )}
      </div>
      {editing === 'new' && (
        <BranchForm
          initial={EMPTY_BRANCH}
          submitLabel="지점 등록"
          onCancel={() => setEditing(null)}
          onSubmit={async (request) => {
            await createBranch(request)
            setEditing(null)
            load()
          }}
        />
      )}
      <ul className="list">
        {branches.map((branch) =>
          editing === branch.id ? (
            <li key={branch.id}>
              <BranchForm
                initial={{
                  code: branch.code ?? '',
                  name: branch.name,
                  address: branch.address,
                  description: branch.description,
                  capacity: branch.capacity,
                  latitude: branch.latitude,
                  longitude: branch.longitude,
                }}
                submitLabel="수정 완료"
                onCancel={() => setEditing(null)}
                onSubmit={async (request) => {
                  await updateBranch(branch.id, request)
                  setEditing(null)
                  load()
                }}
              />
            </li>
          ) : (
            <li key={branch.id} className="row-card">
              <div className="row-card-body">
                <strong>
                  {branch.name} <span className="row-card-mono">{branch.code}</span>
                </strong>
                <span className="row-card-sub">
                  <PinIcon size={13} /> {branch.address}
                  {branch.latitude === null && ' · 좌표 없음'}
                </span>
                <span className="row-card-sub">
                  {branch.operating
                    ? `운영자 ${branch.ownerName} (${branch.ownerPhone ?? '번호 없음'}) · 수용량 ${branch.capacity}`
                    : `운영자 없음${branch.pendingApplications > 0 ? ` · 심사 대기 ${branch.pendingApplications}건` : ''}`}
                </span>
              </div>
              <div className="row-card-end">
                <span className={`badge badge-dot ${branch.operating ? 'badge-completed' : 'badge-pending'}`}>
                  {branch.operating ? '운영 중' : '운영자 모집'}
                </span>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(branch.id)}>
                  수정
                </button>
              </div>
            </li>
          ),
        )}
      </ul>
    </>
  )
}

// ───────── 알림 발송 기록 ─────────

function NotificationsTab() {
  const [notifications, setNotifications] = useState<AdminNotification[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getNotifications()
      .then(setNotifications)
      .catch((err: unknown) => setError(getErrorMessage(err, '알림 기록을 불러오지 못했습니다.')))
  }, [])

  if (!notifications) return error ? <p className="error-text">{error}</p> : <Loading />
  if (notifications.length === 0) return <EmptyState title="보낸 알림이 없어요" />

  return (
    <ul className="list">
      {notifications.map((notification) => (
        <li key={notification.id} className="row-card row-card-top">
          <div className="row-card-body">
            <strong>
              {notification.phoneNumber} · {notification.type}
            </strong>
            <p className="notification-content">{notification.content}</p>
            {notification.error && <p className="error-text">{notification.error}</p>}
          </div>
          <div className="row-card-end">
            <span className={`badge badge-dot ${notification.status === 'SENT' ? 'badge-completed' : notification.status === 'FAILED' ? 'badge-danger' : 'badge-pending'}`}>
              {notification.status === 'SENT'
                ? CHANNEL_LABELS[notification.channel ?? ''] ?? '발송'
                : notification.status === 'FAILED'
                  ? '실패'
                  : '대기'}
            </span>
            <span className="row-card-sub">{notification.createdAt.slice(5, 16).replace('T', ' ')}</span>
          </div>
        </li>
      ))}
    </ul>
  )
}

export function AdminPage() {
  const [tab, setTab] = useState<Tab>('applications')

  return (
    <section>
      <div className="page-header">
        <div>
          <span className="page-eyebrow">Admin</span>
          <h1>본사 관리</h1>
          <p className="page-subtitle">정식 지점을 등록하고, 운영 신청을 심사하고, 보낸 알림을 확인해요.</p>
        </div>
      </div>

      <div className="tabs" role="tablist">
        {(
          [
            ['applications', '운영 신청 심사'],
            ['branches', '정식 지점'],
            ['notifications', '알림 기록'],
          ] as const
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            className={tab === key ? 'nav-pill nav-pill-active' : 'nav-pill'}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'applications' && <ApplicationsTab />}
      {tab === 'branches' && <BranchesTab />}
      {tab === 'notifications' && <NotificationsTab />}
    </section>
  )
}
