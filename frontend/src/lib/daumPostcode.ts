export interface DaumPostcodeData {
  address: string
  roadAddress: string
  jibunAddress: string
  zonecode: string
}

interface DaumPostcodeInstance {
  open: () => void
}

declare global {
  interface Window {
    daum?: {
      Postcode: new (options: { oncomplete: (data: DaumPostcodeData) => void }) => DaumPostcodeInstance
    }
  }
}

const SCRIPT_SRC = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

let scriptPromise: Promise<void> | null = null

function loadScript(): Promise<void> {
  if (window.daum?.Postcode) {
    return Promise.resolve()
  }
  if (scriptPromise) {
    return scriptPromise
  }

  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SCRIPT_SRC
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('주소 검색 스크립트를 불러오지 못했습니다.'))
    document.head.appendChild(script)
  })
  return scriptPromise
}

export async function openAddressSearch(onComplete: (data: DaumPostcodeData) => void): Promise<void> {
  await loadScript()
  new window.daum!.Postcode({ oncomplete: onComplete }).open()
}
