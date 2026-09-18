import { Route, Routes } from 'react-router-dom'
import './App.css'
import { Navbar } from './components/Navbar'
import { ProtectedRoute } from './components/ProtectedRoute'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyStoresPage } from './pages/MyStoresPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SignupPage } from './pages/SignupPage'
import { StoreCategoryPage } from './pages/StoreCategoryPage'
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
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
