import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  BaselineErrorAnalysis,
  BaselineModel,
  BaselineRecord,
  BaselineTrainResult
} from '@/types/baseline'

export interface BaselineTrainPayload {
  dimType: number
  dimId: number
  energyTypeId: number
  startDate: string
  endDate: string
  activate: boolean
  remark?: string
}

export function trainBaseline(payload: BaselineTrainPayload): Promise<BaselineTrainResult> {
  return post<BaselineTrainResult>('/baseline/models/train', payload)
}

export function predictBaseline(payload: {
  baselineId?: number
  dimType: number
  dimId: number
  energyTypeId: number
  startDate: string
  endDate: string
}): Promise<number> {
  return post<number>('/baseline/predict', payload)
}

export function pageBaselineModels(params: {
  pageNum: number
  pageSize: number
  dimType?: number
  dimId?: number
  energyTypeId?: number
  status?: number
}): Promise<PageResult<BaselineModel>> {
  return get<PageResult<BaselineModel>>('/baseline/models', params)
}

export function fetchBaselineModel(id: number): Promise<BaselineModel> {
  return get<BaselineModel>(`/baseline/models/${id}`)
}

export function pageBaselineRecords(params: {
  pageNum: number
  pageSize: number
  baselineId?: number
  dimType?: number
  dimId?: number
  startDate?: string
  endDate?: string
  onlyDeviation?: boolean
}): Promise<PageResult<BaselineRecord>> {
  return get<PageResult<BaselineRecord>>('/baseline/records', params)
}

export function fetchBaselineError(id: number): Promise<BaselineErrorAnalysis> {
  return get<BaselineErrorAnalysis>(`/baseline/models/${id}/error`)
}

export function enableBaseline(id: number): Promise<void> {
  return post<void>(`/baseline/models/${id}/enable`)
}

export function disableBaseline(id: number): Promise<void> {
  return post<void>(`/baseline/models/${id}/disable`)
}
