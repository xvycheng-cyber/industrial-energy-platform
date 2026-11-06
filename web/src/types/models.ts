export interface Enterprise {
  id: number
  code: string
  name: string
  industry?: string
  region?: string
  address?: string
  contactName?: string
  contactPhone?: string
  status: number
  remark?: string
  createTime?: string
}

export interface Workshop {
  id: number
  enterpriseId: number
  enterpriseName?: string
  code: string
  name: string
  manager?: string
  area?: number
  status: number
  remark?: string
  createTime?: string
}

export interface ProductionLine {
  id: number
  enterpriseId: number
  enterpriseName?: string
  workshopId: number
  workshopName?: string
  code: string
  name: string
  productType?: string
  designCapacity?: number
  capacityUnit?: string
  status: number
  remark?: string
  createTime?: string
}

export interface Device {
  id: number
  enterpriseId: number
  enterpriseName?: string
  workshopId?: number
  workshopName?: string
  lineId: number
  lineName?: string
  code: string
  name: string
  deviceType: string
  model?: string
  ratedPower?: number
  energyTypeId?: number
  energyTypeName?: string
  commissionDate?: string
  status: number
  remark?: string
  createTime?: string
}

export interface EnergyType {
  id: number
  code: string
  name: string
  unit: string
  price: number
  co2Factor?: number
  status: number
}

export interface RoleItem {
  id: number
  roleCode: string
  roleName: string
  description?: string
  permissions: string[]
  sortNo?: number
  status: number
  builtin?: number
  createTime?: string
}

export interface UserItem {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  enterpriseId?: number
  enterpriseName?: string
  status: number
  roles: RoleItem[]
  lastLoginTime?: string
  createTime?: string
  remark?: string
}

export interface UserProfile {
  id: number
  username: string
  realName?: string
  enterpriseId?: number
  enterpriseName?: string
  roles: string[]
  permissions: string[]
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserProfile
}

export interface MenuItem {
  key: string
  title: string
  icon?: string
  path?: string
  children: MenuItem[]
}

export interface MeterItem {
  id: number
  enterpriseId: number
  deviceId?: number
  deviceName?: string
  lineId?: number
  lineName?: string
  meterCode: string
  meterName: string
  energyTypeId: number
  energyTypeName?: string
  ratio: number
  collectIntervalSeconds: number
  readingType: number
  installTime?: string
  status: number
  remark?: string
  createTime?: string
}