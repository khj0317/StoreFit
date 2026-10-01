import { type FormEvent, useEffect, useState } from 'react'
import { applyForBranch, getAvailableBranches, getMyApplications, getMyPlaces, updatePlace } from '../../api/owner'
import { EmptyState, Loading } from '../../components/Feedback'
import { BoxIcon, PinIcon, PlusIcon } from '../../components/Icons'
import { getErrorMessage } from '../../lib/api'
import type { BranchApplication, Place } from '../../types'

const APPLICATION_LABELS: Record<BranchApplication['status'], string> = {
  PENDING: '심사 중',
  APPROVED: '승인됨',
  REJECTED: '반려됨',
}

/** 운영자가 정하는 값은 수용량과 소개뿐이다. 이름·주소는 스토어핏 본사가 등록한 정보 */
function OperationForm({
  place,
  submitLabel,
  withMessage,
  onSubmit,
  onCancel,
}: {
  place: Place
  submitLabel: string
  withMessage?: boolean
  onSubmit: (capacity: number, description: string | null, message: string | null) => Promise<void>
  onCancel: () => void
}) {
  const [capacity, setCapacity] = useState(String(place.capacity))
  const [description, setDescription] = useState(place.description ?? '')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await onSubmit(Number(capacity), description.trim() || null, message.trim() || null)
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form branch-form" onSubmit={handleSubmit}>
      <div className="branch-fixed">
        <span className="official-badge">스토어핏 정식 지점</span>
        <strong>{place.name}</strong>
        <span className="row-card-sub">
          <PinIcon size={13} /> {place.address}
        </span>
      </div>
      <label className="form-group">
        <span className="form-label">하루 수용량 (짐 개수)</span>
        <input type="number" min={1} max={1000} value={capacity} onChange={(e) => setCapacity(e.target.value)} required />
      </label>
      <label className="form-group">
        <span className="form-label">
          소개 <span className="form-hint">(선택 · 운영 시간, 찾아오는 길 등)</span>
        </span>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} maxLength={500} />
      </label>
      {withMessage && (
        <label className="form-group">
          <span className="form-label">
            본사에 남길 말 <span className="form-hint">(선택 · 매장 상황, 운영 경험 등)</span>
          </span>
          <textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={2} maxLength={500} />
        </label>
      )}
      {error && <p className="error-text">{error}</p>}
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          닫기
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '보내는 중...' : submitLabel}
        </button>
      </div>
    </form>
  )
}

export function OwnerPlacesPage() {
  const [places, setPlaces] = useState<Place[] | null>(null)
  const [branches, setBranches] = useState<Place[]>([])
  const [applications, setApplications] = useState<BranchApplication[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [picking, setPicking] = useState(false)
  const [applyingId, setApplyingId] = useState<number | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)

  const load = () => {
    Promise.all([getMyPlaces(), getAvailableBranches(), getMyApplications()])
      .then(([mine, available, myApplications]) => {
        setPlaces(mine)
        setBranches(available)
        setApplications(myApplications)
        if (mine.length === 0 && !myApplications.some((application) => application.status === 'PENDING')) setPicking(true)
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '지점을 불러오지 못했습니다.')))
  }

  useEffect(load, [])

  if (error) return <p className="error-text">{error}</p>
  if (!places) return <Loading />

  const pendingPlaceIds = new Set(
    applications.filter((application) => application.status === 'PENDING').map((application) => application.placeId),
  )
  const visibleApplications = applications.filter((application) => application.status !== 'APPROVED')

  return (
    <section className="mypage">
      <div className="page-header">
        <div>
          <span className="page-eyebrow">Owner</span>
          <h1>내 지점</h1>
          <p className="page-subtitle">
            스토어핏 정식 지점 중 운영할 곳을 신청하면 본사 심사 후 운영을 시작해요. 결과는 문자로 알려드려요.
          </p>
        </div>
        {!picking && (
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setPicking(true)}>
            <PlusIcon size={15} /> 지점 운영 신청
          </button>
        )}
      </div>

      {notice && <p className="success-text">{notice}</p>}

      {visibleApplications.length > 0 && (
        <div className="mypage-section">
          <h2>내 운영 신청</h2>
          <ul className="list">
            {visibleApplications.map((application) => (
              <li key={application.id} className="row-card">
                <div className="row-card-body">
                  <strong>{application.placeName}</strong>
                  <span className="row-card-sub">
                    수용량 {application.capacity}개 · {application.createdAt.slice(0, 10)} 신청
                  </span>
                  {application.rejectReason && <p className="row-card-desc">반려 사유: {application.rejectReason}</p>}
                </div>
                <span className={`badge badge-dot ${application.status === 'PENDING' ? 'badge-ready' : 'badge-canceled'}`}>
                  {APPLICATION_LABELS[application.status]}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {picking && (
        <div className="mypage-section">
          <div className="section-header section-header-tight">
            <h2>운영할 지점 선택</h2>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setPicking(false)}>
              닫기
            </button>
          </div>
          {branches.length === 0 ? (
            <p className="place-hint">지금은 운영자를 기다리는 지점이 없어요. 새 지점이 등록되면 여기에 나타나요.</p>
          ) : (
            <ul className="list">
              {branches.map((branch) =>
                applyingId === branch.id ? (
                  <li key={branch.id}>
                    <OperationForm
                      place={branch}
                      submitLabel="운영 신청하기"
                      withMessage
                      onCancel={() => setApplyingId(null)}
                      onSubmit={async (capacity, description, message) => {
                        await applyForBranch(branch.id, { capacity, description, message })
                        setApplyingId(null)
                        setPicking(false)
                        setNotice(`${branch.name} 운영을 신청했어요. 본사 심사가 끝나면 문자로 알려드려요.`)
                        load()
                      }}
                    />
                  </li>
                ) : (
                  <li key={branch.id} className="row-card">
                    <span className="row-card-icon tone-accent">
                      <BoxIcon size={22} />
                    </span>
                    <div className="row-card-body">
                      <strong>{branch.name}</strong>
                      <span className="row-card-sub">
                        <PinIcon size={13} /> {branch.address}
                      </span>
                      {branch.description && <p className="row-card-desc">{branch.description}</p>}
                    </div>
                    <div className="row-card-end">
                      {pendingPlaceIds.has(branch.id) ? (
                        <span className="badge badge-dot badge-ready">심사 중</span>
                      ) : (
                        <button type="button" className="btn btn-soft btn-sm" onClick={() => setApplyingId(branch.id)}>
                          운영 신청
                        </button>
                      )}
                    </div>
                  </li>
                ),
              )}
            </ul>
          )}
        </div>
      )}

      {places.length === 0 && !picking && visibleApplications.length === 0 && (
        <EmptyState title="아직 운영하는 지점이 없어요" description="정식 지점 운영을 신청하면 본사 심사 후 시작할 수 있어요." />
      )}

      {places.length > 0 && (
        <div className="section-header">
          <h2>운영 중</h2>
        </div>
      )}
      <ul className="list">
        {places.map((place) =>
          editingId === place.id ? (
            <li key={place.id}>
              <OperationForm
                place={place}
                submitLabel="수정 완료"
                onCancel={() => setEditingId(null)}
                onSubmit={async (capacity, description) => {
                  await updatePlace(place.id, { capacity, description })
                  setEditingId(null)
                  load()
                }}
              />
            </li>
          ) : (
            <li key={place.id} className="row-card">
              <span className="row-card-icon tone-mint">
                <BoxIcon size={22} />
              </span>
              <div className="row-card-body">
                <strong>{place.name}</strong>
                <span className="row-card-sub">
                  <PinIcon size={13} /> {place.address}
                </span>
                {place.description && <p className="row-card-desc">{place.description}</p>}
              </div>
              <div className="row-card-end">
                <span className="row-card-amount">
                  {place.remaining ?? place.capacity} / {place.capacity}
                </span>
                <span className="row-card-sub">오늘 남은 자리</span>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditingId(place.id)}>
                  수정
                </button>
              </div>
            </li>
          ),
        )}
      </ul>
    </section>
  )
}
