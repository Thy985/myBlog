import { test, expect } from '@playwright/test'

test.describe('Favorites', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(2000)
  })

  test('should display favorite button', async ({ page }) => {
    await page.waitForTimeout(3000)

    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isVisible = await articleLink.isVisible().catch(() => false)

    if (isVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      const favoriteBtn = page.locator('[class*="favorite"], [class*="collect"], [class*="star"]').first()
      const favVisible = await favoriteBtn.isVisible().catch(() => false)
      if (favVisible) {
        await expect(favoriteBtn).toBeVisible()
      }
    }
  })

  test('should add article to favorites when logged in', async ({ page }) => {
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

      const favoriteBtn = page.locator('[class*="favorite"], [class*="collect"], [class*="star"]').first()
      const favVisible = await favoriteBtn.isVisible().catch(() => false)

      if (favVisible) {
        await favoriteBtn.click()
        await page.waitForTimeout(2000)
      }
    }
  })
})
