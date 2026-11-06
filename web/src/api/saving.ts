import service, { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { SavingResult, SavingSummary } from '@/types/saving'

export function calculateSaving(taskId: number, payload?: {
  observationDays?: number
  basePrice?: number
  startTime?: string
}): Promise<SavingResult> {
  return post<SavingResult>(`/saving/tasks/${taskId}/calculate`, payload ?? {})
}

export function pageSavingResults(params: {
  pageNum: number
  pageSize: number
  taskId?: number
  deviceId?: number
  status?: number
  conclusionCode?: number
  includeArchived?: boolean
}): Promise<PageResult<SavingResult>> {
  return get<PageResult<SavingResult>>('/saving/results', params)
}

export function fetchSavingResult(id: number): Promise<SavingResult> {
  return get<SavingResult>(`/saving/results/${id}`)
}

export function confirmSavingResult(id: number, pass: boolean, remark?: string): Promise<void> {
  return post<void>(`/saving/results/${id}/confirm?pass=${pass}&remark=${encodeURIComponent(remark ?? '')}`)
}

export function fetchSavingSummary(): Promise<SavingSummary> {
  return get<SavingSummary>('/saving/summary')
}

export async function exportSavingCsv(params: Record<string, unknown>): Promise<void> {
  const response = await service.get('/saving/export', { params, responseType: 'blob' })
  const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `saving-results-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
