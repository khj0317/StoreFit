import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createStore, getStore, updateStore } from '../api/stores'
import { getErrorMessage } from '../lib/api'
import type { StoreFormValues, StoreMutationRequest } from '../types'

const EMPTY_FORM: StoreFormValues = {
  name: '',
  description: '',
  address: '',
  imageUrls: '',
}

function toRequest(form: StoreFormValues): StoreMutationRequest {
  return {
    name: form.name,
    description: form.description || null,
    address: form.address,
    imageUrls: form.imageUrls
      .split('\n')
      .map((url) => url.trim())
      .filter((url) => url.length > 0),
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

  useEffect(() => {
    if (!isEdit) return
    getStore(Number(storeId))
      .then((store) => {
        setForm({
          name: store.name,
          description: store.description ?? '',
          address: store.address,
          imageUrls: store.imageUrls.join('\n'),
        })
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '짐 보관 정보를 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [isEdit, storeId])

  const updateField = <K extends keyof StoreFormValues>(key: K, value: StoreFormValues[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
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
          <label className="form-group">
            <span>사진 URL (한 줄에 하나씩)</span>
            <textarea
              value={form.imageUrls}
              onChange={(e) => updateField('imageUrls', e.target.value)}
              rows={4}
              placeholder={'https://example.com/1.jpg\nhttps://example.com/2.jpg'}
            />
          </label>
          <label className="form-group">
            <span>주소</span>
            <input value={form.address} onChange={(e) => updateField('address', e.target.value)} required />
          </label>
          <label className="form-group">
            <span>기타사항</span>
            <textarea value={form.description} onChange={(e) => updateField('description', e.target.value)} rows={3} />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? '저장 중...' : '저장'}
          </button>
        </form>
      </div>
    </div>
  )
}
