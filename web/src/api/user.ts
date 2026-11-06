import { del, get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { UserItem } from '@/types/models'

export interface UserQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number | ''
  enterpriseId?: number | ''
}

export interface UserPayload {
  username?: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  enterpriseId?: number | null
  status?: number
  roleIds?: number[]
  remark?: string
}

export function pageUsers(params: UserQuery): Promise<PageResult<UserItem>> {
  return get<PageResult<UserItem>>('/system/users', params as unknown as Record<string, unknown>)
}

export function createUser(payload: UserPayload): Promise<number> {
  return post<number>('/system/users', payload)
}

export function updateUser(id: number, payload: UserPayload): Promise<void> {
  return put<void>(`/system/users/${id}`, payload)
}

export function deleteUser(id: number): Promise<void> {
  return del<void>(`/system/users/${id}`)
}

export function updateUserStatus(id: number, status: number): Promise<void> {
  return put<void>(`/system/users/${id}/status`, { status })
}

export function resetUserPassword(id: number, password: string): Promise<void> {
  return put<void>(`/system/users/${id}/password`, { password })
}
