export interface KpiCard {
  key: string
  label: string
  value: number | null
  unit?: string
  available: boolean
  hint?: string
}

export interface ChartPoint {
  time: string
  label: string
  energyValue: number
}

export interface EnergySlice {
  energyTypeId?: number
  energyTypeName: string
  unit?: string
  energyValue: number
  costAmount?: number
}

export interface LineRank {
  lineId: number
  lineName: string
  energyValue: number
}

export interface BaselinePoint {
  label: string
  actual: number
  baseline: number
  deviationRate?: number
}

export interface AnomalySlice {
  name: string
  count: number
  ratio: number
}

export interface SavingPoint {
  month: string
  savingEnergy: number
  savingAmount?: number
}

export interface SankeyNode {
  name: string
  depth?: number
}

export interface SankeyLink {
  source: string
  target: string
  value: number
}

export interface SankeyGraph {
  nodes: SankeyNode[]
  links: SankeyLink[]
}

export interface CockpitData {
  date: string
  granularity: string
  scopeType: number
  scopeId: number
  scopeName: string
  energyTypeId?: number
  kpis: KpiCard[]
  trend: ChartPoint[]
  structure: EnergySlice[]
  lineRanking: LineRank[]
  baseline: BaselinePoint[]
  anomaly: AnomalySlice[]
  saving: SavingPoint[]
  flow: SankeyGraph
  unavailable: string[]
}

export interface Rating {
  score: number
  grade: string
  basis: string
  dimensions: string[]
}

export interface EnterpriseProfile {
  id: number
  name: string
  industry?: string
  region?: string
  days: number
  totalEnergy: number
  totalCost: number
  coalConsumption: number
  outputTotal?: number
  outputUnit?: string
  unitProductEnergy?: number
  energyIntensity?: number
  completenessRate: number
  structure: EnergySlice[]
  trend: ChartPoint[]
  rating: Rating
  unavailable: string[]
}

export interface LineProfile {
  id: number
  code: string
  name: string
  enterpriseName?: string
  workshopName?: string
  days: number
  energy: number
  outputQty?: number
  outputUnit?: string
  runMinutes?: number
  unitProductEnergy?: number
  deviceCount: number
  activeDeviceCount: number
  baseline?: number
  deviationRate?: number
  anomalyDeviceCount?: number
  trend: ChartPoint[]
  structure: EnergySlice[]
  unavailable: string[]
}

export interface DeviceProfile {
  id: number
  code: string
  name: string
  deviceType: string
  model?: string
  ratedPower?: number
  lineName?: string
  workshopName?: string
  enterpriseName?: string
  status: number
  commissionDate?: string
  days: number
  energy: number
  runMinutes?: number
  loadRate?: number
  outputQty?: number
  unitProductEnergy?: number
  trend: ChartPoint[]
  structure: EnergySlice[]
  unavailable: string[]
}

export interface HierarchyNode {
  id: number
  type: 'enterprise' | 'workshop' | 'line' | 'device'
  name: string
  code?: string
  children: HierarchyNode[]
}
