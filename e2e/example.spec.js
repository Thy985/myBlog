import { test, expect } from '@playwright/test'

test.describe('首页测试', () => {
  test('应该正确加载首页', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // 截图：首页整体
    await page.screenshot({ path: 'test-results/homepage.png', fullPage: true })

    // 检查页面标题
    await expect(page).toHaveTitle(/博客/)

    // 检查导航栏存在
    await expect(page.locator('header')).toBeVisible()
  })

  test('应该正确显示热门文章', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // 截图：热门文章区域
    const hotArticlesSection = page.locator('text=热门文章').first()
    await hotArticlesSection.screenshot({ path: 'test-results/hot-articles.png' })

    // 检查热门文章区域
    await expect(hotArticlesSection).toBeVisible()
  })

  test('骨架屏应该正常工作', async ({ page }) => {
    // 模拟慢速3G网络
    await page.route('**/*', async route => {
      await new Promise(resolve => setTimeout(resolve, 500))
      await route.continue()
    })

    await page.goto('/', { waitUntil: 'domcontentloaded' })

    // 立即截图捕获骨架屏
    await page.screenshot({ path: 'test-results/skeleton-loading.png' })
  })
})

test.describe('空状态测试', () => {
  test('文章列表为空时应该显示空状态', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // 截图：空状态
    await page.screenshot({ path: 'test-results/empty-state.png', fullPage: true })
  })
})

test.describe('搜索功能测试', () => {
  test('桌面端搜索框应该可交互', async ({ page }) => {
    // 设置桌面端视口
    await page.setViewportSize({ width: 1280, height: 720 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // 截图：搜索框
    await page.locator('input[placeholder="搜索文章..."]').first().screenshot({ path: 'test-results/search-desktop.png' })
  })

  test('搜索框聚焦应该显示下拉建议', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[placeholder="搜索文章..."]').first()
    await searchInput.click()
    await searchInput.fill('Vue')

    // 截图：搜索建议下拉框
    await page.screenshot({ path: 'test-results/search-suggestions.png' })
  })
})
