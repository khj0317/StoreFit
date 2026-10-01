import { Link } from 'react-router-dom'
import { AuthCard } from '../components/Feedback'

export function NotFoundPage() {
  return (
    <AuthCard title="길을 잃었나 봐요" subtitle="찾으시는 페이지가 없어요." mood="sad">
      <div className="result-actions">
        <Link to="/" className="btn btn-primary btn-lg">
          홈으로 돌아가기
        </Link>
      </div>
    </AuthCard>
  )
}
