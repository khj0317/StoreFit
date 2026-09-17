import { useEffect, useState } from 'react'
import { api } from './lib/api'
import './App.css'

function App() {
  const [status, setStatus] = useState<'checking' | 'ok' | 'fail'>('checking')

  useEffect(() => {
    api
      .get('/health')
      .then(() => setStatus('ok'))
      .catch(() => setStatus('fail'))
  }, [])

  return (
    <main style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>짐보관 플랫폼</h1>
      <p>
        백엔드 연결 상태:{' '}
        {status === 'checking' && '확인 중...'}
        {status === 'ok' && '✅ 연결 성공'}
        {status === 'fail' && '❌ 연결 실패 (백엔드가 실행 중인지 확인하세요)'}
      </p>
    </main>
  )
}

export default App
