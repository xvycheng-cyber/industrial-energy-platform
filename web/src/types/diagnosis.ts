export interface AnomalyItem {
  id: number
  deviceId: number
  deviceName?: string
  deviceCode?: string
  deviceType?: string
  lineName?: string
  workshopName?: string
  ruleCode: string
  ruleName: string
  anomalyType: string
  severity: number
  riskLabel: string
  riskScore?: number
  periodStart: string
  periodEnd: string
  durationMinutes: number
  mergeCount: number
  actualValue?: number
  baselineValue?: number
  deviationRate?: number
  thresholdValue?: number
  detectTime: string
  lastDetectTime?: string
  status: number
  statusLabel: string
  reportId?: number
  reportNo?: string
  remark?: string
  evidence?: Record<string, unknown>
}

export interface DiagnosisReport {
  id: number
  reportNo: string
  anomalyId: number
  deviceId: number
  deviceName?: string
  deviceCode?: string
  ruleCode?: string
  ruleName?: string
  description: string
  conclusion: string
  causes: string[]
  confirmedCause?: string
  confirmedTime?: string
  suggestions: string[]
  estimatedSaving?: number
  riskLevel: number
  riskLabel: string
  status: number
  generatedAt: string
  actualValue?: number
  baselineValue?: number
  deviationRate?: number
  ruleSnapshot?: Record<string, unknown>
}

export interface DiagnosisRule {
  id: number
  ruleCode: string
  ruleName: string
  anomalyType: string
  condition: Record<string, unknown>
  threshold: Record<string, unknown>
  weight: number
  priority: number
  version: number
  enabled: number
  causeTemplate?: string
  suggestionTemplate?: string
  savingFormula?: string
  remark?: string
}

export interface ScanResult {
  batchNo: string
  ruleCount: number
  deviceCount: number
  signalCount: number
  newAnomalies: number
  mergedAnomalies: number
  reportCount: number
  durationMs: number
  skipped: boolean
}

export interface OverviewData {
  total: number
  openCount: number
  handledCount: number
  falsePositiveCount: number
  highRiskCount: number
  byRule: Record<string, number>
  bySeverity: Record<string, number>
  byStatus: Record<string, number>
  trend: Array<{ date: string; count: number; high: number }>
  topDevices: Array<{ deviceId: number; deviceName: string; count: number }>
}
