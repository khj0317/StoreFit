import { useNavigate } from 'react-router-dom'
import { STORE_CATEGORIES } from '../constants/storeCategories'

export function StoreCategoryPage() {
  const navigate = useNavigate()

  return (
    <section>
      <h1>어떤 짐을 보관하시나요?</h1>
      <p className="page-subtitle">종류를 선택하면 이어서 자세한 정보를 입력합니다.</p>

      <div className="category-grid">
        {STORE_CATEGORIES.map((category) => (
          <button
            key={category.value}
            type="button"
            className="category-card"
            onClick={() => navigate(`/my/stores/new/${category.value}`)}
          >
            <strong>{category.label}</strong>
            <span>{category.description}</span>
            <span className="category-card-price">{category.dailyRate.toLocaleString()}원 / 일</span>
          </button>
        ))}
      </div>
    </section>
  )
}
