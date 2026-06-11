# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: comment-system.spec.js >> 评论系统 E2E 测试 >> 文章详情应该有点赞和收藏功能按钮
- Location: tests/e2e/comment-system.spec.js:97:7

# Error details

```
Error: expect(received).toBeTruthy()

Received: false
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
  - alert [ref=e17]:
    - img [ref=e19]
    - paragraph [ref=e21]: 页面导航失败，请稍后重试
  - alert [ref=e22]:
    - img [ref=e24]
    - paragraph [ref=e26]: 网络错误，请稍后重试
```

# Test source

```ts
  10  | 
  11  |   const usernameInput = page.locator('input[placeholder*="用户名/邮箱/手机号"], input[autocomplete="username"]').first()
  12  |   const passwordInput = page.locator('input[type="password"]').first()
  13  | 
  14  |   await usernameInput.fill(TEST_USERNAME)
  15  |   await passwordInput.fill(TEST_PASSWORD)
  16  | 
  17  |   const captchaInput = page.locator('input[placeholder*="验证码"]').first()
  18  |   const captchaVisible = await captchaInput.isVisible().catch(() => false)
  19  |   if (captchaVisible) {
  20  |     await captchaInput.fill('1234')
  21  |   }
  22  | 
  23  |   await page.locator('.login-button').first().click()
  24  |   await page.waitForTimeout(3000)
  25  | }
  26  | 
  27  | test.describe('评论系统 E2E 测试', () => {
  28  |   test('未登录用户应该看到登录提示而非评论输入框', async ({ page }) => {
  29  |     await page.goto('/article/1')
  30  |     await page.waitForLoadState('domcontentloaded')
  31  |     await page.waitForTimeout(2000)
  32  | 
  33  |     // 未登录时评论输入框应该不可见，或显示"登录后评论"提示
  34  |     const textarea = page.locator('textarea').first()
  35  |     const textareaVisible = await textarea.isVisible().catch(() => false)
  36  |     if (textareaVisible) {
  37  |       // 如果有textarea但被禁用，也应该能看到登录提示
  38  |       const loginPrompt = page.locator('text=登录', 'text=评论')
  39  |       const promptVisible = await loginPrompt.first().isVisible().catch(() => false)
  40  |       expect(promptVisible || !textareaVisible).toBeTruthy()
  41  |     }
  42  |   })
  43  | 
  44  |   test('登录用户应该能看到评论输入框', async ({ page }) => {
  45  |     await login(page)
  46  |     await page.waitForTimeout(1000)
  47  |     await page.goto('/article/1')
  48  |     await page.waitForLoadState('domcontentloaded')
  49  |     await page.waitForTimeout(2000)
  50  | 
  51  |     // 登录后应该能看到评论输入框
  52  |     const textarea = page.locator('textarea').first()
  53  |     await expect(textarea).toBeVisible({ timeout: 10000 })
  54  |   })
  55  | 
  56  |   test('登录用户应该能提交评论', async ({ page }) => {
  57  |     await login(page)
  58  |     await page.waitForTimeout(1000)
  59  |     await page.goto('/article/1')
  60  |     await page.waitForLoadState('domcontentloaded')
  61  |     await page.waitForTimeout(2000)
  62  | 
  63  |     const textarea = page.locator('textarea').first()
  64  |     const isVisible = await textarea.isVisible().catch(() => false)
  65  | 
  66  |     if (isVisible) {
  67  |       const uniqueComment = `E2E测试评论-${Date.now()}`
  68  |       await textarea.fill(uniqueComment)
  69  | 
  70  |       // 查找提交按钮
  71  |       const submitBtn = page.locator('button:has-text("发表"), button:has-text("提交"), .el-button--primary').first()
  72  |       const submitVisible = await submitBtn.isVisible().catch(() => false)
  73  | 
  74  |       if (submitVisible) {
  75  |         await submitBtn.click()
  76  |         await page.waitForTimeout(3000)
  77  | 
  78  |         // 提交后应该看到成功提示或评论出现在列表中
  79  |         const successMsg = page.locator('text=成功, text=评论成功')
  80  |         const msgVisible = await successMsg.first().isVisible().catch(() => false)
  81  |         expect(msgVisible || true).toBeTruthy() // 允许部分成功场景
  82  |       }
  83  |     }
  84  |   })
  85  | 
  86  |   test('评论应该显示在评论列表中', async ({ page }) => {
  87  |     await page.goto('/article/1')
  88  |     await page.waitForLoadState('domcontentloaded')
  89  |     await page.waitForTimeout(3000)
  90  | 
  91  |     // 文章详情页应该有评论区
  92  |     const commentSection = page.locator('text=评论')
  93  |     const sectionVisible = await commentSection.first().isVisible().catch(() => false)
  94  |     expect(sectionVisible).toBeTruthy()
  95  |   })
  96  | 
  97  |   test('文章详情应该有点赞和收藏功能按钮', async ({ page }) => {
  98  |     await page.goto('/article/1')
  99  |     await page.waitForLoadState('domcontentloaded')
  100 |     await page.waitForTimeout(2000)
  101 | 
  102 |     // 应该有点赞按钮
  103 |     const likeBtn = page.locator('button:has-text("点赞"), [class*="like"], .action-btn:has-text("赞")').first()
  104 |     const likeVisible = await likeBtn.isVisible().catch(() => false)
  105 | 
  106 |     // 应该有收藏按钮
  107 |     const favBtn = page.locator('button:has-text("收藏"), [class*="favorite"], .action-btn:has-text("收藏")').first()
  108 |     const favVisible = await favBtn.isVisible().catch(() => false)
  109 | 
> 110 |     expect(likeVisible || favVisible).toBeTruthy()
      |                                       ^ Error: expect(received).toBeTruthy()
  111 |   })
  112 | 
  113 |   test('评论区域应该支持字数限制', async ({ page }) => {
  114 |     await login(page)
  115 |     await page.waitForTimeout(1000)
  116 |     await page.goto('/article/1')
  117 |     await page.waitForLoadState('domcontentloaded')
  118 |     await page.waitForTimeout(2000)
  119 | 
  120 |     const textarea = page.locator('textarea').first()
  121 |     const isVisible = await textarea.isVisible().catch(() => false)
  122 | 
  123 |     if (isVisible) {
  124 |       // 检查是否有字数提示
  125 |       const charCount = page.locator('text=/\\d+\\s*\\/\\s*\\d+/').first()
  126 |       const charCountVisible = await charCount.isVisible().catch(() => false)
  127 | 
  128 |       // 检查maxlength属性
  129 |       const maxlength = await textarea.getAttribute('maxlength').catch(() => null)
  130 | 
  131 |       expect(charCountVisible || maxlength !== null || true).toBeTruthy()
  132 |     }
  133 |   })
  134 | })
  135 | 
```