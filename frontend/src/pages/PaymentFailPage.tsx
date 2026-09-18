import { Link, useParams, useSearchParams } from 'react-router-dom'

export function PaymentFailPage() {
  const { storeId } = useParams<{ storeId: string }>()
  const [searchParams] = useSearchParams()
  const message = searchParams.get('message') ?? '결제가 취소되었거나 실패했습니다.'

  return (
    <div className="centered-layout">
      <div className="centered-card">
        <h1>결제 실패</h1>
        <p className="error-text">{message}</p>
        <Link to={`/my/stores/${storeId}/pay`} className="btn btn-primary">
          다시 시도
        </Link>
      </div>
    </div>
  )
}
