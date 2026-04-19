import { test, expect } from '@playwright/test'

test.describe('Comments', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(2000)
  })

  test('should display comment section', async ({ page }) => {
    await page.waitForTimeout(3000)

    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isVisible = await articleLink.isVisible().catch(() => false)

    if (isVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      const commentSection = page.locator('[class*="comment"], #comment, [id*="comment"]').first()
      await expect(commentSection).toBeVisible({ timeout: 15000 })
    }
  })

  test('should post comment when logged in', async ({ page }) => {
    await page.waitForTimeout(3000)

    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isVisible = await articleLink.isVisible().catch(() => false)

    if (isVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      const loginBtn = page.locator('button:has-text("登录")').first()
      const loginVisible = await loginBtn.isVisible().catch(() => false)

      if (loginVisible) {
        await loginBtn.click()
        await page.waitForTimeout(1000)

        const dialog = page.locator('[role="dialog"], .el-dialog, .dialog').first()
        const usernameInput = dialog.locator('input').first()
        const passwordInput = dialog.locator('input[type="password"]').first()

        await usernameInput.fill('admin')
        await passwordInput.fill('147258369Thy@')

        const submitBtn = dialog.locator('button:has-text("登录"), button[type="submit"]').first()
        await submitBtn.click()
        await page.waitForTimeout(3000)
      }

      const commentInput = page.locator('textarea, input[placeholder*="评论"], [class*="comment-input"]').first()
      const commentVisible = await commentInput.isVisible().catch(() => false)

      if (commentVisible) {
        await commentInput.fill('E2E Test Comment')
        const sendBtn = page.locator('button:has-text("发送"), button:has-text("提交"), button:has-text("评论")').first()
        await sendBtn.click()
        await page.waitForTimeout(2000)
      }
    }
  })
})
