import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createStore, getStore, updateStore } from '../api/stores'
import { getErrorMessage } from '../lib/api'
import type { StoreFormValues, StoreMutationRequest } from '../types'

const EMPTY_FORM: StoreFormValues = {
  name: '',
  description: '',
  address: '',
  latitude: '',
  longitude: '',
  pricePerHour: '',
  capacity: '',
  openTime: '',
  closeTime: '',
  imageUrls: '',
}

function toRequest(form: StoreFormValues): StoreMutationRequest {
  return {
    name: form.name,
    description: form.description || null,
    address: form.address,
    latitude: form.latitude ? Number(form.latitude) : null,
    longitude: form.longitude ? Number(form.longitude) : null,
    pricePerHour: Number(form.pricePerHour),
    capacity: Number(form.capacity),
    openTime: form.openTime || null,
    closeTime: form.closeTime || null,
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
          latitude: store.latitude?.toString() ?? '',
          longitude: store.longitude?.toString() ?? '',
          pricePerHour: store.pricePerHour.toString(),
          capacity: store.capacity.toString(),
          openTime: store.openTime?.slice(0, 5) ?? '',
          closeTime: store.closeTime?.slice(0, 5) ?? '',
          imageUrls: store.imageUrls.join('\n'),
        })
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '보관소 정보를 불러오지 못했습니다.')))
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
    <section>
      <h1>{isEdit ? '보관소 수정' : '보관소 등록'}</h1>
      <form className="form" onSubmit={handleSubmit}>
        <label className="form-group">
          <span>이름</span>
          <input value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
        </label>
        <label className="form-group">
          <span>설명</span>
          <textarea value={form.description} onChange={(e) => updateField('description', e.target.value)} />
        </label>
        <label className="form-group">
          <span>주소</span>
          <input value={form.address} onChange={(e) => updateField('address', e.target.value)} required />
        </label>
        <div className="form-row">
          <label className="form-group">
            <span>위도</span>
            <input type="number" step="any" value={form.latitude} onChange={(e) => updateField('latitude', e.target.value)} />
          </label>
          <label className="form-group">
            <span>경도</span>
            <input type="number" step="any" value={form.longitude} onChange={(e) => updateField('longitude', e.target.value)} />
          </label>
        </div>
        <div className="form-row">
          <label className="form-group">
            <span>시간당 가격 (원)</span>
            <input
              type="number"
              min={1}
              value={form.pricePerHour}
              onChange={(e) => updateField('pricePerHour', e.target.value)}
              required
            />
          </label>
          <label className="form-group">
            <span>수용 개수</span>
            <input type="number" min={1} value={form.capacity} onChange={(e) => updateField('capacity', e.target.value)} required />
          </label>
        </div>
        <div className="form-row">
          <label className="form-group">
            <span>오픈 시간</span>
            <input type="time" value={form.openTime} onChange={(e) => updateField('openTime', e.target.value)} />
          </label>
          <label className="form-group">
            <span>마감 시간</span>
            <input type="time" value={form.closeTime} onChange={(e) => updateField('closeTime', e.target.value)} />
          </label>
        </div>
        <label className="form-group">
          <span>이미지 URL (한 줄에 하나씩)</span>
          <textarea
            value={form.imageUrls}
            onChange={(e) => updateField('imageUrls', e.target.value)}
            rows={4}
            placeholder={'https://example.com/1.jpg\nhttps://example.com/2.jpg'}
          />
        </label>

        {error && <p className="error-text">{error}</p>}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '저장 중...' : '저장'}
        </button>
      </form>
    </section>
  )
}
