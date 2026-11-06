import { del, get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Device, EnergyType, Enterprise, MeterItem, ProductionLine, Workshop } from '@/types/models'

export interface MasterQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number | ''
  enterpriseId?: number | ''
  workshopId?: number | ''
  lineId?: number | ''
  deviceType?: string
}

const query = (params: object) => params as unknown as Record<string, unknown>

/* ------------------------------- 企业 ------------------------------- */
export interface EnterprisePayload {
  code: string
  name: string
  industry?: string
  region?: string
  address?: string
  contactName?: string
  contactPhone?: string
  status?: number
  remark?: string
}

export function pageEnterprises(params: MasterQuery): Promise<PageResult<Enterprise>> {
  return get<PageResult<Enterprise>>('/master/enterprises', query(params))
}

export function createEnterprise(payload: EnterprisePayload): Promise<number> {
  return post<number>('/master/enterprises', payload)
}

export function updateEnterprise(id: number, payload: EnterprisePayload): Promise<void> {
  return put<void>(`/master/enterprises/${id}`, payload)
}

export function deleteEnterprise(id: number): Promise<void> {
  return del<void>(`/master/enterprises/${id}`)
}

export function updateEnterpriseStatus(id: number, status: number): Promise<void> {
  return put<void>(`/master/enterprises/${id}/status`, { status })
}

/* ------------------------------- 车间 ------------------------------- */
export interface WorkshopPayload {
  enterpriseId?: number
  code: string
  name: string
  manager?: string
  area?: number
  status?: number
  remark?: string
}

export function pageWorkshops(params: MasterQuery): Promise<PageResult<Workshop>> {
  return get<PageResult<Workshop>>('/master/workshops', query(params))
}

export function createWorkshop(payload: WorkshopPayload): Promise<number> {
  return post<number>('/master/workshops', payload)
}

export function updateWorkshop(id: number, payload: WorkshopPayload): Promise<void> {
  return put<void>(`/master/workshops/${id}`, payload)
}

export function deleteWorkshop(id: number): Promise<void> {
  return del<void>(`/master/workshops/${id}`)
}

export function updateWorkshopStatus(id: number, status: number): Promise<void> {
  return put<void>(`/master/workshops/${id}/status`, { status })
}

/* ------------------------------- 产线 ------------------------------- */
export interface LinePayload {
  enterpriseId?: number
  workshopId: number
  code: string
  name: string
  productType?: string
  designCapacity?: number
  capacityUnit?: string
  status?: number
  remark?: string
}

export function pageLines(params: MasterQuery): Promise<PageResult<ProductionLine>> {
  return get<PageResult<ProductionLine>>('/master/lines', query(params))
}

export function createLine(payload: LinePayload): Promise<number> {
  return post<number>('/master/lines', payload)
}

export function updateLine(id: number, payload: LinePayload): Promise<void> {
  return put<void>(`/master/lines/${id}`, payload)
}

export function deleteLine(id: number): Promise<void> {
  return del<void>(`/master/lines/${id}`)
}

export function updateLineStatus(id: number, status: number): Promise<void> {
  return put<void>(`/master/lines/${id}/status`, { status })
}

/* ------------------------------- 设备 ------------------------------- */
export interface DevicePayload {
  enterpriseId?: number
  lineId: number
  code: string
  name: string
  deviceType: string
  model?: string
  ratedPower?: number
  energyTypeId?: number
  commissionDate?: string
  status?: number
  remark?: string
}

export function pageDevices(params: MasterQuery): Promise<PageResult<Device>> {
  return get<PageResult<Device>>('/master/devices', query(params))
}

export function createDevice(payload: DevicePayload): Promise<number> {
  return post<number>('/master/devices', payload)
}

export function updateDevice(id: number, payload: DevicePayload): Promise<void> {
  return put<void>(`/master/devices/${id}`, payload)
}

export function deleteDevice(id: number): Promise<void> {
  return del<void>(`/master/devices/${id}`)
}

export function updateDeviceStatus(id: number, status: number): Promise<void> {
  return put<void>(`/master/devices/${id}/status`, { status })
}

/* ----------------------------- 能源类型 ----------------------------- */
export function pageEnergyTypes(params: MasterQuery): Promise<PageResult<EnergyType>> {
  return get<PageResult<EnergyType>>('/master/energy-types', query(params))
}

/* ----------------------------- 计量点 ----------------------------- */
export function pageMeters(params: MasterQuery): Promise<PageResult<MeterItem>> {
  return get<PageResult<MeterItem>>('/master/meters', query(params))
}