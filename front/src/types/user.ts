/**
 * 用户相关类型定义
 */

// 用户角色
export type UserRole = 'user' | 'admin' | 'guest'

// 用户状态
export type UserStatus = 'active' | 'inactive' | 'banned' | string

// 用户性别
export type Gender = 'male' | 'female' | 'other' | string

// 用户基础信息
export interface User {
  id: number
  username: string
  nickname?: string
  email: string
  avatar?: string
  phone?: string
  role: UserRole
  status: UserStatus
  gender?: Gender
  createTime: string
  updateTime?: string
  lastLoginTime?: string
  // 社交链接
  github?: string
  gitee?: string
  csdn?: string
  zhihu?: string
}

// 用户登录表单
export interface LoginForm {
  username: string
  password: string
  rememberMe?: boolean
  captcha?: string
}

// 用户注册表单
export interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
  captcha: string
  agreeTos: boolean
}

// 用户个人资料
export interface UserProfile {
  id: number
  username: string
  nickname?: string
  email: string
  avatar?: string
  phone?: string
  bio?: string
  website?: string
  location?: string
  birthday?: string
  gender?: 'male' | 'female' | 'other'
  // 社交链接
  github?: string
  gitee?: string
  csdn?: string
  zhihu?: string
}

// 用户个人资料更新表单
export interface UserProfileForm {
  nickname?: string
  avatar?: string
  phone?: string
  bio?: string
  website?: string
  location?: string
  birthday?: string
  gender?: 'male' | 'female' | 'other'
  // 社交链接
  github?: string
  gitee?: string
  csdn?: string
  zhihu?: string
}

// 用户账户设置
export interface UserAccountSettings {
  email: string
  phone?: string
  twoFactorEnabled?: boolean
  emailNotification?: boolean
  smsNotification?: boolean
}

// 密码修改表单
export interface PasswordChangeForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// 用户统计信息
export interface UserStats {
  articleCount: number
  commentCount: number
  likeCount: number
  collectCount: number
  followerCount: number
  followingCount: number
}
