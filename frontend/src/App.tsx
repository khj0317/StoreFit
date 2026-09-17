import { Route, Routes } from 'react-router-dom'
import './App.css'
import { Navbar } from './components/Navbar'
import { ProtectedRoute } from './components/ProtectedRoute'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyReservationsPage } from './pages/MyReservationsPage'
import { MyStoresPage } from './pages/MyStoresPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SignupPage } from './pages/SignupPage'
import { StoreDetailPage } from './pages/StoreDetailPage'
import { StoreFormPage } from './pages/StoreFormPage'

function App() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="app-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/stores/:storeId" element={<StoreDetailPage />} />
          <Route
            path="/my/stores"
            element={
              <ProtectedRoute>
                <MyStoresPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my/stores/new"
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
            path="/my/reservations"
            element={
              <ProtectedRoute>
                <MyReservationsPage />
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
