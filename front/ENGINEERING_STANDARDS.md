# 前端工程化规范

## 一、项目架构规范

### 1.1 目录结构
```
src/
├── api/                    # API层（禁止重复！）
│   ├── modules/           # 按功能模块拆分（不再按前台/后台拆分）
│   │   ├── article.js     # 文章相关
│   │   ├── comment.js     # 评论相关
│   │   ├── auth.js        # 认证相关
│   │   └── ...
│   └── request.js         # 统一请求封装
├── components/            # 组件
│   ├── common/            # 通用组件
│   ├── business/          # 业务组件
│   ├── form/              # 表单组件
│   ├── layout/            # 布局组件
│   └── ui/                # UI组件
├── composables/           # 组合式函数（逻辑复用）
├── directives/            # 指令
├── plugins/               # 插件
├── router/                # 路由
│   └── index.js           # 统一路由配置
├── stores/                 # 状态管理
│   └── *.js               # 每个store职责单一
├── types/                 # TypeScript类型定义
├── utils/                 # 工具函数
└── views/                 # 页面组件
```

### 1.2 组件规范
- **组件最大行数：300行** 超过必须拆分
- **组件职责单一**：一个组件只做一件事
- **禁止上帝组件**：不允许一个组件干十件事

### 1.3 API层规范
```javascript
// ✅ 正确：统一API模块，按功能拆分
// article.js - 只包含文章相关API
export function getArticle(id) { ... }
export function getArticles(params) { ... }

// ❌ 错误：前台/后台重复定义同一功能
// api/frontend/article.js 和 api/admin/article.js 同时定义 getArticleDetail
```

---

## 二、代码规范

### 2.1 命名规范
| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | PascalCase | `ArticleCard.vue` |
| 工具函数 | camelCase + 动词前缀 | `getUserInfo()`, `formatDate()` |
| Store | useXxxStore | `useAuthStore`, `useArticleStore` |
| 常量 | SCREAMING_SNAKE_CASE | `MAX_PAGE_SIZE`, `API_BASE_URL` |
| CSS类 | kebab-case | `.article-card`, `.user-info` |

### 2.2 Vue组件规范
```vue
<template>
  <!-- 必须有根元素 -->
  <div class="component-name">
    <!-- 组件内容 -->
  </div>
</template>

<script setup>
// 1. 导入按顺序：vue → 库 → 组价 → 工具 → store
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ArticleCard from '@/components/common/ArticleCard.vue'
import { getArticle } from '@/api/article'

// 2. 定义顺序：ref/reactive → computed → methods → lifecycle
const loading = ref(false)
const article = ref(null)

const authorName = computed(() => article.value?.author?.name || '')

// 3. 方法定义（箭头函数）
const fetchArticle = async (id) => {
  loading.value = true
  try {
    article.value = await getArticle(id)
  } finally {
    loading.value = false
  }
}

// 4. 生命周期
onMounted(() => {
  fetchArticle()
})
</script>

<style scoped>
/* 使用CSS变量，禁止硬编码颜色 */
.component-name {
  color: var(--text-primary);
  padding: var(--spacing-md);
}
</style>
```

### 2.3 Store规范
```javascript
// ✅ 正确：职责单一，清晰的state/getter/action
export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref(null)
  const token = ref(null)

  // Getter
  const isLoggedIn = computed(() => !!token.value)

  // Action
  async function login(credentials) {
    // ...
  }

  return { user, token, isLoggedIn, login }
})

// ❌ 错误：向后兼容的委托层
export const useMainStore = defineStore('main', () => {
  const auth = useAuthStore() // 不要在store定义中调用其他store
  // 这种getStores()转发是维护噩梦
})
```

---

## 三、安全规范

### 3.1 Token安全
```javascript
// ✅ 正确：敏感Token使用httpOnly
document.cookie = `token=${token}; httpOnly; secure; sameSite=strict`

// ❌ 错误：Token可被JS读取
localStorage.setItem('token', token) // XSS攻击可读取
```

### 3.2 XSS防护
```vue
<!-- ✅ 正确：使用sanitize指令 -->
<div v-html="content" v-sanitize></div>

// ❌ 错误：先处理再sanitize
const sanitized = DOMPurify.sanitize(content)
renderMath(sanitized) // 可能重新引入XSS
```

### 3.3 认证失败处理
```javascript
// ✅ 正确：刷新失败时清理并重定向
catch(() => {
  clearAuthInfo()
  window.location.href = '/login'
  return Promise.reject(error)
})

// ❌ 错误：刷新失败时导入可能未初始化的store
import('@/stores').then(({ useMainStore }) => {
  useMainStore().logout() // 循环依赖风险
})
```

---

## 四、性能规范

### 4.1 依赖管理
```json
// ✅ 正确：构建工具放devDependencies
{
  "devDependencies": {
    "vite": "^5.x",
    "vite-plugin-imagemin": "^4.x"
  },
  "dependencies": {
    "vue": "^3.x",
    "element-plus": "^2.x"
  }
}

// ❌ 错误：构建插件放dependencies
{
  "dependencies": {
    "vite-plugin-imagemin": "^4.x" // 构建工具！
  }
}
```

### 4.2 按需引入
```javascript
// ✅ 正确：按需引入图标
import { Edit, Delete, Search } from '@element-plus/icons-vue'

// ❌ 错误：全量引入
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// ✅ 正确：只引入使用的语言高亮
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'

// ❌ 错误：全量引入highlight.js
import hljs from 'highlight.js' // 1MB+
```

### 4.3 组件懒加载
```javascript
// ✅ 正确：路由级懒加载
const ArticleDetail = () => import('@/views/article/Detail.vue')

// ✅ 正确：大组件内部按需加载子组件
const MarkdownRenderer = defineAsyncComponent(() => import('./MarkdownRenderer.vue'))
```

---

## 五、测试规范

### 5.1 组件测试
- 每个组件必须有对应的 `.spec.js` 测试文件
- 测试覆盖率目标：核心组件 > 80%

### 5.2 API测试
- 每个API函数必须有测试
- 测试要覆盖：成功、失败、边界条件

---

## 六、Git规范

### 6.1 提交信息
```
<type>: <subject>

type: feat | fix | docs | style | refactor | test | chore

示例：
feat: 添加文章点赞功能
fix: 修复登录页面白屏问题
refactor: 重构article-detail.vue组件
```

### 6.2 分支命名
```
feature/<feature-name>
bugfix/<bug-description>
hotfix/<urgent-fix>
refactor/<scope>-<description>
```

---

## 七、代码审查清单

### 7.1 PR审查要点
- [ ] 无重复代码（复制粘贴率 < 5%）
- [ ] 组件行数 < 300行
- [ ] 无直接DOM操作
- [ ] 无全局变量污染
- [ ] 敏感信息不暴露在前端
- [ ] API命名一致
- [ ] CSS使用变量，无硬编码
- [ ] 错误处理完善
- [ ] 性能考虑（懒加载、按需引入）

### 7.2 禁止项
- ❌ 直接 `document.querySelector`
- ❌ `window.xxx` 全局变量
- ❌ `innerHTML` 直接赋值
- ❌ 组件超过300行
- ❌ 复制粘贴相同代码
- ❌ 敏感信息硬编码
- ❌ 循环依赖

---

## 八、CI/CD规范

### 8.1 构建检查
```bash
# 构建前必须通过
npm run lint        # ESLint检查
npm run type-check  # TypeScript类型检查
npm run test        # 单元测试
npm run build       # 构建
```

### 8.2 性能预算
- 首屏JS包 < 200KB (gzipped)
- 单个chunk < 500KB
- 图片优化后 < 100KB

---

## 九、文档规范

### 9.1 组件文档
```vue
/**
 * ArticleCard - 文章卡片组件
 * @description 显示文章摘要信息，支持点击跳转详情页
 * @prop {Object} article - 文章对象
 * @prop {Boolean} compact - 紧凑模式
 * @emits {Function} click - 点击事件
 * @example
 * <ArticleCard :article="article" @click="goDetail" />
 */
```

### 9.2 API文档
```javascript
/**
 * getArticleDetail - 获取文章详情
 * @param {number} id - 文章ID
 * @param {Object} options - 配置选项
 * @param {Boolean} options.includeContent - 是否包含内容
 * @returns {Promise<Object>} 文章详情
 * @throws {Error} 文章不存在
 * @example
 * const article = await getArticleDetail(1, { includeContent: true })
 */
```

---

## 十、监控与日志

### 10.1 前端监控
- 错误上报：window.onerror, Promise.reject
- 性能监控：LCP, FCP, CLS
- 业务埋点：关键用户行为

### 10.2 日志规范
```javascript
// ✅ 正确：使用统一的logger
import logger from '@/utils/logger'
logger.info('User logged in', { userId: 123 })
logger.error('API failed', { error: err.message })

// ❌ 错误：console.log直接输出
console.log('User logged in')
console.error(err)
```
