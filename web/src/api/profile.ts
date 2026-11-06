import { get } from '@/utils/request'
import type {
  CockpitData,
  DeviceProfile,
  EnterpriseProfile,
  HierarchyNode,
  LineProfile
} from '@/types/profile'

export function fetchCockpit(params: {
  date?: string
  scopeType?: number
  scopeId?: number
  energyTypeId?: number
  granularity?: string
}): Promise<CockpitData> {
  return get<CockpitData>('/profile/cockpit', params)
}

export function fetchEnterpriseProfile(
  id: number,
  params?: { days?: number; energyTypeId?: number }
): Promise<EnterpriseProfile> {
  return get<EnterpriseProfile>(`/profile/enterprise/${id}`, params)
}

export function fetchLineProfile(id: number, params?: { days?: number }): Promise<LineProfile> {
  return get<LineProfile>(`/profile/line/${id}`, params)
}

export function fetchDeviceProfile(id: number, params?: { days?: number }): Promise<DeviceProfile> {
  return get<DeviceProfile>(`/profile/device/${id}`, params)
}

export function fetchHierarchy(enterpriseId?: number): Promise<HierarchyNode[]> {
  return get<HierarchyNode[]>('/profile/hierarchy', { enterpriseId })
}
