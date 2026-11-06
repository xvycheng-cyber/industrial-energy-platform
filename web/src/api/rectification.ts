import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  Assignee,
  RectificationStats,
  RectificationTask,
  RectificationTaskDetail
} from '@/types/rectification'

export function pageRectificationTasks(params: {
  pageNum: number
  pageSize: number
  status?: number
  keyword?: string
  assigneeId?: number
  overdue?: boolean
}): Promise<PageResult<RectificationTask>> {
  return get<PageResult<RectificationTask>>('/rectification/tasks', params)
}

export function fetchRectificationTask(id: number): Promise<RectificationTaskDetail> {
  return get<RectificationTaskDetail>(`/rectification/tasks/${id}`)
}

export function createRectificationTask(payload: {
  reportId?: number
  anomalyId?: number
  deviceId?: number
  title?: string
  content?: string
  measure?: string
  expectedSaving?: number
  priority?: number
  assigneeId?: number
  deadline?: string
}): Promise<RectificationTaskDetail> {
  return post<RectificationTaskDetail>('/rectification/tasks', payload)
}

export function assignRectificationTask(id: number, payload: { assigneeId: number; deadline?: string; reason?: string }): Promise<void> {
  return post<void>(`/rectification/tasks/${id}/assign`, payload)
}

export function transferRectificationTask(id: number, payload: { assigneeId: number; deadline?: string; reason?: string }): Promise<void> {
  return post<void>(`/rectification/tasks/${id}/transfer`, payload)
}

export function auditRectificationTask(id: number, payload: { pass: boolean; reason?: string; remark?: string }): Promise<void> {
  return post<void>(`/rectification/tasks/${id}/audit`, payload)
}

export function verifyRectificationTask(id: number, payload: { pass: boolean; actualSaving?: number; reason?: string; remark?: string }): Promise<void> {
  return post<void>(`/rectification/tasks/${id}/verify`, payload)
}

export function cancelRectificationTask(id: number, reason?: string): Promise<void> {
  return post<void>(`/rectification/tasks/${id}/cancel?reason=${encodeURIComponent(reason ?? '')}`)
}

export function listAssignees(): Promise<Assignee[]> {
  return get<Assignee[]>('/rectification/assignees')
}

export function fetchRectificationStats(): Promise<RectificationStats> {
  return get<RectificationStats>('/rectification/stats')
}
