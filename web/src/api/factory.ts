import { get } from '@/utils/request'
import type { DeviceDetail, FactorySceneData, LineDetail } from '@/three/types'

export function fetchFactoryScene(enterpriseId?: number): Promise<FactorySceneData> {
  return get<FactorySceneData>('/factory/scene', { enterpriseId })
}

export function fetchFactoryDevice(id: number): Promise<DeviceDetail> {
  return get<DeviceDetail>(`/factory/devices/${id}`)
}

export function fetchFactoryLine(id: number): Promise<LineDetail> {
  return get<LineDetail>(`/factory/lines/${id}`)
}
