import { chromium } from '@playwright/test'

const BASE_URL = 'http://localhost:5173'

async function runHarshUserTest() {
  console.log('Starting harsh user evaluation...\n')

  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext()
  const page = await context.newPage()

  const issues = []
  const praises = []

  // Collect console errors
  const consoleErrors = []
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text())
    }
  })

  // Collect page errors
  page.on('pageerror', err => {
    consoleErrors.push(err.message)
  })

  try {
    // Test 1: Homepage load and visual check
    console.log('Test 1: Loading homepage...')
    await page.goto(BASE_URL, { waitUntil: 'networkidle', timeout: 30000 })
    await page.waitForTimeout(2000)

    // Check if main content loaded
    const hasHeader = await page.locator('Header').count() > 0
    const hasArticles = await page.locator('.index-page').count() > 0

    if (hasHeader && hasArticles) {
      praises.push('✓ Homepage loads successfully')
    } else {
      issues.push('✗ Homepage failed to load properly')
    }

    // Test 2: Check for visual hierarchy
    console.log('Test 2: Checking visual hierarchy...')
    const titleCount = await page.locator('h2').count()
    const articleCards = await page.locator('[data-testid="article-card"]').count()

    if (titleCount > 0 && articleCards > 0) {
      praises.push(`✓ Found ${titleCount} section titles and ${articleCards} article cards`)
    } else {
      issues.push('✗ Missing expected content structure')
    }

    // Test 3: Check user info card
    console.log('Test 3: Checking user info card...')
    const userAvatar = await page.locator('.mb-3 img').first()
    const avatarSrc = await userAvatar.getAttribute('src')
    if (avatarSrc && !avatarSrc.includes('undefined')) {
      praises.push('✓ User avatar displays correctly')
    } else {
      issues.push('✗ User avatar not displaying properly')
    }

    // Test 4: Navigate to article detail
    console.log('Test 4: Testing article detail page...')
    const firstArticle = page.locator('[data-testid="article-card"]').first()
    if (await firstArticle.count() > 0) {
      await firstArticle.click()
      await page.waitForTimeout(2000)

      const detailPageLoaded = await page.locator('.article-detail-page').count() > 0
      if (detailPageLoaded) {
        praises.push('✓ Article detail page loads')

        // Check for sidebar issues
        const tocSections = await page.locator('.article-toc').count()
        if (tocSections > 1) {
          issues.push('✗ Duplicate table of contents found in sidebar')
        } else {
          praises.push('✓ No duplicate TOC in sidebar')
        }

        // Check for comment section
        const hasComments = await page.locator('.comment-list').count() > 0
        if (hasComments) {
          praises.push('✓ Comment section present')
        } else {
          issues.push('✗ Comment section missing')
        }
      } else {
        issues.push('✗ Article detail page failed to load')
      }
    }

    // Test 5: Category navigation
    console.log('Test 5: Testing category navigation...')
    await page.goto(BASE_URL, { waitUntil: 'networkidle' })
    await page.waitForTimeout(1000)

    const categoryLinks = await page.locator('a:has-text("分类")').count()
    if (categoryLinks > 0) {
      // Click on first category
      const categoryLink = page.locator('a:has-text("分类")').first()
      await categoryLink.click()
      await page.waitForTimeout(2000)

      const categoryPageLoaded = await page.url()
      if (categoryPageLoaded.includes('category')) {
        praises.push('✓ Category navigation works')
      } else {
        issues.push('✗ Category navigation not working')
      }
    }

    // Test 6: Check for broken images
    console.log('Test 6: Checking for broken images...')
    const images = await page.locator('img').all()
    let brokenImages = 0
    for (const img of images) {
      const src = await img.getAttribute('src')
      const dataSrc = await img.getAttribute('data-src')
      const finalSrc = src || dataSrc

      if (finalSrc && (finalSrc.includes('@/') || finalSrc.includes('undefined') || finalSrc === '')) {
        brokenImages++
      }
    }

    if (brokenImages === 0) {
      praises.push('✓ No broken images detected')
    } else {
      issues.push(`✗ Found ${brokenImages} potentially broken images`)
    }

    // Test 7: Check tag display
    console.log('Test 7: Checking tag display...')
    await page.goto(BASE_URL, { waitUntil: 'networkidle' })
    await page.waitForTimeout(1000)

    const tagElements = await page.locator('.tag-item, [class*="tag-item"]').count()
    if (tagElements > 0) {
      praises.push(`✓ Found ${tagElements} tags displayed`)
    } else {
      // Check if tags exist in article data by looking at inner HTML
      const firstCard = page.locator('[data-testid="article-card"]').first()
      if (await firstCard.count() > 0) {
        const cardHTML = await firstCard.innerHTML()
        const hasTagClass = cardHTML.includes('tag')
        const hasHashSymbol = cardHTML.includes('#')
        if (hasTagClass || hasHashSymbol) {
          praises.push('✓ Tags rendered in article cards')
        } else {
          issues.push('⚠ No tags found in article cards (API may not return tag data)')
        }
      }
    }

    // Test 8: Responsive design check
    console.log('Test 8: Checking responsive design...')
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto(BASE_URL, { waitUntil: 'networkidle' })
    await page.waitForTimeout(1000)

    praises.push('✓ Mobile viewport renders without crash')

    // Test 9: Dark mode toggle if exists
    console.log('Test 9: Checking dark mode...')
    const darkModeToggle = page.locator('button:has-text("深色"), button:has-text("Dark"), [class*="dark-mode"]').first()
    if (await darkModeToggle.count() > 0) {
      await darkModeToggle.click()
      await page.waitForTimeout(500)
      const isDark = await page.locator('html.dark, html[class*="dark"]').count() > 0
      if (isDark) {
        praises.push('✓ Dark mode toggle works')
      }
    }

    // Test 10: Check for console errors
    console.log('Test 10: Analyzing console errors...')
    const criticalErrors = consoleErrors.filter(e =>
      !e.includes('warning') &&
            !e.includes('Warning') &&
            !e.includes('devtools') &&
            !e.includes('favicon')
    )

    if (criticalErrors.length === 0) {
      praises.push('✓ No critical console errors')
    } else {
      issues.push(`✗ Found ${criticalErrors.length} console errors:`)
      criticalErrors.slice(0, 3).forEach(e => {
        issues.push(`  - ${e.substring(0, 100)}`)
      })
    }

    // Final verdict
    console.log('\n' + '='.repeat(50))
    console.log('HARSH USER EVALUATION RESULTS')
    console.log('='.repeat(50) + '\n')

    console.log('PRAISES (' + praises.length + '):')
    praises.forEach(p => console.log('  ' + p))

    console.log('\nISSUES (' + issues.length + '):')
    issues.forEach(i => console.log('  ' + i))

    console.log('\n' + '='.repeat(50))
    if (issues.length === 0) {
      console.log('🎉 PERFECT SCORE! All tests passed!')
    } else if (issues.length <= 3) {
      console.log('👍 GOOD - Minor issues found')
    } else if (issues.length <= 6) {
      console.log('⚠️  NEEDS IMPROVEMENT - Several issues found')
    } else {
      console.log('❌ POOR - Many issues need fixing')
    }
    console.log('='.repeat(50))

  } catch (error) {
    console.error('Test failed with error:', error.message)
  } finally {
    await browser.close()
  }
}

runHarshUserTest()
