import QRCode from 'qrcode'
import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import type { StoreRecord } from '../types'

/** 운영자에게 보여줄 체크인 QR. 카메라가 안 되면 운영자가 아래 코드를 직접 입력할 수 있다 */
export function QrModal({ store, onClose }: { store: StoreRecord; onClose: () => void }) {
  const [svg, setSvg] = useState('')
  const code = store.checkInCode ?? ''

  useEffect(() => {
    QRCode.toString(code, { type: 'svg', margin: 1, width: 240, color: { dark: '#2b2548', light: '#ffffff' } })
      .then(setSvg)
      .catch(() => setSvg(''))
  }, [code])

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [onClose])

  const isCheckOut = store.status === 'IN_USE'

  // 페이지 섹션에는 등장 애니메이션의 transform이 남아 있어서, 그 안에 두면 position: fixed가
  // 화면이 아니라 섹션 기준으로 잡힌다. body에 바로 붙여서 항상 화면 가운데에 띄운다.
  return createPortal(
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div className="modal-card" role="dialog" aria-modal="true" aria-label="체크인 QR" onClick={(e) => e.stopPropagation()}>
        <span className={`badge badge-dot ${isCheckOut ? 'badge-in_use' : 'badge-ready'}`}>
          {isCheckOut ? '짐 찾을 때 보여주세요' : '짐 맡길 때 보여주세요'}
        </span>
        <h2>{store.placeName}</h2>
        <p className="modal-sub">
          {store.name} · 짐 {store.luggageCount}개
        </p>
        <div className="qr-frame" dangerouslySetInnerHTML={{ __html: svg }} />
        <p className="qr-code-text">{code.slice(0, 4)}-{code.slice(4)}</p>
        <p className="modal-sub">사장님이 QR을 스캔하면 {isCheckOut ? '보관이 끝나요' : '보관이 시작돼요'}.</p>
        <button type="button" className="btn btn-ghost btn-block" onClick={onClose}>
          닫기
        </button>
      </div>
    </div>,
    document.body,
  )
}
