# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: login-flow.spec.js >> Article Detail >> should toggle favorite button state
- Location: tests/e2e/login-flow.spec.js:309:7

# Error details

```
Error: locator.click: Target page, context or browser has been closed
Call log:
  - waiting for locator('[data-testid="article-card"], .article-card').first()

```

```
Error: write EPIPE
```