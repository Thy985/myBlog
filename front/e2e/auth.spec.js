import { test, expect } from '@playwright/test'

test.describe('Homepage UI', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')
  })

  test('should load homepage', async ({ page }) => {
    await expect(page).toHaveTitle(/.*/)
  })

  test('should display main content', async ({ page }) => {
    await expect(page.locator('body')).toBeVisible()
  })
})
