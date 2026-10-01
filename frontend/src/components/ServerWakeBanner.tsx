import { useSyncExternalStore } from 'react'
import { getServerState, retryWakeServer, subscribeServerState } from '../lib/serverWake'
import { Mascot } from './Logo'

/** 잠든 배포 서버를 깨우는 동안 화면 위에 띄우는 안내 */
export function ServerWakeBanner() {
  const state = useSyncExternalStore(subscribeServerState, getServerState)

  if (state === 'waking') {
    return (
      <div className="server-wake" role="status" aria-live="polite">
        <Mascot size={40} mood="sleepy" className="server-wake-mascot" />
        <div className="server-wake-body">
          <strong>잠든 서버를 깨우는 중이에요</strong>
          <span>오랜만의 접속이라 최대 1분 정도 걸려요. 깨어나면 자동으로 이어서 보여드릴게요.</span>
        </div>
        <span className="server-wake-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </span>
      </div>
    )
  }

  if (state === 'down') {
    return (
      <div className="server-wake server-wake-down" role="alert">
        <Mascot size={40} mood="sad" />
        <div className="server-wake-body">
          <strong>서버가 아직 응답하지 않아요</strong>
          <span>잠시 후 다시 시도해주세요.</span>
        </div>
        <button type="button" className="btn btn-sm btn-secondary" onClick={retryWakeServer}>
          다시 시도
        </button>
      </div>
    )
  }

  return null
}
