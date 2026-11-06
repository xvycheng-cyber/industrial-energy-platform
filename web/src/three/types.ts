export interface Vec3 {
  x: number
  y: number
  z: number
}

export type RiskLevel = 'NORMAL' | 'LOW' | 'MEDIUM' | 'HIGH' | 'OFFLINE'

export interface DeviceNode {
  id: number
  code: string
  name: string
  deviceType: string
  status: number
  risk: RiskLevel
  riskLabel: string
  riskReason: string
  energyToday: number
  position: Vec3
}

export interface LineNode {
  id: number
  code: string
  name: string
  energyToday: number
  position: Vec3
  devices: DeviceNode[]
}

export interface WorkshopNode {
  id: number
  code: string
  name: string
  position: Vec3
  width: number
  depth: number
  height: number
  energyToday: number
  lines: LineNode[]
}

export interface FactorySceneData {
  enterpriseId: number
  enterpriseName: string
  width: number
  depth: number
  workshopCount: number
  lineCount: number
  deviceCount: number
  energyToday: number
  dataTime: string
  workshops: WorkshopNode[]
}

export interface DeviceDetail {
  id: number
  code: string
  name: string
  deviceType: string
  model?: string
  ratedPower?: number
  status: number
  statusLabel: string
  risk: RiskLevel
  riskLabel: string
  riskReason?: string
  lineName?: string
  workshopName?: string
  energyToday: number
  baseline?: number
  deviationRate?: number
  anomalyLevel?: string
  diagnosis?: string
  updatedAt?: string
  baselineAvailable: boolean
  anomalyAvailable: boolean
}

export interface LineDetail {
  id: number
  code: string
  name: string
  workshopName?: string
  outputQty?: number
  outputUnit?: string
  energyToday: number
  baseline?: number
  deviationRate?: number
  deviceCount: number
  runningCount: number
  anomalyDeviceCount?: number
  riskCounts: Record<string, number>
  baselineAvailable: boolean
}
