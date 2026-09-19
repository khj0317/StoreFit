import { Route, Routes } from 'react-router-dom'
import './App.css'
import { Navbar } from './components/Navbar'
import { ProtectedRoute } from './components/ProtectedRoute'
import { FindUsernamePage } from './pages/FindUsernamePage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { MyStoresPage } from './pages/MyStoresPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PaymentFailPage } from './pages/PaymentFailPage'
import { PaymentHistoryPage } from './pages/PaymentHistoryPage'
import { PaymentSuccessPage } from './pages/PaymentSuccessPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { SignupPage } from './pages/SignupPage'
import { StoreCategoryPage } from './pages/StoreCategoryPage'
import { StoreFormPage } from './pages/StoreFormPage'
import { StoreHistoryPage } from './pages/StoreHistoryPage'
import { StorePaymentPage } from './pages/StorePaymentPage'

function App() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="app-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/find-username" element={<FindUsernamePage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route
            path="/my/profile"
            element={
              <ProtectedRoute>
                <MyPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/payments"
            element={
              <ProtectedRoute>
                <PaymentHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores"
            element={
              <ProtectedRoute>
                <MyStoresPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/history"
            element={
              <ProtectedRoute>
                <StoreHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/new"
            element={
              <ProtectedRoute>
                <StoreCategoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/new/:category"
            element={
              <ProtectedRoute>
                <StoreFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/edit"
            element={
              <ProtectedRoute>
                <StoreFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/pay"
            element={
              <ProtectedRoute>
                <StorePaymentPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/pay/success"
            element={
              <ProtectedRoute>
                <PaymentSuccessPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/pay/fail"
            element={
              <ProtectedRoute>
                <PaymentFailPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
