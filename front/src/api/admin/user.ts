import request from '@/axios'
import type { ApiResponse } from '@/types/api'
import type { UserProfile } from '@/types/user'

export interface UserListParams {
  page?: number
  size?: number
  keyword?: string
  status?: string
}

export interface CreateUserData {
  username: string
  password: string
  email: string
  nickname?: string
}

// 管理员登录
export function login(username: string, password: string): Promise<ApiResponse<{ token: string; userId: number }>> {
  return request.post('/auth/login', { username, password })
}

// 获取当前登录管理员信息
export function getAdminInfo(): Promise<ApiResponse<UserProfile>> {
  return request.get('/auth/info')
}

// 更新管理员密码
export function updateAdminPassword(data: { oldPassword: string; newPassword: string }): Promise<ApiResponse<null>> {
  return request.put('/user/password', data)
}

// 获取用户列表（管理员权限）
export function getUserList(params: UserListParams = {}): Promise<ApiResponse<{ list: UserProfile[]; total: number }>> {
  return request.get('/admin/users', { params })
}

// 创建用户（管理员权限）
export function createUser(data: CreateUserData): Promise<ApiResponse<{ id: number }>> {
  return request.post('/admin/users', data)
}

// 更新用户状态（管理员权限）
export function updateUserStatus(id: number, status: string): Promise<ApiResponse<null>> {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

// 删除用户（管理员权限）
export function deleteUser(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/users/${id}`)
}

// 分配用户角色（管理员权限）
export function assignRoles(id: number, roleIds: number[]): Promise<ApiResponse<null>> {
  return request.put(`/admin/users/${id}/roles`, roleIds)
}