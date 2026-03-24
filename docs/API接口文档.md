# Myblog 接口文档

> 版本: 3.0.0  
> 更新日期: 2026-02-16  
> 基础URL: `/api`

---

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 响应Code说明
| Code | 说明 |
|------|------|
| 200 | 成功 |
| 401 | 未登录或登录过期 |
| 403 | 无权限 |
| 429 | 请求过于频繁（限流） |
| 500 | 服务器错误 |

---

## 认证接口 `/auth`

### 1. 用户登录
- **URL**: `POST /auth/login`
- **描述**: 用户登录接口，限流 5次/5分钟
- **请求体**:
```json
{
  "username": "admin",
  "password": "147258369Thy@"
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "xxx",
    "userId": 1,
    "username": "admin",
    "nickname": "星辰远征客",
    "avatar": "https://...",
    "roles": ["ADMIN"],
    "isAdmin": true
  }
}
```

### 2. 用户登出
- **URL**: `POST /auth/logout`
- **认证**: 需要登录
- **响应**: `Result<Boolean>`

### 3. 刷新Token
- **URL**: `POST /auth/refresh`
- **认证**: 需要登录
- **响应**: 返回新的Token信息

### 4. 获取用户信息
- **URL**: `GET /auth/info`
- **认证**: 需要登录
- **响应**: 返回用户信息和角色

### 5. 登录历史
- **URL**: `GET /auth/login-history`
- **认证**: 需要登录
- **参数**: `limit` (默认20)
- **响应**: 返回登录历史列表

---

## 用户接口 `/user`

### 1. 用户注册
- **URL**: `POST /user/register`
- **请求体**:
```json
{
  "username": "test",
  "password": "Test1234@",
  "email": "test@example.com"
}
```

### 2. 获取个人资料
- **URL**: `GET /user/profile`
- **认证**: 需要登录
- **响应**: 返回用户详细信息

### 3. 更新个人资料
- **URL**: `PUT /user/profile`
- **认证**: 需要登录
- **请求体**:
```json
{
  "nickname": "新昵称",
  "bio": "个人简介",
  "website": "https://...",
  "github": "https://github.com/xxx",
  "location": "北京",
  "gender": 1,
  "birthday": "2000-01-01"
}
```

### 4. 修改密码
- **URL**: `PUT /user/password`
- **认证**: 需要登录
- **请求体**:
```json
{
  "oldPassword": "旧密码",
  "newPassword": "新密码"
}
```

### 5. 上传头像
- **URL**: `POST /user/avatar`
- **认证**: 需要登录
- **Content-Type**: `multipart/form-data`
- **参数**: `file` (图片文件)

### 6. 获取用户角色
- **URL**: `GET /user/roles`
- **认证**: 需要登录
- **响应**: 返回用户角色列表

---

## 密码重置接口 `/password`

### 1. 发送重置验证码
- **URL**: `POST /password/reset-code`
- **限流**: 3次/分钟
- **请求体**:
```json
{
  "email": "1850833838@qq.com"
}
```

### 2. 验证验证码
- **URL**: `POST /password/verify-code`
- **参数**: 
  - `email`: 邮箱
  - `code`: 验证码

### 3. 重置密码
- **URL**: `POST /password/reset`
- **请求体**:
```json
{
  "email": "1850833838@qq.com",
  "code": "123456",
  "newPassword": "新密码"
}
```

---

## MFA接口 `/mfa`

### 1. 设置MFA
- **URL**: `POST /mfa/setup`
- **认证**: 需要登录

### 2. 启用MFA
- **URL**: `POST /mfa/verify-and-enable`
- **认证**: 需要登录
- **请求体**:
```json
{
  "code": "123456"
}
```

### 3. 禁用MFA
- **URL**: `POST /mfa/disable`
- **认证**: 需要登录

### 4. 获取MFA状态
- **URL**: `GET /mfa/status`
- **认证**: 需要登录

---

## 文章接口 `/article`

### 1. 创建文章
- **URL**: `POST /article`
- **认证**: 需要登录
- **请求体**:
```json
{
  "title": "文章标题",
  "content": "文章内容(Markdown)",
  "description": "文章摘要",
  "titleImage": "封面图URL",
  "categoryId": 1,
  "tagIds": [1, 2],
  "status": 0,
  "commentStatus": 1,
  "viewStatus": 1
}
```

### 2. 更新文章
- **URL**: `PUT /article/{id}`
- **认证**: 需要登录（作者本人）
- **请求体**: 同创建

### 3. 删除文章
- **URL**: `DELETE /article/{id}`
- **认证**: 需要登录

### 4. 获取文章详情
- **URL**: `GET /article/{id}`
- **公开接口**
- **响应**: 返回文章详情（含内容）

### 5. 文章列表
- **URL**: `GET /article/list`
- **公开接口**
- **参数**:
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 搜索关键词
  - `categoryId`: 分类ID
  - `tagId`: 标签ID

### 6. 获取用户文章
- **URL**: `GET /article/user/{userId}`
- **参数**: `page`, `size`

### 7. 热门文章
- **URL**: `GET /article/hot`
- **公开接口**
- **参数**: `limit` (默认10)

### 8. 发布文章
- **URL**: `POST /article/{id}/publish`
- **认证**: 需要登录

### 9. 下线文章
- **URL**: `POST /article/{id}/offline`
- **认证**: 需要登录

### 10. 置顶文章
- **URL**: `POST /article/{id}/top`
- **认证**: 需要登录
- **参数**: `isTop` (true/false)

### 11. 点赞文章
- **URL**: `POST /article/{id}/like`
- **认证**: 需要登录

### 12. 取消点赞
- **URL**: `DELETE /article/{id}/like`
- **认证**: 需要登录

### 13. 收藏文章
- **URL**: `POST /article/{id}/collect`
- **认证**: 需要登录

### 14. 取消收藏
- **URL**: `DELETE /article/{id}/collect`
- **认证**: 需要登录

### 15. 我的收藏
- **URL**: `GET /article/user/collects`
- **认证**: 需要登录
- **参数**: `page`, `size`

### 16. 文章归档
- **URL**: `GET /article/archive`
- **公开接口**
- **参数**: `page`, `size`

---

## 评论接口 `/comment`

### 1. 获取文章评论
- **URL**: `GET /comment/list`
- **公开接口**
- **参数**: `articleId`

### 2. 获取评论回复
- **URL**: `GET /comment/{rootId}/replies`
- **公开接口**

### 3. 创建评论
- **URL**: `POST /comment`
- **认证**: 需要登录
- **请求体**:
```json
{
  "articleId": 1,
  "content": "评论内容",
  "parentId": 0,
  "replyToUserId": null
}
```

### 4. 删除评论
- **URL**: `DELETE /comment/{id}`
- **认证**: 需要登录

### 5. 点赞评论
- **URL**: `POST /comment/{id}/like`
- **认证**: 需要登录

### 6. 取消点赞评论
- **URL**: `DELETE /comment/{id}/like`
- **认证**: 需要登录

### 7. 待审核评论（管理员）
- **URL**: `GET /comment/pending`
- **认证**: 需要管理员

### 8. 审核通过评论（管理员）
- **URL**: `PUT /comment/{id}/approve`
- **认证**: 需要管理员

### 9. 拒绝评论（管理员）
- **URL**: `PUT /comment/{id}/reject`
- **认证**: 需要管理员

---

## 分类接口 `/category`

### 1. 分类列表
- **URL**: `GET /category/list`
- **公开接口**

### 2. 获取分类详情
- **URL**: `GET /category/{id}`
- **公开接口**

### 3. 创建分类（管理员）
- **URL**: `POST /category`
- **认证**: 需要登录

### 4. 更新分类（管理员）
- **URL**: `PUT /category/{id}`
- **认证**: 需要登录

### 5. 删除分类（管理员）
- **URL**: `DELETE /category/{id}`
- **认证**: 需要登录

---

## 标签接口 `/tag`

### 1. 标签列表
- **URL**: `GET /tag/list`
- **公开接口**

### 2. 热门标签
- **URL**: `GET /tag/hot`
- **公开接口**
- **参数**: `limit` (默认20)

### 3. 获取标签详情
- **URL**: `GET /tag/{id}`
- **公开接口**

### 4. 创建标签（管理员）
- **URL**: `POST /tag`
- **认证**: 需要登录
- **参数**: `name`, `color`

### 5. 删除标签（管理员）
- **URL**: `DELETE /tag/{id}`
- **认证**: 需要登录

---

## 通知接口 `/notification`

### 1. 通知列表
- **URL**: `GET /notification/list`
- **认证**: 需要登录
- **参数**: `page`, `size`

### 2. 未读数量
- **URL**: `GET /notification/unread-count`
- **认证**: 需要登录

### 3. 标记已读
- **URL**: `PUT /notification/{id}/read`
- **认证**: 需要登录

### 4. 全部已读
- **URL**: `PUT /notification/read-all`
- **认证**: 需要登录

### 5. 删除通知
- **URL**: `DELETE /notification/{id}`
- **认证**: 需要登录

---

## 文件接口 `/file`

### 1. 上传文件
- **URL**: `POST /file/upload`
- **认证**: 需要登录
- **Content-Type**: `multipart/form-data`
- **参数**: `file`, `categoryId`

### 2. 上传图片
- **URL**: `POST /file/upload/image`
- **认证**: 需要登录

### 3. 删除文件
- **URL**: `DELETE /file/{id}`
- **认证**: 需要登录

### 4. 文件列表
- **URL**: `GET /file/list`
- **认证**: 需要登录
- **参数**: `categoryId`, `fileType`, `page`, `size`

### 5. 图片列表
- **URL**: `GET /file/images`
- **认证**: 需要登录
- **参数**: `page`, `size`

### 6. 获取文件URL
- **URL**: `GET /file/{id}/url`

### 7. 记录下载
- **URL**: `POST /file/{id}/download`

### 8. 创建分类
- **URL**: `POST /file/category`
- **参数**: `name`, `description`

### 9. 删除分类
- **URL**: `DELETE /file/category/{id}`

### 10. 分类列表
- **URL**: `GET /file/category/list`

---

## 搜索接口 `/search`

### 1. 搜索文章
- **URL**: `GET /search/articles`
- **公开接口**
- **参数**: `keyword`, `page`, `size`

### 2. 全局搜索
- **URL**: `GET /search/global`
- **公开接口**
- **参数**: `keyword`, `type`, `page`, `size`

### 3. 搜索建议
- **URL**: `GET /search/suggestions`
- **公开接口**
- **参数**: `keyword`, `limit`

### 4. 热门搜索词
- **URL**: `GET /search/hot-keywords`
- **公开接口**
- **参数**: `limit`

---

## 数据分析接口 `/analytics`

### 1. 记录页面访问
- **URL**: `POST /analytics/pv`
- **限流**: 120次/分钟
- **参数**: `pageUrl`

### 2. 数据概览
- **URL**: `GET /analytics/overview`
- **认证**: 需要管理员

### 3. 趋势数据
- **URL**: `GET /analytics/trend`
- **认证**: 需要管理员
- **参数**: `startDate`, `endDate` (格式: YYYY-MM-DD)

### 4. 设备统计
- **URL**: `GET /analytics/device`
- **认证**: 需要管理员

### 5. 来源统计
- **URL**: `GET /analytics/source`
- **认证**: 需要管理员

### 6. 热门文章
- **URL**: `GET /analytics/hot-articles`
- **认证**: 需要管理员
- **参数**: `limit`

### 7. 实时数据
- **URL**: `GET /analytics/realtime`
- **认证**: 需要管理员

---

## 博客设置接口 `/blog/setting`

### 1. 获取博客详情
- **URL**: `GET /blog/setting/detail`
- **公开接口**
- **响应**:
```json
{
  "name": "博客名称",
  "author": "作者",
  "introduction": "简介",
  "avatar": "头像URL",
  "logo": "Logo URL",
  "github": "GitHub主页",
  "gitee": "Gitee主页",
  "csdn": "CSDN主页",
  "zhihu": "知乎主页",
  "footerInfo": "页脚信息",
  "beianCode": "备案号",
  "seoTitle": "SEO标题",
  "seoKeywords": "SEO关键词",
  "seoDescription": "SEO描述"
}
```

---

## 管理接口 `/admin`

> 需要 ADMIN 角色

### 1. 仪表盘数据
- **URL**: `GET /admin/dashboard`
- **认证**: 需要管理员

### 2. 用户列表
- **URL**: `GET /admin/users`
- **认证**: 需要管理员
- **参数**: `page`, `size`, `keyword`, `status`

### 3. 创建用户
- **URL**: `POST /admin/users`
- **认证**: 需要管理员

### 4. 修改用户状态
- **URL**: `PUT /admin/users/{id}/status`
- **认证**: 需要管理员
- **参数**: `status`

### 5. 删除用户
- **URL**: `DELETE /admin/users/{id}`
- **认证**: 需要管理员

### 6. 分配角色
- **URL**: `PUT /admin/users/{id}/roles`
- **认证**: 需要管理员
- **请求体**: `[1, 2]`

### 7. 获取设置
- **URL**: `GET /admin/settings`
- **认证**: 需要管理员

### 8. 更新设置
- **URL**: `PUT /admin/settings/{key}`
- **认证**: 需要管理员
- **参数**: `value`

---

## 健康检查接口 `/health`

### 1. 健康检查
- **URL**: `GET /health`
- **公开接口**

---

## 角色说明

| 角色Code | 说明 |
|----------|------|
| SUPER_ADMIN | 超级管理员 |
| ADMIN | 管理员 |
| USER | 普通用户 |
| GUEST | 访客 |

---

## 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 147258369Thy@ | 管理员 |
| test | test1234@ | 普通用户 |
