// Toss Payments' own published sandbox test client key (safe to share, no real merchant attached).
// Replace with your real client key from https://developers.tosspayments.com before going live.
const CLIENT_KEY = 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq'

const SCRIPT_SRC = 'https://js.tosspayments.com/v1/payment'

export interface RequestPaymentParams {
  amount: number
  orderId: string
  orderName: string
  customerName?: string
  successUrl: string
  failUrl: string
}

interface TossPaymentsInstance {
  requestPayment: (method: string, params: RequestPaymentParams) => Promise<void>
}

declare global {
  interface Window {
    TossPayments?: (clientKey: string) => TossPaymentsInstance
  }
}

let scriptPromise: Promise<void> | null = null

function loadScript(): Promise<void> {
  if (window.TossPayments) {
    return Promise.resolve()
  }
  if (scriptPromise) {
    return scriptPromise
  }

  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SCRIPT_SRC
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('결제 스크립트를 불러오지 못했습니다.'))
    document.head.appendChild(script)
  })
  return scriptPromise
}

export async function requestTossPayment(params: RequestPaymentParams): Promise<void> {
  await loadScript()
  const tossPayments = window.TossPayments!(CLIENT_KEY)
  await tossPayments.requestPayment('카드', params)
}
