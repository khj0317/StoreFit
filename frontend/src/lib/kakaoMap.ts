/**
 * 카카오 지도 JavaScript SDK. VITE_KAKAO_JS_KEY가 없으면 지도를 쓰지 않고(목록만 보여주고) 넘어간다.
 * 키는 카카오 개발자 콘솔 → 내 애플리케이션 → 앱 키 → JavaScript 키, 그리고
 * 플랫폼 → Web에 사이트 도메인(http://localhost:5173, 배포 주소)을 등록해야 동작한다.
 */

/* eslint-disable @typescript-eslint/no-explicit-any */
declare global {
  interface Window {
    kakao?: any
  }
}

const APP_KEY = import.meta.env.VITE_KAKAO_JS_KEY as string | undefined

let loading: Promise<any> | null = null

export function isKakaoMapEnabled(): boolean {
  return Boolean(APP_KEY)
}

export function loadKakaoMaps(): Promise<any> {
  if (!APP_KEY) return Promise.reject(new Error('카카오 지도 키가 없습니다.'))
  if (window.kakao?.maps?.LatLng) return Promise.resolve(window.kakao)
  loading ??= new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${APP_KEY}&autoload=false&libraries=services`
    script.async = true
    script.onload = () => window.kakao.maps.load(() => resolve(window.kakao))
    script.onerror = () => {
      loading = null
      reject(new Error('카카오 지도를 불러오지 못했습니다.'))
    }
    document.head.appendChild(script)
  })
  return loading
}

/** 주소 → 좌표. 지도 키가 없거나 찾지 못하면 null */
export async function geocodeAddress(address: string): Promise<{ latitude: number; longitude: number } | null> {
  if (!APP_KEY || !address) return null
  const kakao = await loadKakaoMaps()
  return new Promise((resolve) => {
    new kakao.maps.services.Geocoder().addressSearch(address, (result: any[], status: string) => {
      if (status === kakao.maps.services.Status.OK && result[0]) {
        resolve({ latitude: Number(result[0].y), longitude: Number(result[0].x) })
      } else {
        resolve(null)
      }
    })
  })
}
