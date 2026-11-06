export interface RectificationTask {
  id: number
  taskNo: string
  title: string
  deviceId?: number
  deviceName?: string
  deviceCode?: string
  lineName?: string
  reportId?: number
  anomalyId?: number
  ruleName?: string
  status: number
  statusLabel: string
  priority: number
  priorityLabel: string
  assigneeId?: number
  assigneeName?: string
  transferCount?: number
  expectedSaving?: number
  progress: number
  deadline?: string
  overdue: boolean
  rejectReason?: string
  assignTime?: string
  finishTime?: string
  auditTime?: string
  createTime: string
  content?: string
  measure?: string
}

export interface RectificationFeedback {
  id: number
  feedbackType: number
  feedbackTypeLabel: string
  content?: string
  beforeDesc?: string
  afterDesc?: string
  images: string[]
  actualSaving?: number
  submitByName?: string
  submitTime: string
  verifyResult?: number
  verifyRemark?: string
  verifyTime?: string
}

export interface RectificationTaskDetail extends RectificationTask {
  feedbacks: RectificationFeedback[]
  reportNo?: string
  reportDescription?: string
  reportConclusion?: string
  anomalies: string[]
}

export interface RectificationStats {
  total: number
  overdue: number
  byStatus: Record<string, number>
}

export interface Assignee {
  id: number
  username: string
  realName?: string
  phone?: string
}
