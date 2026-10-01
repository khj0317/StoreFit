import { type ChangeEvent, type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getPlaces } from '../api/places'
import { createStore, getStore, updateStore } from '../api/stores'
import { uploadImages } from '../api/uploads'
import { BranchMap } from '../components/BranchMap'
import { CategoryIcon } from '../components/CategoryIcon'
import { Loading } from '../components/Feedback'
import { ImageIcon, PinIcon } from '../components/Icons'
import { Mascot } from '../components/Logo'
import { STORE_CATEGORIES, STORE_CATEGORY_DAILY_RATES, STORE_CATEGORY_LABELS } from '../constants/storeCategories'
import { getErrorMessage } from '../lib/api'
import type { Place, StoreCategory, StoreFormValues, StoreMutationRequest } from '../types'

function countDays(startDate: string, endDate: string): number {
  if (!startDate || !endDate) return 0
  const days = Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86_400_000) + 1
  return days > 0 ? days : 0
}

const VALID_CATEGORIES = new Set<string>(STORE_CATEGORIES.map((category) => category.value))

function isStoreCategory(value: string | undefined): value is StoreCategory {
  return value !== undefined && VALID_CATEGORIES.has(value)
}

function emptyForm(category: StoreCategory): StoreFormValues {
  return {
    placeId: null,
    name: '',
    description: '',
    imageUrls: [],
    category,
    luggageCount: '1',
    startDate: '',
    endDate: '',
  }
}

function toRequest(form: StoreFormValues): StoreMutationRequest {
  return {
    placeId: form.placeId ?? 0,
    name: form.name,
    description: form.description || null,
    imageUrls: form.imageUrls,
    category: form.category,
    luggageCount: Number(form.luggageCount),
    startDate: form.startDate,
    endDate: form.endDate,
  }
}

function todayString(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

export function StoreFormPage() {
  const { storeId, category: categoryParam } = useParams<{ storeId: string; category: string }>()
  const isEdit = storeId !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<StoreFormValues>(() => emptyForm(isStoreCategory(categoryParam) ? categoryParam : 'OTHER'))
  // 수정할 때는 서버의 "남은 자리"에 내 기존 예약이 이미 빠져 있으므로 다시 더해서 보여준다
  const [original, setOriginal] = useState<{ placeId: number; count: number } | null>(null)
  const [locked, setLocked] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [loading, setLoading] = useState(isEdit)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [places, setPlaces] = useState<Place[] | null>(null)
  const [placesError, setPlacesError] = useState('')

  const days = countDays(form.startDate, form.endDate)
  const luggageCount = Number(form.luggageCount) || 0

  useEffect(() => {
    if (!isEdit && !isStoreCategory(categoryParam)) {
      navigate('/my/stores/new', { replace: true })
    }
  }, [isEdit, categoryParam, navigate])

  useEffect(() => {
    if (!isEdit) return
    getStore(Number(storeId))
      .then((store) => {
        setForm({
          placeId: store.placeId,
          name: store.name,
          description: store.description ?? '',
          imageUrls: store.imageUrls,
          category: store.category,
          luggageCount: store.luggageCount.toString(),
          startDate: store.startDate,
          endDate: store.endDate,
        })
        setOriginal({ placeId: store.placeId, count: store.luggageCount })
        setLocked(store.status !== 'PENDING' || store.paymentStatus === 'DONE')
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '짐 보관 정보를 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [isEdit, storeId])

  // 날짜가 정해지면 그 기간에 보관소마다 남은 자리를 다시 불러온다
  useEffect(() => {
    if (days === 0) return
    let ignore = false
    getPlaces(form.startDate, form.endDate)
      .then((data) => {
        if (!ignore) {
          setPlaces(data)
          setPlacesError('')
        }
      })
      .catch((err: unknown) => {
        if (!ignore) setPlacesError(getErrorMessage(err, '보관소 목록을 불러오지 못했습니다.'))
      })
    return () => {
      ignore = true
    }
  }, [form.startDate, form.endDate, days])

  const remainingOf = (place: Place) =>
    (place.remaining ?? place.capacity) + (original && original.placeId === place.id ? original.count : 0)

  const updateField = <K extends keyof StoreFormValues>(key: K, value: StoreFormValues[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleFilesSelected = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files
    if (!files || files.length === 0) return

    setUploadError('')
    setUploading(true)
    try {
      const urls = await uploadImages(Array.from(files))
      setForm((prev) => ({ ...prev, imageUrls: [...prev.imageUrls, ...urls] }))
    } catch (err) {
      setUploadError(getErrorMessage(err, '이미지 업로드에 실패했습니다.'))
    } finally {
      setUploading(false)
      event.target.value = ''
    }
  }

  const handleRemoveImage = (url: string) => {
    setForm((prev) => ({ ...prev, imageUrls: prev.imageUrls.filter((existing) => existing !== url) }))
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (form.placeId === null) {
      setError('짐을 맡길 지점을 선택해주세요.')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      const request = toRequest(form)
      const saved = isEdit ? await updateStore(Number(storeId), request) : await createStore(request)
      navigate(isEdit ? '/my/stores' : `/my/stores/${saved.id}/pay`)
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'))
      // 그 사이 다른 사람이 자리를 가져갔을 수 있으니 남은 자리를 새로 불러온다
      getPlaces(form.startDate, form.endDate).then(setPlaces).catch(() => undefined)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <Loading />
  }

  if (locked) {
    return (
      <div className="centered-layout">
        <div className="centered-card result-card">
          <div className="auth-head">
            <Mascot size={72} mood="sad" />
            <h1>수정할 수 없어요</h1>
          </div>
          <p>결제를 마쳤거나 진행 중인 짐 보관은 수정할 수 없어요. 변경이 필요하면 취소 후 다시 예약해주세요.</p>
          <div className="result-actions">
            <Link to="/my/stores" className="btn btn-primary">
              보관 현황으로
            </Link>
          </div>
        </div>
      </div>
    )
  }

  const estimate = days * STORE_CATEGORY_DAILY_RATES[form.category] * luggageCount

  return (
    <div className="centered-layout">
      <div className="centered-card centered-card-wide">
        {!isEdit && (
          <div className="step-indicator" aria-label="2단계 중 2단계">
            <span />
            <span className="active" />
          </div>
        )}
        <h1 className="form-title">{isEdit ? '예약 수정' : '언제, 어디에 맡길까요?'}</h1>
        <form className="form" onSubmit={handleSubmit}>
          <div className="form-group">
            <span className="form-label">짐 종류</span>
            {isEdit ? (
              <select value={form.category} onChange={(e) => updateField('category', e.target.value as StoreCategory)}>
                {STORE_CATEGORIES.map((category) => (
                  <option key={category.value} value={category.value}>
                    {category.label}
                  </option>
                ))}
              </select>
            ) : (
              <div className="category-selected-row" data-tone={form.category}>
                <CategoryIcon category={form.category} size="sm" />
                <div>
                  <strong>{STORE_CATEGORY_LABELS[form.category]}</strong>
                  <small>하루 {STORE_CATEGORY_DAILY_RATES[form.category].toLocaleString()}원</small>
                </div>
                <Link to="/my/stores/new">변경</Link>
              </div>
            )}
          </div>

          <div className="form-row">
            <label className="form-group">
              <span className="form-label">맡기는 날</span>
              <input
                type="date"
                value={form.startDate}
                min={todayString()}
                onChange={(e) => updateField('startDate', e.target.value)}
                required
              />
            </label>
            <label className="form-group">
              <span className="form-label">찾는 날</span>
              <input
                type="date"
                value={form.endDate}
                min={form.startDate || todayString()}
                onChange={(e) => updateField('endDate', e.target.value)}
                required
              />
            </label>
          </div>

          <label className="form-group">
            <span className="form-label">짐 개수</span>
            <input
              type="number"
              min={1}
              value={form.luggageCount}
              onChange={(e) => updateField('luggageCount', e.target.value)}
              required
            />
          </label>

          <div className="form-group">
            <span className="form-label">
              스토어핏 지점 <span className="form-hint">(정식 운영 중인 지점만 보여요)</span>
            </span>
            {days === 0 ? (
              <p className="place-hint">날짜를 먼저 고르면 그 기간에 자리가 남은 지점을 보여드려요.</p>
            ) : placesError ? (
              <p className="error-text">{placesError}</p>
            ) : places === null ? (
              <p className="place-hint">남은 자리를 확인하는 중이에요...</p>
            ) : places.length === 0 ? (
              <p className="place-hint">아직 운영 중인 지점이 없어요.</p>
            ) : (
              <>
              <BranchMap
                places={places}
                selectedId={form.placeId}
                disabledIds={new Set(places.filter((place) => remainingOf(place) < Math.max(luggageCount, 1)).map((place) => place.id))}
                onSelect={(placeId) => updateField('placeId', placeId)}
              />
              <div className="place-list" role="radiogroup" aria-label="지점 선택">
                {places.map((place) => {
                  const remaining = remainingOf(place)
                  const full = remaining < Math.max(luggageCount, 1)
                  const selected = form.placeId === place.id
                  return (
                    <button
                      key={place.id}
                      type="button"
                      role="radio"
                      aria-checked={selected}
                      className={`place-option${selected ? ' place-option-selected' : ''}`}
                      disabled={full}
                      onClick={() => updateField('placeId', place.id)}
                    >
                      <span className="place-option-body">
                        <strong>{place.name}</strong>
                        <span className="place-option-address">
                          <PinIcon size={13} /> {place.address}
                        </span>
                        {place.description && <span className="place-option-desc">{place.description}</span>}
                      </span>
                      <span className={`place-remaining${full ? ' place-remaining-full' : remaining <= 3 ? ' place-remaining-low' : ''}`}>
                        {full ? '자리 부족' : `${remaining}자리 남음`}
                      </span>
                    </button>
                  )
                })}
              </div>
              </>
            )}
          </div>

          <label className="form-group">
            <span className="form-label">제목</span>
            <input
              value={form.name}
              onChange={(e) => updateField('name', e.target.value)}
              placeholder="예) 방학 동안 맡길 캐리어"
              required
            />
          </label>

          <div className="form-group">
            <span className="form-label">
              사진 <span className="form-hint">(선택)</span>
            </span>
            <div className="image-preview-grid">
              {form.imageUrls.map((url) => (
                <div key={url} className="image-preview">
                  <img src={url} alt="" />
                  <button type="button" onClick={() => handleRemoveImage(url)} aria-label="이미지 삭제">
                    ×
                  </button>
                </div>
              ))}
              <label className="upload-drop" aria-disabled={uploading}>
                <input type="file" accept="image/*" multiple onChange={handleFilesSelected} disabled={uploading} />
                <ImageIcon size={22} />
                {uploading ? '올리는 중' : '사진 추가'}
              </label>
            </div>
            {uploadError && <p className="error-text">{uploadError}</p>}
          </div>

          <label className="form-group">
            <span className="form-label">
              기타사항 <span className="form-hint">(선택)</span>
            </span>
            <textarea
              value={form.description}
              onChange={(e) => updateField('description', e.target.value)}
              rows={3}
              placeholder="깨지기 쉬운 물건이 있다면 알려주세요"
            />
          </label>

          <div className="estimate">
            <span>{days > 0 ? `${days}일 · 짐 ${luggageCount}개 예상 금액` : '날짜를 고르면 금액을 계산해 드려요'}</span>
            <strong>{estimate.toLocaleString()}원</strong>
          </div>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting || uploading}>
            {submitting ? '저장 중...' : isEdit ? '수정 완료' : '예약하고 결제하기'}
          </button>
        </form>
      </div>
    </div>
  )
}
