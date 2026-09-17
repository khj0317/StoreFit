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
  imageUrls: [],
}

function toRequest(form: StoreFormValues): StoreMutationRequest {
  return {
    name: form.name,
    description: form.description || null,
    address: form.address,
    imageUrls: form.imageUrls,
  }
}

export function StoreFormPage() {
  const { storeId } = useParams<{ storeId: string }>()
  const isEdit = storeId !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<StoreFormValues>(EMPTY_FORM)
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
          imageUrls: store.imageUrls,
        })
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
      const saved = isEdit ? await updateStore(Number(storeId), request) : await createStore(request)
      navigate(`/stores/${saved.id}`)
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p>불러오는 중...</p>
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
