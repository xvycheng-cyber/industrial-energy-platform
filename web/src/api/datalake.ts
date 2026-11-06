import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AggregateItem,
  CompletenessResult,
  DashboardResult,
  DetailItem,
  FailureItem,
  GapItem,
  ImportResult,
  IngestLogItem,
  IngestResult,
  ProfileResult,
  QualitySummary,
  RawDataItem
} from '@/types/datalake'

export interface PageParams {
  pageNum: number
  pageSize: number
  [key: string]: unknown
}

export interface ReadingPayload {
  meterId: number
  collectTime: string
  value: number
  readingType?: number
  pointCode?: string
}

export function ingestBatch(payload: {
  batchNo: string
  source: string
  readings: ReadingPayload[]
}): Promise<IngestResult> {
  return post<IngestResult>('/datalake/ingest', payload)
}

export function importFile(file: File): Promise<ImportResult> {
  const form = new FormData()
  form.append('file', file)
  return post<ImportResult>('/datalake/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function simulate(payload: { start?: string; end?: string; stepMinutes?: number }): Promise<number> {
  return post<number>('/datalake/simulate', payload)
}

export function pageRaw(params: PageParams): Promise<PageResult<RawDataItem>> {
  return get<PageResult<RawDataItem>>('/datalake/raw', params)
}

export function pageDetails(params: PageParams): Promise<PageResult<DetailItem>> {
  return get<PageResult<DetailItem>>('/datalake/details', params)
}

export function pageLogs(params: PageParams): Promise<PageResult<IngestLogItem>> {
  return get<PageResult<IngestLogItem>>('/datalake/logs', params)
}

export function pageFailures(params: PageParams): Promise<PageResult<FailureItem>> {
  return get<PageResult<FailureItem>>('/datalake/failures', params)
}

export function pageAggregates(params: PageParams): Promise<PageResult<AggregateItem>> {
  return get<PageResult<AggregateItem>>('/datalake/aggregates', params)
}

export function cleanData(payload: { batchNo?: string; date?: string; limit?: number }): Promise<number> {
  return post<number>('/datalake/clean', payload)
}

export function aggregateDay(date: string, periods?: string): Promise<number> {
  const query = periods ? `?date=${date}&periods=${periods}` : `?date=${date}`
  return post<number>(`/datalake/aggregate${query}`)
}

export function completeness(params: {
  date?: string
  enterpriseId?: number
  deviceId?: number
  meterId?: number
}): Promise<CompletenessResult> {
  return get<CompletenessResult>('/datalake/completeness', params)
}

export function qualitySummary(params: { date?: string; enterpriseId?: number }): Promise<QualitySummary> {
  return get<QualitySummary>('/datalake/quality/summary', params)
}

export function pageGaps(params: PageParams): Promise<PageResult<GapItem>> {
  return get<PageResult<GapItem>>('/datalake/gaps', params)
}

export function detectGaps(date: string): Promise<number> {
  return post<number>(`/datalake/gaps/detect?date=${date}`)
}

export function backfillGap(gapId: number): Promise<number> {
  return post<number>('/datalake/gaps/backfill', { gapId })
}

export function dashboard(params: { date?: string; enterpriseId?: number }): Promise<DashboardResult> {
  return get<DashboardResult>('/datalake/ads/dashboard', params)
}

export function profile(params: {
  dimType: number
  dimId?: number
  days?: number
}): Promise<ProfileResult> {
  return get<ProfileResult>('/datalake/ads/profile', params)
}
