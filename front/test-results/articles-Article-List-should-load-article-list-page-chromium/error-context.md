# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: articles.spec.js >> Article List >> should load article list page
- Location: tests/e2e/articles.spec.js:10:7

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('[class*="article"], [class*="card"], .article-item, article').first()
Expected: visible
Timeout: 15000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 15000ms
  - waiting for locator('[class*="article"], [class*="card"], .article-item, article').first()

```

```yaml
- text: 加载中...
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | 
  3  | test.describe('Article List', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/')
  6  |     await page.waitForLoadState('domcontentloaded')
  7  |     await page.waitForTimeout(2000)
  8  |   })
  9  | 
  10 |   test('should load article list page', async ({ page }) => {
  11 |     await page.waitForTimeout(3000)
  12 |     const articleCards = page.locator('[class*="article"], [class*="card"], .article-item, article')
> 13 |     await expect(articleCards.first()).toBeVisible({ timeout: 15000 })
     |                                        ^ Error: expect(locator).toBeVisible() failed
  14 |   })
  15 | 
  16 |   test('should navigate to article detail page', async ({ page }) => {
  17 |     await page.waitForTimeout(3000)
  18 | 
  19 |     const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
  20 |     const isVisible = await articleLink.isVisible().catch(() => false)
  21 | 
  22 |     if (isVisible) {
  23 |       await articleLink.click()
  24 |       await page.waitForLoadState('domcontentloaded')
  25 |       await page.waitForTimeout(2000)
  26 | 
  27 |       const articleTitle = page.locator('h1, [class*="title"], [class*="article-title"]').first()
  28 |       await expect(articleTitle).toBeVisible({ timeout: 15000 })
  29 |     }
  30 |   })
  31 | 
  32 |   test('should display article content correctly', async ({ page }) => {
  33 |     await page.waitForTimeout(3000)
  34 | 
  35 |     const articleLink = page.locator('a[href*="/article/"], a[href*="/articles/"]').first()
  36 |     const isVisible = await articleLink.isVisible().catch(() => false)
  37 | 
  38 |     if (isVisible) {
  39 |       await articleLink.click()
  40 |       await page.waitForLoadState('domcontentloaded')
  41 |       await page.waitForTimeout(2000)
  42 | 
  43 |       const articleContent = page.locator('[class*="content"], article, [class*="article-content"]').first()
  44 |       await expect(articleContent).toBeVisible({ timeout: 15000 })
  45 |     }
  46 |   })
  47 | })
  48 | 
```