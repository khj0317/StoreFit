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

export type StoreStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'

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
  luggageCount: number
  startTime: string
  endTime: string
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
  luggageCount: number
  startTime: string
  endTime: string
}

export interface StoreFormValues {
  name: string
  description: string
  address: string
  addressDetail: string
  imageUrls: string[]
  luggageCount: string
  startTime: string
  endTime: string
}

export interface ReviewCreateRequest {
  storeId: number
  rating: number
  content: string
}
