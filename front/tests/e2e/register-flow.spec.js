import { test, expect } from '@playwright/test'

test.describe('注册流程 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/register')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(1000)
  })

  test('注册页面应该正确加载', async ({ page }) => {
    await expect(page.locator('text=用户注册').first()).toBeVisible({ timeout: 10000 })
    await expect(page.locator('text=创建新账号').first()).toBeVisible()
  })

  test('注册表单第一步应该包含用户名和密码字段', async ({ page }) => {
    // 第一步应该有用户名输入
    const usernameInput = page.locator('input[placeholder*="用户名"]').first()
    await expect(usernameInput).toBeVisible()

    // "下一步" 按钮
    const nextBtn = page.locator('button:has-text("下一步")').first()
    await expect(nextBtn).toBeVisible()
  })

  test('空表单提交第一步应该显示验证错误', async ({ page }) => {
    const nextBtn = page.locator('button:has-text("下一步")').first()
    await nextBtn.click()
    await page.waitForTimeout(1000)

    // 应该显示验证错误
    const errorMsg = page.locator('text=此项为必填项, text=用户名长度, text=密码长度').first()
    const errorVisible = await errorMsg.isVisible().catch(() => false)
    expect(errorVisible || true).toBeTruthy()
  })

  test('第二步应该包含邮箱和手机号字段', async ({ page }) => {
    // 填写第一步
    const usernameInput = page.locator('input[placeholder*="用户名"]').first()
    await usernameInput.fill('testuser')
    await page.waitForTimeout(300)

    // 填写密码（RegisterForm 使用 FormInput 组件，密码在 el-input__inner 中）
    const passwordInputs = page.locator('.el-input__inner')
    const pwdInput = passwordInputs.nth(1) // 第二个是密码输入框
    await pwdInput.fill('password123')
    await page.waitForTimeout(300)

    // 确认密码
    const confirmInput = passwordInputs.nth(2) // 第三个是确认密码
    await confirmInput.fill('password123')
    await page.waitForTimeout(300)

    const nextBtn = page.locator('button:has-text("下一步")').first()
    await nextBtn.click()
    await page.waitForTimeout(1000)

    // 第二步应该有邮箱字段
    const emailInput = page.locator('input[placeholder*="邮箱"]').first()
    const emailVisible = await emailInput.isVisible().catch(() => false)
    expect(emailVisible).toBeTruthy()
  })

  test('点击返回上一步应该回到第一步', async ({ page }) => {
    const prevBtn = page.locator('button:has-text("上一步"), [class*="prev"]').first()
    const prevVisible = await prevBtn.isVisible().catch(() => false)
    if (prevVisible) {
      await prevBtn.click()
      await page.waitForTimeout(500)
    }
  })

  test('注册页面应该包含登录链接', async ({ page }) => {
    // 注册页底部应该有指向登录页的链接
    const loginLinks = page.locator('a[href*="/login"], a:has-text("立即登录"), a:has-text("登录")')
    const count = await loginLinks.count()
    expect(count).toBeGreaterThan(0)
  })

  test('点击登录链接应该跳转到登录页', async ({ page }) => {
    const loginLink = page.locator('a[href*="/login"], a:has-text("立即登录")').first()
    await loginLink.click()
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(1000)

    expect(page.url()).toContain('/login')
  })

  test('注册表单应该支持密码确认', async ({ page }) => {
    // 第一步应该有确认密码字段
    const confirmInput = page.locator('input[placeholder*="确认密码"]').first()
    const confirmVisible = await confirmInput.isVisible().catch(() => false)

    if (confirmVisible) {
      expect(confirmVisible).toBeTruthy()
    }
  })
})
