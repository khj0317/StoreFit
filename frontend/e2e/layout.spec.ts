import { expect, test, type Page } from '@playwright/test'

/**
 * 한글 줄바꿈 회귀 테스트: 좁은 화면에서 단어가 두 줄로 쪼개지는 곳(예: "사장님으로 둘러보 / 기")이 없어야 한다.
 * 모든 글자의 줄 위치를 재서, 한 단어가 서로 다른 줄에 걸쳐 있으면 실패한다.
 */
async function splitWords(page: Page): Promise<string[]> {
  return page.evaluate(() => {
    const found: string[] = []
    const walker = document.createTreeWalker(document.querySelector('#root')!, NodeFilter.SHOW_TEXT)
    let node: Node | null
    while ((node = walker.nextNode())) {
      const text = node.textContent ?? ''
      const el = node.parentElement
      if (!/[가-힣]/.test(text) || !el || el.offsetParent === null) continue
      for (const match of text.matchAll(/\S+/g)) {
        const range = document.createRange()
        range.setStart(node, match.index!)
        range.setEnd(node, match.index! + match[0].length)
        const lines = new Set([...range.getClientRects()].filter((r) => r.width > 0).map((r) => Math.round(r.top)))
        if (lines.size > 1) found.push(match[0])
      }
    }
    return found
  })
}

for (const width of [360, 440, 768]) {
  test(`${width}px 화면에서 한글 단어가 쪼개지지 않는다`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 })
    for (const path of ['/', '/login', '/signup']) {
      await page.goto(path)
      await page.waitForLoadState('load')
      await page.waitForTimeout(600)
      // 가운뎃점(·) 뒤 줄바꿈은 단어 사이라 허용한다
      const broken = (await splitWords(page)).filter((word) => !word.includes('·'))
      expect(broken, `${path}에서 쪼개진 단어`).toEqual([])
    }
  })
}
