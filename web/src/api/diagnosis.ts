import { get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AnomalyItem,
  DiagnosisReport,
  DiagnosisRule,
  OverviewData,
  ScanResult
} from '@/types/diagnosis'

export function runScan(payload?: {
  enterpriseId?: number
  start?: string
  end?: string
}): Promise<ScanResult> {
  return post<ScanResult>('/diagnosis/scan', payload ?? {})
}

export function pageAnomalies(params: {
  pageNum: number
  pageSize: number
  ruleCode?: string
  severity?: number
  status?: number
  deviceId?: number
  startDate?: string
  endDate?: string
}): Promise<PageResult<AnomalyItem>> {
  return get<PageResult<AnomalyItem>>('/diagnosis/anomalies', params)
}

export function fetchAnomaly(id: number): Promise<AnomalyItem> {
  return get<AnomalyItem>(`/diagnosis/anomalies/${id}`)
}

export function handleAnomaly(id: number, status: number, remark?: string): Promise<void> {
  return post<void>(`/diagnosis/anomalies/${id}/handle`, { status, remark })
}

export function fetchReportByAnomaly(anomalyId: number): Promise<DiagnosisReport> {
  return get<DiagnosisReport>(`/diagnosis/anomalies/${anomalyId}/report`)
}

export function fetchReport(id: number): Promise<DiagnosisReport> {
  return get<DiagnosisReport>(`/diagnosis/reports/${id}`)
}

export function confirmReport(id: number, confirmedCause: string): Promise<void> {
  return post<void>(`/diagnosis/reports/${id}/confirm`, { confirmedCause })
}

export function fetchOverview(params?: {
  enterpriseId?: number
  startDate?: string
  endDate?: string
}): Promise<OverviewData> {
  return get<OverviewData>('/diagnosis/overview', params)
}

export function listRules(): Promise<DiagnosisRule[]> {
  return get<DiagnosisRule[]>('/diagnosis/rules')
}

export function updateRule(
  id: number,
  payload: {
    condition?: Record<string, unknown>
    threshold?: Record<string, unknown>
    priority?: number
    weight?: number
    enabled?: boolean
    remark?: string
  }
): Promise<DiagnosisRule> {
  return put<DiagnosisRule>(`/diagnosis/rules/${id}`, payload)
}

export function toggleRule(id: number, enabled: boolean): Promise<void> {
  return post<void>(`/diagnosis/rules/${id}/toggle?enabled=${enabled}`)
}
