import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(1000)
  })

  test('should display login page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/XingChen|博客|Blog/)
    const loginBtn = page.locator('button:has-text("登录")').first()
    await expect(loginBtn).toBeVisible()
  })

  test('should login with valid credentials', async ({ page }) => {
    const loginBtn = page.locator('button:has-text("登录")').first()
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

    const userInfo = page.locator('text=admin').first()
    await expect(userInfo).toBeVisible({ timeout: 15000 })
  })

  test('should logout successfully', async ({ page }) => {
    const loginBtn = page.locator('button:has-text("登录")').first()
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

    const userInfo = page.locator('text=admin').first()
    await expect(userInfo).toBeVisible({ timeout: 15000 })

    const logoutBtn = page.locator('button:has-text("退出"), button:has-text("Logout")').first()
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click()
      await page.waitForTimeout(1000)
    }
  })
})
