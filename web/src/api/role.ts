import { del, get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { RoleItem } from '@/types/models'

export interface RoleQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number | ''
}

export interface RolePayload {
  roleCode: string
  roleName: string
  description?: string
  permissions: string[]
  sortNo?: number
  status?: number
}

export function pageRoles(params: RoleQuery): Promise<PageResult<RoleItem>> {
  return get<PageResult<RoleItem>>('/system/roles', params as unknown as Record<string, unknown>)
}

export function allRoles(): Promise<RoleItem[]> {
  return get<RoleItem[]>('/system/roles/all')
}

export function createRole(payload: RolePayload): Promise<number> {
  return post<number>('/system/roles', payload)
}

export function updateRole(id: number, payload: RolePayload): Promise<void> {
  return put<void>(`/system/roles/${id}`, payload)
}

export function deleteRole(id: number): Promise<void> {
  return del<void>(`/system/roles/${id}`)
}

export function updateRoleStatus(id: number, status: number): Promise<void> {
  return put<void>(`/system/roles/${id}/status`, { status })
}
