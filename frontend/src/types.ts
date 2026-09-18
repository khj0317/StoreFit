export interface SignupRequest {
  username: string
  password: string
  name: string
  email?: string
  phoneNumber?: string
}

export interface SignupResponse {
  id: number
  username: string
  name: string
  email: string | null
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  username: string
  name: string
}

export interface FindUsernameRequest {
  email: string
  name: string
}

export interface FindUsernameResponse {
  username: string
}

export interface ResetPasswordRequest {
  username: string
  email: string
  name: string
  newPassword: string
}

export type StoreStatus = 'PENDING' | 'PICKED_UP' | 'IN_USE' | 'COMPLETED'

export type StoreCategory = 'LIGHT' | 'MEDIUM' | 'CLOTHES' | 'OTHER'

export type PaymentStatus = 'READY' | 'DONE' | 'FAILED' | 'CANCELED'

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
  totalPrice: number
  paymentStatus: PaymentStatus | null
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

export interface PaymentReadyResponse {
  orderId: string
  amount: number
  orderName: string
}

export interface PaymentConfirmRequest {
  paymentKey: string
  orderId: string
  amount: number
}

export interface PaymentResponse {
  id: number
  storeId: number
  orderId: string
  amount: number
  status: PaymentStatus
  method: string | null
  approvedAt: string | null
}
