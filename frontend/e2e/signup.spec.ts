import { expect, test } from '@playwright/test'

/** 휴대폰 인증 가입: 개발 설정에서는 문자 대신 인증번호를 화면에 보여준다 */
test('휴대폰 인증을 마쳐야 가입 버튼이 열리고, 가입하면 바로 로그인된다', async ({ page }) => {
  const suffix = Date.now().toString().slice(-8)
  const phone = `010${suffix}`
  const username = `e2e${suffix}`

  await page.goto('/signup')
  const submit = page.locator('form button[type=submit]')
  await expect(submit).toBeDisabled()

  await page.getByPlaceholder('010-0000-0000').fill(phone)
  await page.getByRole('button', { name: '인증번호 받기' }).click()
  const devCode = page.locator('.dev-code')
  await expect(devCode).toBeVisible()
  const code = (await devCode.innerText()).match(/(\d{6})\s*$/)?.[1]
  expect(code).toBeTruthy()

  await page.getByLabel('인증번호').fill(code!)
  await page.getByRole('button', { name: '확인' }).click()
  await expect(page.getByText('인증 완료')).toBeVisible()

  await page.getByPlaceholder('영문, 숫자, 밑줄(_) 4~20자').fill(username)
  await page.getByPlaceholder('8자 이상').fill('e2e-password-1')
  await page.locator('label', { hasText: '이름' }).locator('input').fill('이투이')
  await expect(submit).toBeEnabled()
  await submit.click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByText('이투이', { exact: false }).first()).toBeVisible()
})

test('틀린 인증번호로는 인증되지 않는다', async ({ page }) => {
  const phone = `010${(Date.now() + 1).toString().slice(-8)}`
  await page.goto('/signup')
  await page.getByPlaceholder('010-0000-0000').fill(phone)
  await page.getByRole('button', { name: '인증번호 받기' }).click()
  const devCode = (await page.locator('.dev-code').innerText()).match(/(\d{6})\s*$/)![1]
  const wrong = devCode === '000000' ? '111111' : '000000'

  await page.getByLabel('인증번호').fill(wrong)
  await page.getByRole('button', { name: '확인' }).click()
  await expect(page.locator('.error-text')).toContainText('일치하지 않습니다')
  await expect(page.locator('form button[type=submit]')).toBeDisabled()
})
