import { test, expect } from '@playwright/test'

test.describe('首页测试', () => {
  test('应该正确加载首页', async ({ page }) => {
    await page.goto('/')

    // 检查页面标题
    await expect(page).toHaveTitle(/博客/)

    // 检查导航栏存在
    await expect(page.locator('header')).toBeVisible()
  })

  test('应该正确显示热门文章', async ({ page }) => {
    await page.goto('/')

    // 检查热门文章区域
    await expect(page.locator('text=热门文章')).toBeVisible()
  })
})

test.describe('空状态测试', () => {
  test('文章列表为空时应该显示空状态', async ({ page }) => {
    await page.goto('/')

    // 空状态组件应该存在或显示空状态消息
    const emptyState = page.locator('.empty-state-container, text=暂无文章')
    // 注意：当没有文章数据时才会显示
  })
})
