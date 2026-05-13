import { createRouter, createWebHistory } from 'vue-router'

// 路由懒加载
const About = () => import('@/views/about.vue')
const NotFound = () => import('@/views/404.vue')
const Index = () => import('@/views/frontend/index.vue')
const Discover = () => import('@/views/frontend/discover.vue')
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
const UserNotificationList = () => import('@/views/frontend/notification-list.vue')

const routes = [
  {
    path: '/user/notifications',
    component: UserNotificationList,
    meta: {
      title: '我的通知',
      requiresAuth: true
    }
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
    path: '/',
    component: Index,
    meta: {
      title: 'XingChen博客'
    }
  },
  {
    path: '/discover',
    component: Discover,
    meta: {
      title: '发现社区'
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
    path: '/user/profile',
    component: UserProfileSettings,
    meta: {
      title: '个人资料',
      requiresAuth: true
    }
  },
  {
    path: '/user/account',
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
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
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
  parseQuery: (query) => query,
  stringifyQuery: (query) => new URLSearchParams(query).toString()
})

export default router
