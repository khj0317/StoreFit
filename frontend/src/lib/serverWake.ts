/**
 * 배포 서버(Render 무료 플랜)는 15분 동안 요청이 없으면 잠들고, 다음 요청이 오면 깨어나는 데 1분 가까이 걸린다.
 * 사이트를 열자마자 헬스 체크로 서버를 깨우기 시작하고, 깨어날 때까지 API 요청을 잠깐 기다리게 해서
 * 사용자가 오류 화면 대신 "서버를 깨우는 중" 안내를 보게 한다.
 */

export type ServerState = 'checking' | 'waking' | 'ready' | 'down'

const HEALTH_URL = `${import.meta.env.VITE_API_BASE_URL ?? '/api'}/health`
/** 이 시간 안에 응답하면 깨어 있던 것으로 보고 안내를 띄우지 않는다 */
const SHOW_NOTICE_AFTER_MS = 2500
const ATTEMPT_TIMEOUT_MS = 20_000
const RETRY_DELAY_MS = 3000
/** 이만큼 기다려도 안 깨어나면 포기하고 요청을 그냥 보낸다 */
const GIVE_UP_AFTER_MS = 150_000

let state: ServerState = 'checking'
const listeners = new Set<() => void>()
let started = false
let resolveReady: () => void = () => {}
const settled = new Promise<void>((resolve) => {
  resolveReady = resolve
})

function setState(next: ServerState) {
  if (state === next) return
  state = next
  if (next === 'ready' || next === 'down') resolveReady()
  listeners.forEach((listener) => listener())
}

export function getServerState(): ServerState {
  return state
}

export function subscribeServerState(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

async function ping(): Promise<boolean> {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), ATTEMPT_TIMEOUT_MS)
  try {
    const response = await fetch(HEALTH_URL, { cache: 'no-store', signal: controller.signal })
    return response.ok
  } catch {
    return false
  } finally {
    window.clearTimeout(timer)
  }
}

const sleep = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

/** 앱이 시작될 때 한 번 부른다. 서버가 깨어날 때까지 헬스 체크를 반복한다. */
export function wakeServer() {
  if (started) return
  started = true
  const startedAt = Date.now()
  const noticeTimer = window.setTimeout(() => {
    if (state === 'checking') setState('waking')
  }, SHOW_NOTICE_AFTER_MS)

  void (async () => {
    while (Date.now() - startedAt < GIVE_UP_AFTER_MS) {
      if (await ping()) {
        window.clearTimeout(noticeTimer)
        setState('ready')
        return
      }
      if (state === 'checking') setState('waking')
      await sleep(RETRY_DELAY_MS)
    }
    setState('down')
  })()
}

/** 다시 시도 버튼: 처음부터 다시 깨운다 */
export function retryWakeServer() {
  started = false
  setState('checking')
  wakeServer()
}

/** API 요청 전에 부른다. 서버가 깨어나는 중이면 깨어날 때까지(최대 GIVE_UP_AFTER_MS) 기다린다. */
export function whenServerSettled(): Promise<void> {
  if (state === 'ready' || state === 'down' || !started) return Promise.resolve()
  return settled
}
