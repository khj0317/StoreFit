export type MemberRole = 'USER' | 'OWNER' | 'ADMIN'

export interface SignupRequest {
  username: string
  password: string
  name: string
  phoneNumber: string
  verificationToken: string
  role?: MemberRole
}

export type VerificationPurpose = 'SIGNUP' | 'FIND_USERNAME' | 'RESET_PASSWORD' | 'CHANGE_PHONE'

export interface SendCodeResponse {
  expiresInSeconds: number
  /** 문자가 실제로 가지 않는 개발 환경에서만 온다 */
  devCode: string | null
}

export interface SignupResponse {
  id: number
  username: string
  name: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  username: string
  name: string
  role: MemberRole
}

export interface FindUsernameRequest {
  phoneNumber: string
  verificationToken: string
}

export interface FindUsernameResponse {
  username: string
}

export interface ResetPasswordRequest {
  username: string
  phoneNumber: string
  verificationToken: string
  newPassword: string
}

export type StoreStatus = 'PENDING' | 'IN_USE' | 'COMPLETED' | 'CANCELED' | 'EXPIRED' | 'NO_SHOW'

export type StoreCategory = 'LIGHT' | 'MEDIUM' | 'CLOTHES' | 'OTHER'

export type PaymentStatus = 'READY' | 'DONE' | 'FAILED' | 'CANCELED' | 'PARTIAL_CANCELED'

export interface StoreRecord {
  id: number
  memberId: number
  memberName: string
  placeId: number
  placeName: string
  placeAddress: string
  name: string
  description: string | null
  imageUrls: string[]
  category: StoreCategory
  luggageCount: number
  startDate: string
  endDate: string
  status: StoreStatus
  totalPrice: number
  paymentStatus: PaymentStatus | null
  refundedAmount: number
  checkInCode: string | null
  checkedInAt: string | null
  checkedOutAt: string | null
  /** 이 시각까지 결제하지 않으면 자동 취소된다 (결제했으면 null) */
  paymentDeadline: string | null
  overdueDays: number
  overdueFee: number
  createdAt: string
  updatedAt: string
}

export interface StoreMutationRequest {
  placeId: number
  name: string
  description: string | null
  imageUrls: string[]
  category: StoreCategory
  luggageCount: number
  startDate: string
  endDate: string
}

export interface StoreFormValues {
  placeId: number | null
  name: string
  description: string
  imageUrls: string[]
  category: StoreCategory
  luggageCount: string
  startDate: string
  endDate: string
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
  storeName: string
  orderId: string
  amount: number
  status: PaymentStatus
  method: string | null
  approvedAt: string | null
  canceledAmount: number
  canceledAt: string | null
  createdAt: string
}

export interface MemberProfile {
  id: number
  username: string
  name: string
  phoneNumber: string | null
  role: MemberRole
}

export interface UpdateProfileRequest {
  name: string
}

export interface ChangePhoneRequest {
  phoneNumber: string
  verificationToken: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface DeleteAccountRequest {
  password: string
}

export interface RefundPreview {
  paidAmount: number
  refundAmount: number
  refundRate: number
  policy: string
}

/** 보관소. remaining은 기간을 지정해 조회했을 때만 있다 */
export interface Place {
  id: number
  name: string
  address: string
  description: string | null
  capacity: number
  remaining: number | null
  latitude: number | null
  longitude: number | null
}

/** 운영자는 정식 지점의 수용량과 소개만 정한다 (이름·주소는 본사 등록 정보) */
export interface PlaceRequest {
  capacity: number
  description: string | null
}

export type OwnerNextAction = 'CHECK_IN' | 'CHECK_OUT' | 'NONE'

export interface OwnerReservation {
  id: number
  checkInCode: string | null
  placeId: number
  placeName: string
  customerName: string
  customerPhone: string | null
  name: string
  category: StoreCategory
  luggageCount: number
  startDate: string
  endDate: string
  status: StoreStatus
  paid: boolean
  totalPrice: number
  checkedInAt: string | null
  checkedOutAt: string | null
  overdueDays: number
  overdueFee: number
  nextAction: OwnerNextAction
}

export interface OwnerDashboard {
  today: string
  arrivals: OwnerReservation[]
  departures: OwnerReservation[]
  storedLuggageCount: number
  monthRevenue: number
  places: { id: number; name: string; address: string; capacity: number; occupiedToday: number }[]
}

export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface BranchApplication {
  id: number
  placeId: number
  placeName: string
  placeAddress: string
  applicantId: number
  applicantName: string
  applicantPhone: string | null
  capacity: number
  description: string | null
  message: string | null
  status: ApplicationStatus
  rejectReason: string | null
  createdAt: string
  decidedAt: string | null
}

export interface BranchApplyRequest {
  capacity: number
  description: string | null
  message: string | null
}

export interface AdminBranch {
  id: number
  code: string | null
  name: string
  address: string
  description: string | null
  capacity: number
  latitude: number | null
  longitude: number | null
  operating: boolean
  ownerName: string | null
  ownerPhone: string | null
  pendingApplications: number
}

export interface BranchRequest {
  code: string
  name: string
  address: string
  description: string | null
  capacity: number
  latitude: number | null
  longitude: number | null
}

export interface AdminNotification {
  id: number
  createdAt: string
  phoneNumber: string
  type: string
  channel: string | null
  content: string
  status: 'PENDING' | 'SENT' | 'FAILED'
  error: string | null
}
