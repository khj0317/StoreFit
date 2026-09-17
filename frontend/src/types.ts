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

export interface StoreSummary {
  id: number
  name: string
  address: string
  thumbnailUrl: string | null
}

export interface StoreDetail {
  id: number
  hostId: number
  hostName: string
  name: string
  description: string | null
  address: string
  imageUrls: string[]
  createdAt: string
  updatedAt: string
}

export interface StoreMutationRequest {
  name: string
  description: string | null
  address: string
  imageUrls: string[]
}

export interface StoreFormValues {
  name: string
  description: string
  address: string
  imageUrls: string[]
}

export type ReservationStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'

export interface Reservation {
  id: number
  storeId: number
  storeName: string
  memberId: number
  memberName: string
  luggageCount: number
  startTime: string
  endTime: string
  status: ReservationStatus
  createdAt: string
}

export interface ReservationCreateRequest {
  storeId: number
  luggageCount: number
  startTime: string
  endTime: string
}

export interface Review {
  id: number
  reservationId: number
  storeId: number
  memberId: number
  memberName: string
  rating: number
  content: string | null
  createdAt: string
}

export interface ReviewCreateRequest {
  reservationId: number
  rating: number
  content: string
}
