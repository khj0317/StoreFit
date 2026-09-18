import type { StoreCategory } from '../types'

export interface StoreCategoryOption {
  value: StoreCategory
  label: string
  description: string
  dailyRate: number
}

export const STORE_CATEGORIES: StoreCategoryOption[] = [
  { value: 'LIGHT', label: '5~10kg', description: '캐리어, 작은 박스 등 가벼운 짐', dailyRate: 3_000 },
  { value: 'MEDIUM', label: '10~30kg', description: '큰 박스, 다수의 짐', dailyRate: 5_000 },
  { value: 'CLOTHES', label: '옷 보관', description: '의류, 이불 등 섬유 제품', dailyRate: 4_000 },
  { value: 'OTHER', label: '기타', description: '위 항목에 해당하지 않는 짐', dailyRate: 4_000 },
]

export const STORE_CATEGORY_LABELS: Record<StoreCategory, string> = STORE_CATEGORIES.reduce(
  (acc, category) => ({ ...acc, [category.value]: category.label }),
  {} as Record<StoreCategory, string>,
)

export const STORE_CATEGORY_DAILY_RATES: Record<StoreCategory, number> = STORE_CATEGORIES.reduce(
  (acc, category) => ({ ...acc, [category.value]: category.dailyRate }),
  {} as Record<StoreCategory, number>,
)
