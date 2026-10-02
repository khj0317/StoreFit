import { expect, test, type Page } from '@playwright/test'

/**
 * 가입 없이 둘러보기: 체험 이용자가 받은 체크인 QR을 체험 사장님이 스캔(코드 입력)해서 짐을 받는다.
 * 테스트마다 체험 데이터를 처음 상태로 되돌리고 시작한다 (개발 설정에서만 열린 API).
 */
test.beforeEach(async ({ request }) => {
  const reset = await request.post('/api/auth/demo-reset')
  expect(reset.status()).toBe(204)
})

async function enterDemo(page: Page, who: '이용자로 둘러보기' | '사장님으로 둘러보기') {
  await page.goto('/')
  await page.getByRole('link', { name: '가입 없이 둘러보기' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await page.getByRole('button', { name: new RegExp(who) }).click()
}

test('체험 이용자는 보관 중인 짐과 오늘 맡길 짐의 체크인 QR을 본다', async ({ page }) => {
  await enterDemo(page, '이용자로 둘러보기')

  await expect(page).toHaveURL(/\/my\/stores$/)
  await expect(page.locator('.demo-banner')).toContainText('체험 이용자로 둘러보는 중이에요')
  await expect(page.getByRole('heading', { name: '기숙사 이불 세트' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '여름옷 정리 상자' })).toBeVisible()

  await page.getByRole('button', { name: /체크인 QR/ }).first().click()
  const qr = page.getByRole('dialog', { name: '체크인 QR' })
  await expect(qr).toBeVisible()
  await expect(qr.locator('.qr-code-text')).toHaveText(/^[A-Z0-9]{4}-[A-Z0-9]{4}$/)
  await qr.getByRole('button', { name: '닫기' }).click()
  await expect(qr).toBeHidden()
})

test('체험 사장님이 이용자의 체크인 코드로 짐을 받으면 이용자 화면에 보관 중으로 바뀐다', async ({ page }) => {
  // 이용자 화면에서 오늘 맡길 짐의 코드를 읽는다
  await enterDemo(page, '이용자로 둘러보기')
  await page.getByRole('button', { name: /체크인 QR/ }).first().click()
  const code = (await page.locator('.qr-code-text').innerText()).replace('-', '')
  await page.keyboard.press('Escape')
  await page.getByRole('button', { name: '로그아웃' }).click()

  // 사장님: 대시보드 → QR 체크인 → 코드 입력 → 체크인
  await enterDemo(page, '사장님으로 둘러보기')
  await expect(page).toHaveURL(/\/owner$/)
  await expect(page.getByRole('heading', { name: '운영 대시보드' })).toBeVisible()
  await page.getByRole('link', { name: 'QR 체크인' }).first().click()
  await page.getByPlaceholder('예) ABCD-2345').fill(code)
  await page.getByRole('button', { name: '조회' }).click()
  await expect(page.getByText('기숙사 이불 세트', { exact: false })).toBeVisible()
  await page.getByRole('button', { name: /받기 \(체크인\)/ }).click()
  await expect(page.locator('.success-text')).toBeVisible()
  await page.getByRole('button', { name: '로그아웃' }).click()

  // 다시 이용자: 같은 짐이 이제 "찾을 때 QR"을 보여준다 (보관 중)
  await enterDemo(page, '이용자로 둘러보기')
  const card = page.locator('.store-card', { has: page.getByRole('heading', { name: '기숙사 이불 세트' }) })
  await expect(card.getByRole('button', { name: /찾을 때 QR/ })).toBeVisible()
})
