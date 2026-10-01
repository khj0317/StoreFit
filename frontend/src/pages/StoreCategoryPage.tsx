import { useNavigate } from 'react-router-dom'
import { CategoryIcon } from '../components/CategoryIcon'
import { ArrowRightIcon } from '../components/Icons'
import { STORE_CATEGORIES } from '../constants/storeCategories'

export function StoreCategoryPage() {
  const navigate = useNavigate()

  return (
    <section>
      <div className="step-indicator" aria-label="2단계 중 1단계">
        <span className="active" />
        <span />
      </div>
      <h1>어떤 짐을 보관하시나요?</h1>
      <p className="page-subtitle">종류를 고르면 이어서 기간과 장소를 입력해요.</p>

      <div className="category-grid">
        {STORE_CATEGORIES.map((category) => (
          <button
            key={category.value}
            type="button"
            className="category-card"
            data-tone={category.value}
            onClick={() => navigate(`/my/stores/new/${category.value}`)}
          >
            <span className="category-card-arrow">
              <ArrowRightIcon size={16} />
            </span>
            <CategoryIcon category={category.value} />
            <strong>{category.label}</strong>
            <span className="category-card-desc">{category.description}</span>
            <span className="category-card-price">
              {category.dailyRate.toLocaleString()}원 <small>/ 일</small>
            </span>
          </button>
        ))}
      </div>
    </section>
  )
}
