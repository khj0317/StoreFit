import { defineConfig, devices } from '@playwright/test'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * 화면 E2E 테스트: 실제 백엔드(로컬 Postgres)와 Vite 개발 서버를 띄우고 브라우저로 누른다.
 *   npm run test:e2e
 * 로컬에서는 이미 켜 둔 서버를 그대로 쓰고, CI에서는 두 서버를 새로 띄운다.
 * 백엔드는 개발 설정이라 문자는 보내지 않고, 인증번호를 화면에 보여준다(개발 모드).
 */
const isWindows = process.platform === 'win32'
const backendDir = fileURLToPath(new URL('../backend', import.meta.url))

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // 체험 데이터를 함께 쓰므로 순서대로 돌린다
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  timeout: 60_000,
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'ko-KR',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: isWindows ? `"${join(backendDir, 'gradlew.bat')}" bootRun --console=plain` : './gradlew bootRun --console=plain',
      cwd: backendDir,
      url: 'http://localhost:8080/api/health',
      timeout: 240_000,
      reuseExistingServer: !process.env.CI,
      stdout: 'ignore',
    },
    {
      command: 'npm run dev -- --port 5173 --strictPort',
      url: 'http://localhost:5173',
      timeout: 60_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
})
