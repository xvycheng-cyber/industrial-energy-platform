export interface RawDataItem {
  id: number
  meterId: number
  meterCode?: string
  meterName?: string
  deviceName?: string
  collectTime: string
  rawValue: number
  readingType: number
  quality: number
  source: string
  batchNo: string
  cleanStatus: number
  receivedTime?: string
}

export interface DetailItem {
  id: number
  meterId: number
  meterName?: string
  deviceName?: string
  periodStart: string
  periodEnd: string
  energyValue: number
  unit?: string
  startReading?: number
  endReading?: number
  estimated: number
  qualityFlag: number
  calcVersion?: string
}

export interface IngestLogItem {
  id: number
  batchNo: string
  enterpriseId?: number
  enterpriseName?: string
  dataDate?: string
  source: string
  expectedCount: number
  receivedCount: number
  validCount: number
  missingCount: number
  successRate: number
  status: number
  errorMsg?: string
  startTime?: string
  endTime?: string
}

export interface FailureItem {
  id: number
  batchNo: string
  meterId?: number
  meterName?: string
  collectTime?: string
  errorCode: string
  errorMsg?: string
  retryCount: number
  status: number
  payload?: string
  createTime?: string
}

export interface GapItem {
  id: number
  meterId: number
  meterName?: string
  deviceName?: string
  gapStart: string
  gapEnd: string
  expectedCount: number
  actualCount: number
  missingCount: number
  status: number
  fillTime?: string
}

export interface AggregateItem {
  id: number
  dimType: number
  dimId: number
  dimName?: string
  energyTypeId: number
  energyTypeName?: string
  unit?: string
  periodType: number
  periodStart: string
  energyValue: number
  costAmount?: number
  dataCount?: number
}

export interface DeviceCompleteness {
  meterId: number
  meterCode?: string
  meterName?: string
  deviceId?: number
  deviceName?: string
  expectedCount: number
  validCount: number
  missingCount: number
  completenessRate: number
}

export interface CompletenessResult {
  expectedCount: number
  validCount: number
  missingCount: number
  completenessRate: number
  devices: DeviceCompleteness[]
}

export interface IngestResult {
  batchNo: string
  receivedCount: number
  insertedCount: number
  duplicatedCount: number
  failedCount: number
  expectedCount: number
  completenessRate: number
  status: number
}

export interface ImportResult {
  batchNo: string
  totalRows: number
  successRows: number
  failedRows: number
  errors: string[]
}

export interface QualitySummary {
  date: string
  total: number
  normal: number
  suspicious: number
  invalid: number
  duplicate: number
  pendingClean: number
  failurePending: number
}

export interface EnergySlice {
  energyTypeId: number
  energyTypeName: string
  unit?: string
  energyValue: number
  costAmount?: number
}

export interface ProfilePoint {
  time: string
  label: string
  energyValue: number
}

export interface DashboardResult {
  date: string
  totalEnergy: number
  totalCost: number
  completenessRate: number
  deviceCount: number
  todayBatchCount: number
  lastBatchNo?: string
  energyTypes: EnergySlice[]
  topDevices: { deviceId: number; deviceName: string; energyValue: number }[]
  hourly: ProfilePoint[]
}

export interface ProfileResult {
  dimType: number
  dimId: number
  dimName?: string
  days: number
  totalEnergy: number
  avgEnergy: number
  maxEnergy: number
  minEnergy: number
  series: ProfilePoint[]
  energyTypes: EnergySlice[]
}
