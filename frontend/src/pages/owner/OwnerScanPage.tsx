import QrScanner from 'qr-scanner'
import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react'
import { checkIn, checkOut, lookupCheckIn } from '../../api/owner'
import { CategoryIcon } from '../../components/CategoryIcon'
import { CheckIcon } from '../../components/Icons'
import { StatusBadge } from '../../components/StatusBadge'
import { STORE_CATEGORY_LABELS } from '../../constants/storeCategories'
import { getErrorMessage } from '../../lib/api'
import type { OwnerReservation } from '../../types'

type CameraState = 'idle' | 'starting' | 'on' | 'unavailable'

/**
 * 손님 QR을 스캔해서 체크인·체크아웃한다.
 * 스캔하면 바로 처리하지 않고 예약 내용을 먼저 보여준 뒤, 사장님이 확인 버튼을 눌러야 처리한다.
 * 카메라를 쓸 수 없으면 QR 아래의 8자리 코드를 직접 입력할 수 있다.
 */
export function OwnerScanPage() {
  const videoRef = useRef<HTMLVideoElement>(null)
  const scannerRef = useRef<QrScanner | null>(null)
  const [camera, setCamera] = useState<CameraState>('idle')
  const [code, setCode] = useState('')
  const [reservation, setReservation] = useState<OwnerReservation | null>(null)
  const [error, setError] = useState('')
  const [done, setDone] = useState('')
  const [busy, setBusy] = useState(false)

  const lookup = useCallback(async (raw: string) => {
    const value = raw.trim()
    if (!value) return
    setError('')
    setDone('')
    setBusy(true)
    try {
      setReservation(await lookupCheckIn(value))
      setCode(value)
      scannerRef.current?.stop()
      setCamera('idle')
    } catch (err) {
      setReservation(null)
      setError(getErrorMessage(err, '예약을 찾지 못했습니다.'))
    } finally {
      setBusy(false)
    }
  }, [])

  const startCamera = async () => {
    if (!videoRef.current) return
    setError('')
    setCamera('starting')
    try {
      if (!scannerRef.current) {
        scannerRef.current = new QrScanner(videoRef.current, (result) => void lookup(result.data), {
          returnDetailedScanResult: true,
          highlightScanRegion: true,
          maxScansPerSecond: 3,
        })
      }
      await scannerRef.current.start()
      setCamera('on')
    } catch {
      setCamera('unavailable')
    }
  }

  useEffect(() => {
    return () => {
      scannerRef.current?.destroy()
      scannerRef.current = null
    }
  }, [])

  const handleManualSubmit = (event: FormEvent) => {
    event.preventDefault()
    void lookup(code)
  }

  const handleAction = async () => {
    if (!reservation?.checkInCode) return
    setError('')
    setBusy(true)
    try {
      const updated =
        reservation.nextAction === 'CHECK_IN' ? await checkIn(reservation.checkInCode) : await checkOut(reservation.checkInCode)
      setDone(
        updated.status === 'IN_USE'
          ? `${updated.customerName}님의 짐 ${updated.luggageCount}개를 받았어요. 보관을 시작합니다.`
          : updated.overdueFee > 0
            ? `${updated.customerName}님께 짐을 돌려드렸어요. 연체료 ${updated.overdueFee.toLocaleString()}원을 받아주세요.`
            : `${updated.customerName}님께 짐을 돌려드렸어요. 보관이 끝났습니다.`,
      )
      setReservation(updated)
    } catch (err) {
      setError(getErrorMessage(err, '처리에 실패했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  const reset = () => {
    setReservation(null)
    setCode('')
    setDone('')
    setError('')
  }

  return (
    <section className="mypage">
      <div className="page-header">
        <div>
          <span className="page-eyebrow">Owner</span>
          <h1>QR 체크인</h1>
          <p className="page-subtitle">손님이 보여주는 QR을 스캔하면 짐을 받거나 돌려드릴 수 있어요.</p>
        </div>
      </div>

      <div className="mypage-section scan-panel">
        <div className={`scan-video-wrap${camera === 'on' ? ' scan-video-on' : ''}`}>
          <video ref={videoRef} className="scan-video" muted playsInline />
          {camera !== 'on' && (
            <div className="scan-placeholder">
              {camera === 'unavailable' ? (
                <p>카메라를 사용할 수 없어요. 권한을 허용했는지 확인하거나, 아래에 코드를 직접 입력해주세요.</p>
              ) : (
                <button type="button" className="btn btn-primary btn-lg" onClick={startCamera} disabled={camera === 'starting'}>
                  {camera === 'starting' ? '카메라 켜는 중...' : '카메라로 스캔하기'}
                </button>
              )}
            </div>
          )}
        </div>

        <form className="form-inline form" onSubmit={handleManualSubmit}>
          <label className="form-group">
            <span className="form-label">또는 코드 직접 입력</span>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="예) ABCD-2345"
              autoCapitalize="characters"
              autoComplete="off"
            />
          </label>
          <button type="submit" className="btn btn-soft" disabled={busy || !code.trim()}>
            조회
          </button>
        </form>
      </div>

      {error && <p className="error-text">{error}</p>}
      {done && (
        <p className="success-text">
          <CheckIcon size={14} /> {done}
        </p>
      )}

      {reservation && (
        <div className="mypage-section scan-result" data-tone={reservation.category}>
          <div className="receipt-head">
            <CategoryIcon category={reservation.category} size="md" />
            <div>
              <strong>{reservation.customerName}님</strong>
              <span>{reservation.customerPhone ?? '연락처 없음'}</span>
            </div>
            <StatusBadge status={reservation.status} paid={reservation.paid} />
          </div>
          <div className="payment-summary-row">
            <span>지점</span>
            <span>{reservation.placeName}</span>
          </div>
          <div className="payment-summary-row">
            <span>짐</span>
            <span>
              {STORE_CATEGORY_LABELS[reservation.category]} {reservation.luggageCount}개 · {reservation.name}
            </span>
          </div>
          {reservation.overdueFee > 0 && (
            <div className="payment-summary-row">
              <span>연체료 (현장 결제)</span>
              <span className="overdue-amount">
                {reservation.overdueDays}일 · {reservation.overdueFee.toLocaleString()}원
              </span>
            </div>
          )}
          <div className="payment-summary-row">
            <span>기간</span>
            <span>
              {reservation.startDate} ~ {reservation.endDate}
            </span>
          </div>

          <div className="result-actions">
            {reservation.nextAction === 'CHECK_IN' && (
              <button type="button" className="btn btn-primary btn-lg" onClick={handleAction} disabled={busy}>
                짐 {reservation.luggageCount}개 받기 (체크인)
              </button>
            )}
            {reservation.nextAction === 'CHECK_OUT' && (
              <button type="button" className="btn btn-secondary btn-lg" onClick={handleAction} disabled={busy}>
                짐 돌려드리기 (체크아웃)
              </button>
            )}
            {reservation.nextAction === 'NONE' && !done && (
              <p className="place-hint">
                {reservation.status === 'PENDING'
                  ? reservation.paid
                    ? `보관 시작일(${reservation.startDate}) 당일에만 체크인할 수 있어요.`
                    : '아직 결제하지 않은 예약이에요.'
                  : '이미 처리가 끝난 예약이에요.'}
              </p>
            )}
            <button type="button" className="btn btn-ghost" onClick={reset}>
              다음 손님 스캔
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
