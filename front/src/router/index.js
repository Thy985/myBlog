import { createRouter, createWebHistory } from 'vue-router'

// 路由懒加载
const AdminIndex = () => import('@/views/admin/index.vue')
const About = () => import('@/views/about.vue')
const NotFound = () => import('@/views/404.vue')
const AdminLogin = () => import('@/views/admin/login.vue')
const Admin = () => import('@/layouts/admin.vue')
const AdminArticleList = () => import('@/views/admin/article-list.vue')
const AdminCategoryList = () => import('@/views/admin/category-list.vue')
const AdminTagList = () => import('@/views/admin/tag-list.vue')
const AdminBlogSetting = () => import('@/views/admin/blog-setting.vue')
const AdminComments = () => import('@/views/admin/comments.vue')
const AdminUsers = () => import('@/views/admin/users.vue')
const AdminMedia = () => import('@/views/admin/media.vue')
const AdminPageViews = () => import('@/views/admin/analytics/page-views.vue')
const Index = () => import('@/views/frontend/index.vue')
const ArticleDetail = () => import('@/views/frontend/article-detail.vue')
const CategoryList = () => import('@/views/frontend/category-list.vue')
const TagList = () => import('@/views/frontend/tag-list.vue')
const CategoryArticleList = () => import('@/views/frontend/category-article-list.vue')
const TagArticleList = () => import('@/views/frontend/tag-article-list.vue')
const ArchiveList = () => import('@/views/frontend/archive-list.vue')
const Search = () => import('@/views/frontend/search.vue')
const UserCenter = () => import('@/views/frontend/user-center.vue')
const UserSettings = () => import('@/views/frontend/user-settings.vue')
const UserArticleCreate = () => import('@/views/frontend/user-article-create.vue')
const UserArticleList = () => import('@/views/frontend/user-article-list.vue')
const UserComments = () => import('@/views/frontend/user-comments.vue')
const UserCollections = () => import('@/views/frontend/user-collections.vue')
const UserCategories = () => import('@/views/frontend/user-categories.vue')
const UserTags = () => import('@/views/frontend/user-tags.vue')
const UserMedia = () => import('@/views/frontend/user-media.vue')
const UserProfileSettings = () => import('@/views/frontend/user-profile-settings.vue')
const UserAccountSettings = () => import('@/views/frontend/user-account-settings.vue')
const UserLogin = () => import('@/views/frontend/Login.vue')
const UserRegister = () => import('@/views/frontend/Register.vue')
const UserForgotPassword = () => import('@/views/frontend/ForgotPassword.vue')
const UserVerifyMfa = () => import('@/views/frontend/VerifyMfa.vue')

const routes = [
  {
    // 指定访问路径
    path: '/admin',
    component: Admin,
    meta: { requiresAuth: true, role: 'admin' },
    // 使用到 admin.vue 布局的，都需要放置在其子路由下面
    children: [{
      path: 'dashboard',
      component: AdminIndex,
      meta: {
        title: '仪表盘'
      }
    }, {
      path: 'articles',
      component: AdminArticleList,
      meta: {
        title: '文章管理'
      }
    }, {
      path: 'articles/create',
      component: AdminArticleList,
      meta: {
        title: '发布文章'
      }
    }, {
      path: 'categories',
      component: AdminCategoryList,
      meta: {
        title: '分类管理'
      }
    }, {
      path: 'categories/create',
      component: AdminCategoryList,
      meta: {
        title: '添加分类'
      }
    }, {
      path: 'tags',
      component: AdminTagList,
      meta: {
        title: '标签管理'
      }
    }, {
      path: 'tags/create',
      component: AdminTagList,
      meta: {
        title: '添加标签'
      }
    }, {
      path: 'comments',
      component: AdminComments,
      meta: {
        title: '评论管理'
      }
    }, {
      path: 'users',
      component: AdminUsers,
      meta: {
        title: '用户管理'
      }
    }, {
      path: 'settings/site',
      component: AdminBlogSetting,
      meta: {
        title: '站点设置'
      }
    }, {
      path: 'settings/plugins',
      component: AdminBlogSetting,
      meta: {
        title: '插件管理'
      }
    }, {
      path: 'settings/backup',
      component: AdminBlogSetting,
      meta: {
        title: '数据备份'
      }
    }, {
      path: 'settings/logs',
      component: AdminBlogSetting,
      meta: {
        title: '日志管理'
      }
    }, {
      path: 'analytics/page-views',
      component: AdminPageViews,
      meta: {
        title: '访问统计'
      }
    }, {
      path: 'media',
      component: AdminMedia,
      meta: {
        title: '文件列表'
      }
    }, {
      path: 'media/upload',
      component: AdminMedia,
      meta: {
        title: '上传文件'
      }
    }]

  },
  {
    path: '/about',
    component: About
  },
  {
    path: '/login',
    component: UserLogin,
    meta: {
      title: '登录页'
    }
  },
  {
    path: '/register',
    component: UserRegister,
    meta: {
      title: '注册页'
    }
  },
  {
    path: '/forgot-password',
    component: UserForgotPassword,
    meta: {
      title: '忘记密码页'
    }
  },
  {
    path: '/verify-mfa',
    component: UserVerifyMfa,
    meta: {
      title: 'MFA验证'
    }
  },
  {
    path: '/admin/login',
    component: AdminLogin,
    meta: {
      title: '后台登录页'
    }
  },
  {
    path: '/',
    component: Index,
    meta: {
      title: 'XingChen博客'
    }
  },
  {
    path: '/article/:id(\\d+)',
    name: 'article',
    component: ArticleDetail,
    meta: {
      title: '文章详情页'
    }
  },
  {
    path: '/category',
    component: CategoryList,
    meta: {
      title: '分类'
    }
  },
  // RESTful 分类文章列表: /category/:id/:name?
  {
    path: '/category/:id(\\d+)/:name?',
    name: 'category-articles',
    component: CategoryArticleList,
    meta: {
      title: '分类文章列表'
    }
  },
  {
    path: '/tag',
    component: TagList,
    meta: {
      title: '标签'
    }
  },
  // RESTful 标签文章列表: /tag/:id/:name?
  {
    path: '/tag/:id(\\d+)/:name?',
    name: 'tag-articles',
    component: TagArticleList,
    meta: {
      title: '标签文章列表'
    }
  },
  {
    path: '/archive',
    component: ArchiveList,
    meta: {
      title: '归档'
    }
  },
  {
    path: '/search',
    component: Search,
    meta: {
      title: '搜索结果'
    }
  },
  {
    path: '/comment-test',
    component: () => import('@/views/frontend/comment-test.vue'),
    meta: {
      title: '评论系统测试'
    }
  },
  {
    path: '/user',
    component: UserCenter,
    meta: {
      title: '个人中心',
      requiresAuth: true
    }
  },
  {
    path: '/user/articles/create',
    component: UserArticleCreate,
    meta: {
      title: '发布文章',
      requiresAuth: true
    }
  },
  {
    path: '/user/articles',
    component: UserArticleList,
    meta: {
      title: '我的文章',
      requiresAuth: true
    }
  },
  {
    path: '/user/comments',
    component: UserComments,
    meta: {
      title: '我的评论',
      requiresAuth: true
    }
  },
  {
    path: '/user/collections',
    component: UserCollections,
    meta: {
      title: '我的收藏',
      requiresAuth: true
    }
  },
  {
    path: '/user/categories',
    component: UserCategories,
    meta: {
      title: '我的分类',
      requiresAuth: true
    }
  },
  {
    path: '/user/tags',
    component: UserTags,
    meta: {
      title: '我的标签',
      requiresAuth: true
    }
  },
  {
    path: '/user/media',
    component: UserMedia,
    meta: {
      title: '我的媒体库',
      requiresAuth: true
    }
  },
  {
    path: '/user/settings/profile',
    component: UserProfileSettings,
    meta: {
      title: '个人资料',
      requiresAuth: true
    }
  },
  {
    path: '/user/settings/account',
    component: UserAccountSettings,
    meta: {
      title: '账户设置',
      requiresAuth: true
    }
  },
  {
    path: '/user/settings',
    component: UserSettings,
    meta: {
      title: '个人设置',
      requiresAuth: true
    }
  },
  // 将匹配所有内容并将其放在 `$route.params.pathMatch` 下
  // 通配符路由必须放在所有具体路由的最后
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound
  }
]

const router = createRouter({
  // 使用 HTML5 History 模式，移除 URL 中的 #
  history: createWebHistory(),
  routes,

  // 滚动行为配置
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    if (to.path !== from.path) {
      return { top: 0, behavior: 'smooth' }
    }
    return false
  },

  // 路由切换时的过渡行为
  parseQuery: (query) => query,
  stringifyQuery: (query) => new URLSearchParams(query).toString()
})

// ES6 模块导出语句，它用于将 router 对象导出，以便其他文件可以导入和使用这个对象
export default router
