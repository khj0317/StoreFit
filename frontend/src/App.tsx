import { Route, Routes } from 'react-router-dom'
import './App.css'
import { DemoBanner } from './components/DemoEntry'
import { Navbar } from './components/Navbar'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ServerWakeBanner } from './components/ServerWakeBanner'
import { FindUsernamePage } from './pages/FindUsernamePage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { MyStoresPage } from './pages/MyStoresPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { AdminPage } from './pages/admin/AdminPage'
import { OwnerDashboardPage } from './pages/owner/OwnerDashboardPage'
import { OwnerPlacesPage } from './pages/owner/OwnerPlacesPage'
import { OwnerScanPage } from './pages/owner/OwnerScanPage'
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
        <ServerWakeBanner />
        <DemoBanner />
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
              <ProtectedRoute role="USER">
                <PaymentHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores"
            element={
              <ProtectedRoute role="USER">
                <MyStoresPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/history"
            element={
              <ProtectedRoute role="USER">
                <StoreHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/new"
            element={
              <ProtectedRoute role="USER">
                <StoreCategoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/new/:category"
            element={
              <ProtectedRoute role="USER">
                <StoreFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/edit"
            element={
              <ProtectedRoute role="USER">
                <StoreFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/pay"
            element={
              <ProtectedRoute role="USER">
                <StorePaymentPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/pay/success"
            element={
              <ProtectedRoute role="USER">
                <PaymentSuccessPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/:storeId/pay/fail"
            element={
              <ProtectedRoute role="USER">
                <PaymentFailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner"
            element={
              <ProtectedRoute role="OWNER">
                <OwnerDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/places"
            element={
              <ProtectedRoute role="OWNER">
                <OwnerPlacesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/scan"
            element={
              <ProtectedRoute role="OWNER">
                <OwnerScanPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute role="ADMIN">
                <AdminPage />
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
