import type { RiskLevel } from './types'

export const RISK_COLORS: Record<RiskLevel, number> = {
  NORMAL: 0x22c55e,
  LOW: 0xeab308,
  MEDIUM: 0xf97316,
  HIGH: 0xef4444,
  OFFLINE: 0x64748b
}

export const RISK_LABELS: Record<RiskLevel, string> = {
  NORMAL: '正常',
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  OFFLINE: '离线'
}

export const COLORS = {
  background: 0x0a1726,
  fog: 0x0a1726,
  ground: 0x102a42,
  grid: 0x1b3a57,
  road: 0x121f2e,
  building: 0x24506f,
  buildingEdge: 0x3b7ea8,
  roof: 0x1e3d53,
  line: 0x1d4055,
  lineStripe: 0x3b82f6,
  deviceBody: 0x8ba3b8,
  deviceDark: 0x46586b,
  pipe: 0x35d0ba,
  substation: 0x2ea3ff,
  label: '#dbeafe'
}
