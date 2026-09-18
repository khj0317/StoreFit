import { type ChangeEvent, type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createStore, getStore, updateStore } from '../api/stores'
import { uploadImages } from '../api/uploads'
import { openAddressSearch } from '../lib/daumPostcode'
import { getErrorMessage } from '../lib/api'
import type { StoreFormValues, StoreMutationRequest } from '../types'

const EMPTY_FORM: StoreFormValues = {
  name: '',
  description: '',
  address: '',
  addressDetail: '',
  imageUrls: [],
  luggageCount: '1',
  startTime: '',
  endTime: '',
}

function toRequest(form: StoreFormValues): StoreMutationRequest {
  return {
    name: form.name,
    description: form.description || null,
    address: [form.address, form.addressDetail].filter((part) => part.trim().length > 0).join(' '),
    imageUrls: form.imageUrls,
    luggageCount: Number(form.luggageCount),
    startTime: form.startTime,
    endTime: form.endTime,
  }
}

export function StoreFormPage() {
  const { storeId } = useParams<{ storeId: string }>()
  const isEdit = storeId !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<StoreFormValues>(EMPTY_FORM)
  const [locked, setLocked] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [loading, setLoading] = useState(isEdit)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')

  useEffect(() => {
    if (!isEdit) return
    getStore(Number(storeId))
      .then((store) => {
        setForm({
          name: store.name,
          description: store.description ?? '',
          address: store.address,
          addressDetail: '',
          imageUrls: store.imageUrls,
          luggageCount: store.luggageCount.toString(),
          startTime: store.startTime.slice(0, 16),
          endTime: store.endTime.slice(0, 16),
        })
        setLocked(store.status !== 'PENDING')
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '짐 보관 정보를 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [isEdit, storeId])

  const updateField = <K extends keyof StoreFormValues>(key: K, value: StoreFormValues[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleAddressSearch = () => {
    openAddressSearch((data) => {
      updateField('address', data.roadAddress || data.jibunAddress)
    }).catch((err: unknown) => setError(getErrorMessage(err, '주소 검색을 열지 못했습니다.')))
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
    setError('')
    setSubmitting(true)
    try {
      const request = toRequest(form)
      if (isEdit) {
        await updateStore(Number(storeId), request)
      } else {
        await createStore(request)
      }
      navigate('/my/stores')
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p>불러오는 중...</p>
  }

  if (locked) {
    return (
      <div className="centered-layout">
        <div className="centered-card">
          <h1>수정할 수 없습니다</h1>
          <p>완료되었거나 취소된 짐 보관은 수정할 수 없습니다.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>{isEdit ? '내 짐 보관 수정' : '내 짐 보관'}</h1>
        <form className="form" onSubmit={handleSubmit}>
          <label className="form-group">
            <span>제목</span>
            <input value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
          </label>

          <div className="form-group">
            <span>사진</span>
            {form.imageUrls.length > 0 && (
              <div className="image-preview-grid">
                {form.imageUrls.map((url) => (
                  <div key={url} className="image-preview">
                    <img src={url} alt="" />
                    <button type="button" onClick={() => handleRemoveImage(url)} aria-label="이미지 삭제">
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
            <input type="file" accept="image/*" multiple onChange={handleFilesSelected} disabled={uploading} />
            {uploading && <p>업로드 중...</p>}
            {uploadError && <p className="error-text">{uploadError}</p>}
          </div>

          <label className="form-group">
            <span>주소</span>
            <div className="address-input-row">
              <input
                value={form.address}
                onClick={handleAddressSearch}
                readOnly
                placeholder="클릭해서 주소를 검색해주세요"
                required
              />
              <button type="button" className="btn btn-ghost" onClick={handleAddressSearch}>
                주소 검색
              </button>
            </div>
          </label>

          <label className="form-group">
            <span>상세주소 (선택)</span>
            <input
              value={form.addressDetail}
              onChange={(e) => updateField('addressDetail', e.target.value)}
              placeholder="동/호수 등 상세 주소"
            />
          </label>

          <div className="form-row">
            <label className="form-group">
              <span>짐 개수</span>
              <input
                type="number"
                min={1}
                value={form.luggageCount}
                onChange={(e) => updateField('luggageCount', e.target.value)}
                required
              />
            </label>
          </div>

          <div className="form-row">
            <label className="form-group">
              <span>시작 시간</span>
              <input
                type="datetime-local"
                value={form.startTime}
                onChange={(e) => updateField('startTime', e.target.value)}
                required
              />
            </label>
            <label className="form-group">
              <span>종료 시간</span>
              <input
                type="datetime-local"
                value={form.endTime}
                onChange={(e) => updateField('endTime', e.target.value)}
                required
              />
            </label>
          </div>

          <label className="form-group">
            <span>기타사항</span>
            <textarea value={form.description} onChange={(e) => updateField('description', e.target.value)} rows={3} />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary" disabled={submitting || uploading}>
            {submitting ? '저장 중...' : '저장'}
          </button>
        </form>
      </div>
    </div>
  )
}
