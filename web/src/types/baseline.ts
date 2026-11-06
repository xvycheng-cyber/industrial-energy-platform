export interface BaselineModel {
  id: number
  enterpriseId: number
  dimType: number
  dimId: number
  dimName: string
  energyTypeId: number
  energyTypeName: string
  modelVersion: string
  algo: string
  baselineType: number
  sampleCount: number
  standbySamples?: number
  sampleRejected: number
  outlierCount: number
  fallbackCount: number
  mae?: number
  rmse?: number
  mape?: number
  r2?: number
  confidence: number
  status: number
  statusLabel: string
  trainStart?: string
  trainEnd?: string
  effectiveFrom?: string
  effectiveTo?: string
  calcTime?: string
  featureNames: string[]
  fallbackLog: string[]
  params?: Record<string, unknown>
}

export interface BaselineRecord {
  id: number
  baselineId: number
  modelVersion?: string
  periodStart: string
  periodEnd: string
  actualValue?: number
  baselineValue: number
  deviationValue?: number
  deviationRate?: number
  baselineType: number
  baselineTypeName: string
  fallbackReason?: string
  comparable: number
  condition: Record<string, number | string>
}

export interface BaselineTrainResult {
  modelId: number
  modelVersion: string
  algo: string
  sampleCount: number
  normalSamples: number
  standbySamples: number
  rejectedSamples: number
  outlierSamples: number
  requiredSamples: number
  mae?: number
  rmse?: number
  mape?: number
  r2?: number
  predictedCount: number
  activated: boolean
  fallbacks: string[]
}

export interface ResidualBucket {
  range: string
  count: number
}

export interface BaselineErrorAnalysis {
  baselineId: number
  modelVersion: string
  sampleCount: number
  mae?: number
  rmse?: number
  mape?: number
  r2?: number
  fallbackCount: number
  buckets: ResidualBucket[]
  worst: BaselineRecord[]
}
