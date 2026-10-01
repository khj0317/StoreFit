import { Link, useParams, useSearchParams } from 'react-router-dom'
import { AuthCard } from '../components/Feedback'

export function PaymentFailPage() {
  const { storeId } = useParams<{ storeId: string }>()
  const [searchParams] = useSearchParams()
  const message = searchParams.get('message') ?? '결제가 취소되었거나 실패했습니다.'

  return (
    <AuthCard title="결제를 완료하지 못했어요" mood="sad">
      <p className="error-text">{message}</p>
      <div className="result-actions">
        <Link to={`/my/stores/${storeId}/pay`} className="btn btn-primary btn-lg">
          다시 시도하기
        </Link>
        <Link to="/my/stores" className="btn btn-ghost">
          보관 현황으로
        </Link>
      </div>
    </AuthCard>
  )
}
