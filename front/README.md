# Myblog 前端项目

基于 Vue 3 + Vite 的现代化个人博客系统前端。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite 4.x
- **语言**: TypeScript
- **UI 框架**: Element Plus
- **样式**: Tailwind CSS
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **代码规范**: ESLint + Prettier

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── composables/      # 组合式函数
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # 状态管理
│   ├── styles/           # 全局样式
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 公共资源
├── docs/                 # 前端文档
├── .env.example          # 环境变量模板
├── package.json          # 依赖配置
├── vite.config.js        # Vite 配置
└── tsconfig.json         # TypeScript 配置
```

## 快速开始

### 环境要求

- Node.js 16+
- npm 8+ 或 pnpm 7+

### 安装依赖

```bash
# 使用 npm
npm install

# 或使用 pnpm（推荐）
pnpm install
```

### 配置环境变量

1. 复制环境变量模板：
```bash
cp .env.example .env.development
```

2. 编辑 `.env.development`：
```properties
# API 基础地址
VITE_APP_BASE_API='http://localhost:8080/api'

# 应用标题
VITE_APP_TITLE='Myblog'
```

### 开发模式

```bash
# 启动开发服务器
npm run dev

# 或
pnpm dev
```

访问 http://localhost:5173

### 生产构建

```bash
# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 开发指南

### 目录说明

#### API 接口 (`src/api/`)
```typescript
// src/api/article.ts
import request from '@/utils/request'

export const getArticleList = (params) => {
  return request.get('/articles', { params })
}
```

#### 组件开发 (`src/components/`)
```vue
<!-- src/components/ArticleCard.vue -->
<script setup lang="ts">
import { defineProps } from 'vue'

interface Props {
  article: Article
}

const props = defineProps<Props>()
</script>

<template>
  <div class="article-card">
    <h3>{{ article.title }}</h3>
  </div>
</template>
```

#### 状态管理 (`src/stores/`)
```typescript
// src/stores/user.ts
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null
  }),
  actions: {
    async login(credentials) {
      // 登录逻辑
    }
  }
})
```

### 路由配置

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})
```

### 代码规范

#### ESLint
```bash
# 检查代码
npm run lint

# 自动修复
npm run lint:fix
```

#### Prettier
```bash
# 格式化代码
npm run format
```

### 组件库使用

项目使用 Element Plus 作为 UI 组件库：

```vue
<script setup>
import { ElButton, ElMessage } from 'element-plus'

const handleClick = () => {
  ElMessage.success('操作成功')
}
</script>

<template>
  <el-button type="primary" @click="handleClick">
    点击我
  </el-button>
</template>
```

### Tailwind CSS

```vue
<template>
  <div class="flex items-center justify-center p-4 bg-gray-100">
    <h1 class="text-2xl font-bold text-blue-600">
      标题
    </h1>
  </div>
</template>
```

## 测试

```bash
# 运行单元测试
npm run test

# 运行测试并生成覆盖率报告
npm run test:coverage

# 运行 E2E 测试
npm run test:e2e
```

## 构建优化

### 代码分割

Vite 自动进行代码分割，也可以手动配置：

```javascript
// vite.config.js
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia']
        }
      }
    }
  }
}
```

### 性能优化

- 路由懒加载
- 组件懒加载
- 图片懒加载
- 虚拟滚动（长列表）
- 防抖和节流

## 部署

### 静态部署

构建后的 `dist/` 目录可以部署到任何静态服务器：

```bash
# 构建
npm run build

# 部署到 Nginx
cp -r dist/* /var/www/html/
```

### Nginx 配置

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    root /var/www/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Docker 部署

```dockerfile
# Dockerfile
FROM node:16-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 环境变量

### 开发环境 (`.env.development`)
```properties
VITE_APP_BASE_API='http://localhost:8080/api'
VITE_APP_TITLE='Myblog - 开发环境'
```

### 生产环境 (`.env.production`)
```properties
VITE_APP_BASE_API='https://api.yourdomain.com/api'
VITE_APP_TITLE='Myblog'
```

## 常见问题

### 1. 端口被占用
修改 `vite.config.js` 中的端口配置：
```javascript
export default {
  server: {
    port: 3000
  }
}
```

### 2. API 跨域问题
在 `vite.config.js` 中配置代理：
```javascript
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

### 3. 依赖安装失败
```bash
# 清除缓存
npm cache clean --force

# 删除 node_modules 和 lock 文件
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 相关文档

- [Vue 3 文档](https://vuejs.org/)
- [Vite 文档](https://vitejs.dev/)
- [Element Plus 文档](https://element-plus.org/)
- [Tailwind CSS 文档](https://tailwindcss.com/)

## 更新日志

查看 [CHANGELOG.md](../docs/CHANGELOG.md)

## 贡献

请阅读 [贡献指南](../CONTRIBUTING.md)

## 许可证

[指定许可证]
