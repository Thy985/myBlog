/* eslint-disable */
// TypeScript 类型定义
// 为将来的 TypeScript 迁移做准备

// 用户信息类型
export interface User {
  id: number;
  username: string;
  email: string;
  avatar?: string;
  createdAt: string;
  updatedAt: string;
}

// 博客设置类型
export interface BlogSetting {
  id: number;
  blogName: string;
  blogDescription: string;
  blogAvatar?: string;
  blogBanner?: string;
  createdAt: string;
  updatedAt: string;
}

// 文章类型
export interface Article {
  id: number;
  title: string;
  content: string;
  summary?: string;
  coverImage?: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  categoryId: number;
  categoryName: string;
  tags: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
}