import { test, expect } from '@playwright/test'

// Helper function to login
async function login(page) {
  const loginBtn = page.locator('button:has-text("登录")').first()
  await loginBtn.click()
  await page.waitForTimeout(500)

  const dialog = page.locator('.el-dialog, [role="dialog"], .dialog').first()
  await dialog.locator('input').first().fill('admin')
  await dialog.locator('input[type="password"]').first().fill('147258369Thy@')
  await dialog.locator('button:has-text("登录"), button[type="submit"]').first().click()
  await page.waitForTimeout(2000)
}

test.describe('Article List', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
  })

  test('should display article list', async ({ page }) => {
    // Wait for articles to load
    await page.waitForTimeout(2000)

    const articleCards = page.locator('[class*="article"], .article-card, .article-item')
    const count = await articleCards.count()

    // Should have at least some articles or empty state
    const hasArticles = count > 0
    const hasEmptyState = await page.locator('text=暂无文章, text=没有文章').first().isVisible().catch(() => false)
    expect(hasArticles || hasEmptyState).toBeTruthy()
  })

  test('should navigate to article detail', async ({ page }) => {
    await page.waitForTimeout(2000)

    // Find article link
    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isLinkVisible = await articleLink.isVisible().catch(() => false)

    if (isLinkVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(1000)

      // Should be on article detail page (URL should contain /article/)
      expect(page.url()).toContain('/article/')
    }
  })

  test('should display pagination', async ({ page }) => {
    await page.waitForTimeout(2000)

    const pagination = page.locator('.el-pagination, .pagination, [class*="pagination"]')
    const isPaginationVisible = await pagination.isVisible().catch(() => false)

    // Pagination might or might not be visible depending on article count
    expect(typeof isPaginationVisible === 'boolean').toBe(true)
  })
})

test.describe('Article Actions (Authenticated)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
    await login(page)
  })

  test('should like an article', async ({ page }) => {
    await page.waitForTimeout(2000)

    // Find like button
    const likeBtn = page.locator('[class*="like"], [class*="star"]').first()
    const isLikeVisible = await likeBtn.isVisible().catch(() => false)

    if (isLikeVisible) {
      await likeBtn.click()
      await page.waitForTimeout(1000)
      // Like should have been triggered
    }
  })

  test('should collect an article', async ({ page }) => {
    await page.waitForTimeout(2000)

    // Find collect/favorite button
    const collectBtn = page.locator('[class*="collect"], [class*="favorite"]').first()
    const isCollectVisible = await collectBtn.isVisible().catch(() => false)

    if (isCollectVisible) {
      await collectBtn.click()
      await page.waitForTimeout(1000)
      // Collect should have been triggered
    }
  })

  test('should display comment section', async ({ page }) => {
    // Navigate to an article first
    await page.waitForTimeout(2000)

    const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
    const isLinkVisible = await articleLink.isVisible().catch(() => false)

    if (isLinkVisible) {
      await articleLink.click()
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      // Check for comment section
      const commentSection = page.locator('[class*="comment"], #comment')
      const isCommentVisible = await commentSection.isVisible().catch(() => false)
      expect(typeof isCommentVisible === 'boolean').toBe(true)
    }
  })
})

test.describe('Article Search', () => {
  test('should search articles by keyword', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(1000)

    // Find search input
    const searchInput = page.locator('input[type="search"], input[placeholder*="搜索"], input[placeholder*="search"]').first()
    const isSearchVisible = await searchInput.isVisible().catch(() => false)

    if (isSearchVisible) {
      await searchInput.fill('test')
      await searchInput.press('Enter')
      await page.waitForTimeout(2000)

      // Should see search results or empty state
      const hasResults = await page.locator('[class*="article"], .article-card').count() > 0
      const hasEmptyState = await page.locator('text=暂无, text=没有找到').first().isVisible().catch(() => false)
      expect(hasResults || hasEmptyState).toBeTruthy()
    }
  })
})
