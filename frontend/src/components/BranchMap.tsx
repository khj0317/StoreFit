import { useEffect, useMemo, useRef, useState } from 'react'
import { isKakaoMapEnabled, loadKakaoMaps } from '../lib/kakaoMap'
import type { Place } from '../types'

/**
 * 지점들을 카카오 지도에 표시한다. 마커를 누르면 그 지점을 고른다.
 * 지도 키가 없거나 좌표가 있는 지점이 하나도 없으면 아무것도 그리지 않는다 (목록으로 고르면 된다).
 */
export function BranchMap({
  places,
  selectedId,
  disabledIds,
  onSelect,
}: {
  places: Place[]
  selectedId: number | null
  disabledIds?: Set<number>
  onSelect: (placeId: number) => void
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [failed, setFailed] = useState(false)
  const onSelectRef = useRef(onSelect)
  onSelectRef.current = onSelect

  const located = useMemo(
    () => places.filter((place) => place.latitude !== null && place.longitude !== null),
    [places],
  )
  // 부모가 렌더링할 때마다 새 배열·Set을 넘겨도 지도를 다시 만들지 않도록 내용으로 비교한다
  const markerKey = located.map((place) => `${place.id}:${place.latitude}:${place.longitude}`).join('|')
  const disabledKey = [...(disabledIds ?? [])].sort().join(',')

  useEffect(() => {
    if (!isKakaoMapEnabled() || located.length === 0 || !containerRef.current) return
    let cancelled = false
    const overlays: { setMap: (map: unknown) => void }[] = []

    loadKakaoMaps()
      .then((kakao) => {
        if (cancelled || !containerRef.current) return
        const map = new kakao.maps.Map(containerRef.current, {
          center: new kakao.maps.LatLng(located[0].latitude, located[0].longitude),
          level: 7,
        })
        const bounds = new kakao.maps.LatLngBounds()

        located.forEach((place) => {
          const position = new kakao.maps.LatLng(place.latitude, place.longitude)
          bounds.extend(position)
          const disabled = disabledKey.split(',').includes(String(place.id))
          const pin = document.createElement('button')
          pin.type = 'button'
          pin.className = `map-pin${place.id === selectedId ? ' map-pin-selected' : ''}${disabled ? ' map-pin-disabled' : ''}`
          pin.textContent = place.name.replace('스토어핏 ', '')
          pin.disabled = disabled
          pin.addEventListener('click', () => onSelectRef.current(place.id))
          const overlay = new kakao.maps.CustomOverlay({ position, content: pin, yAnchor: 1.2 })
          overlay.setMap(map)
          overlays.push(overlay)
        })

        if (located.length > 1) map.setBounds(bounds)
      })
      .catch(() => setFailed(true))

    return () => {
      cancelled = true
      overlays.forEach((overlay) => overlay.setMap(null))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [markerKey, selectedId, disabledKey])

  if (!isKakaoMapEnabled() || located.length === 0 || failed) return null

  return <div ref={containerRef} className="branch-map" role="application" aria-label="지점 지도" />
}
