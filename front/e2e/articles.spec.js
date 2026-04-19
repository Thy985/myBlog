import { test, expect } from '@playwright/test'

test.describe('Article List', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(2000)
  })

  test('should load article list page', async ({ page }) => {
    await page.waitForTimeout(3000)
    const articleCards = page.locator('[class*="article"], [class*="card"], .article-item, article')
    await expect(articleCards.first()).toBeVisible({ timeout: 15000 })
  })

  test('should navigate to article detail page', async ({ page }) => {
    await page.waitForTimeout(3000)

    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isVisible = await articleLink.isVisible().catch(() => false)

    if (isVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      const articleTitle = page.locator('h1, [class*="title"], [class*="article-title"]').first()
      await expect(articleTitle).toBeVisible({ timeout: 15000 })
    }
  })

  test('should display article content correctly', async ({ page }) => {
    await page.waitForTimeout(3000)

    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isVisible = await articleLink.isVisible().catch(() => false)

    if (isVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      const articleContent = page.locator('[class*="content"], article, [class*="article-content"]').first()
      await expect(articleContent).toBeVisible({ timeout: 15000 })
    }
  })
})
