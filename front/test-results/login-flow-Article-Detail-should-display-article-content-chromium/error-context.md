# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: login-flow.spec.js >> Article Detail >> should display article content
- Location: tests/e2e/login-flow.spec.js:241:7

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for locator('[data-testid="article-card"], .article-card').first()

```

# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - button "打开 AI 助手" [ref=e3]:
    - img [ref=e4]
  - generic [ref=e8]:
    - generic [ref=e9]: "[plugin:vite:vue] [vue/compiler-sfc] Unexpected token, expected \",\" (18:12) /workspace/front/src/components/layout/MobileMenu.vue 165| 166| const props = defineProps<{ 167| modelValue?: string 168| isOpen?: boolean 169| }>()"
    - generic [ref=e10]: /workspace/front/src/components/layout/MobileMenu.vue:18:12
    - generic [ref=e11]: 14 | <!-- 移动端搜索框 - 带搜索建议 --> 15 | <div class="relative mb-4"> 16 | <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"> | ^ 17 | <svg class="w-4 h-4 text-text-tertiary" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" v... 18 | <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m19 19-4-4...
    - generic [ref=e12]: at constructor (/workspace/front/node_modules/@babel/parser/lib/index.js:365:19) at Parser.raise (/workspace/front/node_modules/@babel/parser/lib/index.js:6599:19) at Parser.unexpected (/workspace/front/node_modules/@babel/parser/lib/index.js:6619:16) at Parser.expect (/workspace/front/node_modules/@babel/parser/lib/index.js:6899:12) at Parser.parseObjectLike (/workspace/front/node_modules/@babel/parser/lib/index.js:11822:14) at Parser.parseExprAtom (/workspace/front/node_modules/@babel/parser/lib/index.js:11339:23) at Parser.parseExprSubscripts (/workspace/front/node_modules/@babel/parser/lib/index.js:11081:23) at Parser.parseUpdate (/workspace/front/node_modules/@babel/parser/lib/index.js:11066:21) at Parser.parseMaybeUnary (/workspace/front/node_modules/@babel/parser/lib/index.js:11046:23) at Parser.parseMaybeUnaryOrPrivate (/workspace/front/node_modules/@babel/parser/lib/index.js:10899:61)
    - generic [ref=e13]:
      - text: Click outside, press Esc key, or fix the code to dismiss.
      - text: You can also disable this overlay by setting
      - code [ref=e14]: server.hmr.overlay
      - text: to
      - code [ref=e15]: "false"
      - text: in
      - code [ref=e16]: vite.config.js
      - text: .
```

# Test source

```ts
  132 |     }
  133 |     expect(isVisible || page.url().includes('/login')).toBeTruthy()
  134 |   })
  135 | 
  136 |   test('should display logout option after login', async ({ page }) => {
  137 |     await login(page)
  138 |     await page.waitForTimeout(2000)
  139 |     const userMenuBtn = page.locator('#user-menu-button').first()
  140 |     const isBtnVisible = await userMenuBtn.isVisible().catch(() => false)
  141 | 
  142 |     if (isBtnVisible) {
  143 |       await userMenuBtn.click()
  144 |       await page.waitForTimeout(500)
  145 |       const logoutOption = page.locator('text=退出登录').first()
  146 |       await expect(logoutOption).toBeVisible({ timeout: 5000 })
  147 |     } else {
  148 |       expect(page.url().includes('/login')).toBeTruthy()
  149 |     }
  150 |   })
  151 | 
  152 |   test('should logout successfully', async ({ page }) => {
  153 |     await login(page)
  154 |     await page.waitForTimeout(2000)
  155 |     const userMenuBtn = page.locator('#user-menu-button').first()
  156 |     const isBtnVisible = await userMenuBtn.isVisible().catch(() => false)
  157 | 
  158 |     if (isBtnVisible) {
  159 |       await userMenuBtn.click()
  160 |       await page.waitForTimeout(1000)
  161 |       await page.locator('a:has-text("退出登录"), [role="menuitem"]:has-text("退出登录")').click()
  162 |       await page.waitForTimeout(2000)
  163 |       await page.waitForLoadState('domcontentloaded')
  164 |     } else {
  165 |       expect(page.url().includes('/login')).toBeTruthy()
  166 |     }
  167 |   })
  168 | 
  169 |   test('should navigate to user settings page', async ({ page }) => {
  170 |     await page.goto('/user/settings')
  171 |     await page.waitForLoadState('domcontentloaded')
  172 |     await page.waitForTimeout(1000)
  173 | 
  174 |     const settingsContent = page.locator('text=设置, text=个人资料, text=账号设置').first()
  175 |     const isVisible = await settingsContent.isVisible().catch(() => false)
  176 |     expect(isVisible || page.url().includes('settings')).toBeTruthy()
  177 |   })
  178 | 
  179 |   test('should navigate to user favorites page', async ({ page }) => {
  180 |     await page.goto('/user/favorites')
  181 |     await page.waitForLoadState('domcontentloaded')
  182 |     await page.waitForTimeout(1000)
  183 | 
  184 |     const favoritesContent = page.locator('text=收藏, h1:has-text("收藏")').first()
  185 |     const isVisible = await favoritesContent.isVisible().catch(() => false)
  186 |     expect(isVisible || page.url().includes('favorites')).toBeTruthy()
  187 |   })
  188 | 
  189 |   test('should navigate to user articles page', async ({ page }) => {
  190 |     await page.goto('/user/articles')
  191 |     await page.waitForLoadState('domcontentloaded')
  192 |     await page.waitForTimeout(1000)
  193 | 
  194 |     const articlesContent = page.locator('text=文章, h1:has-text("文章")').first()
  195 |     const isVisible = await articlesContent.isVisible().catch(() => false)
  196 |     expect(isVisible || page.url().includes('articles')).toBeTruthy()
  197 |   })
  198 | })
  199 | 
  200 | test.describe('Article List', () => {
  201 |   test.beforeEach(async ({ page }) => {
  202 |     await page.goto('/')
  203 |     await page.waitForLoadState('domcontentloaded')
  204 |   })
  205 | 
  206 |   test('should load and display article list', async ({ page }) => {
  207 |     const articleCards = page.locator('[data-testid="article-card"], .article-card')
  208 |     await expect(articleCards.first()).toBeVisible({ timeout: 15000 })
  209 |   })
  210 | 
  211 |   test('should navigate to article detail page', async ({ page }) => {
  212 |     const articleCards = page.locator('[data-testid="article-card"], .article-card')
  213 |     await articleCards.first().click()
  214 |     await page.waitForLoadState('domcontentloaded')
  215 |     await expect(page).toHaveURL(/\/article\/\d+/, { timeout: 10000 })
  216 |   })
  217 | 
  218 |   test('should display article content on detail page', async ({ page }) => {
  219 |     const articleCards = page.locator('[data-testid="article-card"], .article-card')
  220 |     await articleCards.first().click()
  221 |     await page.waitForLoadState('domcontentloaded')
  222 |     const content = page.locator('.article-content, article, [class*="content"]').first()
  223 |     await expect(content).toBeVisible({ timeout: 10000 })
  224 |   })
  225 | })
  226 | 
  227 | test.describe('Article Detail', () => {
  228 |   test.beforeEach(async ({ page }) => {
  229 |     await page.goto('/')
  230 |     await page.waitForLoadState('domcontentloaded')
  231 |     const articleCards = page.locator('[data-testid="article-card"], .article-card')
> 232 |     await articleCards.first().click()
      |                                ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  233 |     await page.waitForLoadState('domcontentloaded')
  234 |   })
  235 | 
  236 |   test('should display article title', async ({ page }) => {
  237 |     const title = page.locator('h1, [class*="title"]').first()
  238 |     await expect(title).toBeVisible({ timeout: 10000 })
  239 |   })
  240 | 
  241 |   test('should display article content', async ({ page }) => {
  242 |     const content = page.locator('.article-content, article').first()
  243 |     await expect(content).toBeVisible({ timeout: 10000 })
  244 |   })
  245 | 
  246 |   test('should display like button', async ({ page }) => {
  247 |     await page.waitForTimeout(1000)
  248 |     const likeBtn = page.locator('button:has-text("点赞")').first()
  249 |     const isVisible = await likeBtn.isVisible().catch(() => false)
  250 |     expect(isVisible).toBeTruthy()
  251 |   })
  252 | 
  253 |   test('should display favorite button', async ({ page }) => {
  254 |     await page.waitForTimeout(1000)
  255 |     const favoriteBtn = page.locator('button:has-text("收藏")').first()
  256 |     const isVisible = await favoriteBtn.isVisible().catch(() => false)
  257 |     expect(isVisible).toBeTruthy()
  258 |   })
  259 | 
  260 |   test('should display comment section', async ({ page }) => {
  261 |     await page.waitForTimeout(1000)
  262 |     const commentSection = page.locator('h3:has-text("评论")').first()
  263 |     const isVisible = await commentSection.isVisible().catch(() => false)
  264 |     expect(isVisible).toBeTruthy()
  265 |   })
  266 | 
  267 |   test('should display comment input when logged in', async ({ page }) => {
  268 |     await login(page)
  269 |     await page.waitForTimeout(2000)
  270 |     await page.goto('/article/1')
  271 |     await page.waitForLoadState('domcontentloaded')
  272 |     await page.waitForTimeout(2000)
  273 |     const commentInput = page.locator('textarea').first()
  274 |     const isVisible = await commentInput.isVisible().catch(() => false)
  275 |     expect(isVisible).toBeTruthy()
  276 |   })
  277 | 
  278 |   test('should toggle like button state', async ({ page }) => {
  279 |     await login(page)
  280 |     await page.waitForTimeout(2000)
  281 | 
  282 |     const userMenuBtn = page.locator('#user-menu-button').first()
  283 |     const isLoggedIn = await userMenuBtn.isVisible({ timeout: 2000 }).catch(() => false)
  284 | 
  285 |     if (!isLoggedIn) {
  286 |       console.log('Login failed for like button test, skipping toggle test')
  287 |       test.skip()
  288 |       return
  289 |     }
  290 | 
  291 |     await page.goto('/article/1')
  292 |     await page.waitForLoadState('domcontentloaded')
  293 |     await page.waitForTimeout(2000)
  294 | 
  295 |     const likeBtn = page.locator('button').filter({ hasText: /点赞|^\d+$/ }).first()
  296 |     await expect(likeBtn).toBeVisible({ timeout: 10000 })
  297 | 
  298 |     const initialClass = await likeBtn.getAttribute('class')
  299 |     const isInitiallyActive = initialClass?.includes('bg-red-50')
  300 | 
  301 |     await likeBtn.click()
  302 |     await page.waitForTimeout(2000)
  303 | 
  304 |     const newClass = await likeBtn.getAttribute('class')
  305 |     const isNowActive = newClass?.includes('bg-red-50')
  306 |     expect(isNowActive).toBe(!isInitiallyActive)
  307 |   })
  308 | 
  309 |   test('should toggle favorite button state', async ({ page }) => {
  310 |     await login(page)
  311 |     await page.waitForTimeout(2000)
  312 | 
  313 |     const userMenuBtn = page.locator('#user-menu-button').first()
  314 |     const isLoggedIn = await userMenuBtn.isVisible({ timeout: 2000 }).catch(() => false)
  315 | 
  316 |     if (!isLoggedIn) {
  317 |       console.log('Login failed for favorite button test, skipping toggle test')
  318 |       test.skip()
  319 |       return
  320 |     }
  321 | 
  322 |     await page.goto('/article/1')
  323 |     await page.waitForLoadState('domcontentloaded')
  324 |     await page.waitForTimeout(2000)
  325 | 
  326 |     const favoriteBtn = page.locator('button').filter({ hasText: /收藏|^\d+$/ }).first()
  327 |     await expect(favoriteBtn).toBeVisible({ timeout: 10000 })
  328 | 
  329 |     const initialClass = await favoriteBtn.getAttribute('class')
  330 |     const isInitiallyActive = initialClass?.includes('bg-yellow-50')
  331 | 
  332 |     await favoriteBtn.click()
```