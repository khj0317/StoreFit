export interface SignupRequest {
  email: string
  password: string
  name: string
  phoneNumber?: string
}

export interface SignupResponse {
  id: number
  email: string
  name: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  email: string
  name: string
}

export type StoreStatus = 'PENDING' | 'PICKED_UP' | 'IN_USE' | 'COMPLETED' | 'CANCELLED'

export type StoreCategory = 'LIGHT' | 'MEDIUM' | 'CLOTHES' | 'OTHER'

export interface ReviewSummary {
  id: number
  rating: number
  content: string | null
  createdAt: string
}

export interface StoreRecord {
  id: number
  memberId: number
  memberName: string
  name: string
  description: string | null
  address: string
  imageUrls: string[]
  category: StoreCategory
  luggageCount: number
  startDate: string
  endDate: string
  status: StoreStatus
  review: ReviewSummary | null
  createdAt: string
  updatedAt: string
}

export interface StoreMutationRequest {
  name: string
  description: string | null
  address: string
  imageUrls: string[]
  category: StoreCategory
  luggageCount: number
  startDate: string
  endDate: string
}

export interface StoreFormValues {
  name: string
  description: string
  address: string
  addressDetail: string
  imageUrls: string[]
  category: StoreCategory
  luggageCount: string
  startDate: string
  endDate: string
}

export interface ReviewCreateRequest {
  storeId: number
  rating: number
  content: string
}
