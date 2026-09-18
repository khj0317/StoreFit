import type { StoreCategory } from '../types'

export interface StoreCategoryOption {
  value: StoreCategory
  label: string
  description: string
}

export const STORE_CATEGORIES: StoreCategoryOption[] = [
  { value: 'LIGHT', label: '5~10kg', description: '캐리어, 작은 박스 등 가벼운 짐' },
  { value: 'MEDIUM', label: '10~30kg', description: '큰 박스, 다수의 짐' },
  { value: 'CLOTHES', label: '옷 보관', description: '의류, 이불 등 섬유 제품' },
  { value: 'OTHER', label: '기타', description: '위 항목에 해당하지 않는 짐' },
]

export const STORE_CATEGORY_LABELS: Record<StoreCategory, string> = STORE_CATEGORIES.reduce(
  (acc, category) => ({ ...acc, [category.value]: category.label }),
  {} as Record<StoreCategory, string>,
)
