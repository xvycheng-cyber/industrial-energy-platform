export interface SavingPoint {
  time: string
  baseline: number
  actual: number
  saving: number
  price: number
}

export interface SavingVersion {
  id: number
  version: number
  conclusion: string
  savingValue: number
  createTime: string
  archived: number
}

export interface SavingResult {
  id: number
  taskId: number
  taskNo?: string
  title?: string
  deviceId: number
  deviceName?: string
  lineName?: string
  version: number
  verifyStart: string
  verifyEnd: string
  actualDays: number
  sampleDays: number
  baselineModelVersion: string
  baselineTotal: number
  actualTotal: number
  savingValue: number
  savingRate: number
  savingAmount: number
  unitPrice: number
  completeness: number
  comparable: number
  outputBefore?: number
  outputAfter?: number
  outputChangeRate?: number
  conclusionCode: number
  conclusion: string
  reason?: string
  status: number
  statusLabel: string
  archived: number
  auditedAt?: string
  auditRemark?: string
  createTime: string
  points: SavingPoint[]
  versions: SavingVersion[]
}

export interface SavingSummary {
  totalSaving: number
  totalAmount: number
  resultCount: number
  ranking: Array<{ deviceId: number; deviceName: string; saving: number; rate: number }>
  trend: Array<{ month: string; saving: number; amount: number }>
}
