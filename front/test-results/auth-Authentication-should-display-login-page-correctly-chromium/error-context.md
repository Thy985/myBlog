# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: auth.spec.js >> Authentication >> should display login page correctly
- Location: e2e\auth.spec.js:10:7

# Error details

```
Test timeout of 60000ms exceeded.
```

```
Error: expect(page).toHaveTitle(expected) failed

Expected pattern: /XingChen|博客|Blog/
Received string:  ""

Call log:
  - Expect "toHaveTitle" with timeout 15000ms

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | 
  3  | test.describe('Authentication', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/')
  6  |     await page.waitForLoadState('domcontentloaded')
  7  |     await page.waitForTimeout(1000)
  8  |   })
  9  | 
  10 |   test('should display login page correctly', async ({ page }) => {
> 11 |     await expect(page).toHaveTitle(/XingChen|博客|Blog/)
     |                        ^ Error: expect(page).toHaveTitle(expected) failed
  12 |     const loginBtn = page.locator('button:has-text("登录")').first()
  13 |     await expect(loginBtn).toBeVisible()
  14 |   })
  15 | 
  16 |   test('should login with valid credentials', async ({ page }) => {
  17 |     const loginBtn = page.locator('button:has-text("登录")').first()
  18 |     await loginBtn.click()
  19 |     await page.waitForTimeout(1000)
  20 | 
  21 |     const dialog = page.locator('[role="dialog"], .el-dialog, .dialog').first()
  22 |     const usernameInput = dialog.locator('input').first()
  23 |     const passwordInput = dialog.locator('input[type="password"]').first()
  24 | 
  25 |     await usernameInput.fill('admin')
  26 |     await passwordInput.fill('147258369Thy@')
  27 | 
  28 |     const submitBtn = dialog.locator('button:has-text("登录"), button[type="submit"]').first()
  29 |     await submitBtn.click()
  30 | 
  31 |     await page.waitForTimeout(3000)
  32 | 
  33 |     const userInfo = page.locator('text=admin').first()
  34 |     await expect(userInfo).toBeVisible({ timeout: 15000 })
  35 |   })
  36 | 
  37 |   test('should logout successfully', async ({ page }) => {
  38 |     const loginBtn = page.locator('button:has-text("登录")').first()
  39 |     await loginBtn.click()
  40 |     await page.waitForTimeout(1000)
  41 | 
  42 |     const dialog = page.locator('[role="dialog"], .el-dialog, .dialog').first()
  43 |     const usernameInput = dialog.locator('input').first()
  44 |     const passwordInput = dialog.locator('input[type="password"]').first()
  45 | 
  46 |     await usernameInput.fill('admin')
  47 |     await passwordInput.fill('147258369Thy@')
  48 | 
  49 |     const submitBtn = dialog.locator('button:has-text("登录"), button[type="submit"]').first()
  50 |     await submitBtn.click()
  51 | 
  52 |     await page.waitForTimeout(3000)
  53 | 
  54 |     const userInfo = page.locator('text=admin').first()
  55 |     await expect(userInfo).toBeVisible({ timeout: 15000 })
  56 | 
  57 |     const logoutBtn = page.locator('button:has-text("退出"), button:has-text("Logout")').first()
  58 |     if (await logoutBtn.isVisible()) {
  59 |       await logoutBtn.click()
  60 |       await page.waitForTimeout(1000)
  61 |     }
  62 |   })
  63 | })
```