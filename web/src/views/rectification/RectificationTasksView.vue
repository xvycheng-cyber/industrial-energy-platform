<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignRectificationTask,
  auditRectificationTask,
  cancelRectificationTask,
  createRectificationTask,
  fetchRectificationStats,
  fetchRectificationTask,
  listAssignees,
  pageRectificationTasks,
  transferRectificationTask,
  verifyRectificationTask
} from '@/api/rectification'
import { pageDevices } from '@/api/master'
import type { Assignee, RectificationStats, RectificationTask, RectificationTaskDetail } from '@/types/rectification'
import type { Device } from '@/types/models'

const loading = ref(false)
const records = ref<RectificationTask[]>([])
const total = ref(0)
const stats = ref<RectificationStats | null>(null)
const assignees = ref<Assignee[]>([])
const devices = ref<Device[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined,
  assigneeId: undefined as number | undefined,
  keyword: '',
  overdue: false
})

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { label: '待分配', type: 'info' },
  1: { label: '待整改', type: 'warning' },
  2: { label: '整改中', type: 'primary' },
  3: { label: '待审核', type: 'warning' },
  7: { label: '效果验证中', type: 'primary' },
  4: { label: '已完成', type: 'success' },
  5: { label: '已驳回', type: 'danger' },
  6: { label: '已撤销', type: 'info' }
}

const createVisible = ref(false)
const assignVisible = ref(false)
const auditVisible = ref(false)
const verifyVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<RectificationTaskDetail | null>(null)
const current = ref<RectificationTask | null>(null)

const createForm = reactive({
  deviceId: undefined as number | undefined,
  assigneeId: undefined as number | undefined,
  title: '',
  measure: '',
  expectedSaving: undefined as number | undefined,
  deadline: ''
})

const assignForm = reactive({ assigneeId: undefined as number | undefined, deadline: '', reason: '' })
const auditForm = reactive({ pass: true, reason: '', remark: '' })
const verifyForm = reactive({ pass: true, actualSaving: undefined as number | undefined, reason: '', remark: '' })

const statusOptions = computed(() => Object.entries(statusMap).map(([value, item]) => ({
  value: Number(value),
  label: item.label
})))

async function load(): Promise<void> {
  loading.value = true
  try {
    const [page, stat] = await Promise.all([
      pageRectificationTasks({ ...query, status: query.status ?? undefined }),
      fetchRectificationStats()
    ])
    records.value = page.records
    total.value = page.total
    stats.value = stat
  } finally {
    loading.value = false
  }
}

async function loadBase(): Promise<void> {
  const [people, devicePage] = await Promise.all([listAssignees(), pageDevices({ pageNum: 1, pageSize: 200 })])
  assignees.value = people
  devices.value = devicePage.records
}

async function openCreate(): Promise<void> {
  createForm.deviceId = undefined
  createForm.assigneeId = assignees.value[0]?.id
  createForm.title = ''
  createForm.measure = ''
  createForm.expectedSaving = undefined
  createForm.deadline = ''
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  if (!createForm.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  await createRectificationTask({
    deviceId: createForm.deviceId,
    title: createForm.title || undefined,
    measure: createForm.measure || undefined,
    expectedSaving: createForm.expectedSaving,
    assigneeId: createForm.assigneeId,
    deadline: createForm.deadline || undefined
  })
  ElMessage.success('整改任务已创建')
  createVisible.value = false
  await load()
}

function openAssign(item: RectificationTask, transfer: boolean): void {
  current.value = item
  assignForm.assigneeId = item.assigneeId ?? assignees.value[0]?.id
  assignForm.deadline = ''
  assignForm.reason = ''
  assignVisible.value = true
  assignVisible.value = true
  assignMode.value = transfer ? 'transfer' : 'assign'
}
const assignMode = ref<'assign' | 'transfer'>('assign')

async function submitAssign(): Promise<void> {
  if (!current.value || !assignForm.assigneeId) {
    ElMessage.warning('请选择负责人')
    return
  }
  const payload = {
    assigneeId: assignForm.assigneeId,
    deadline: assignForm.deadline || undefined,
    reason: assignForm.reason || undefined
  }
  if (assignMode.value === 'transfer') {
    await transferRectificationTask(current.value.id, payload)
  } else {
    await assignRectificationTask(current.value.id, payload)
  }
  ElMessage.success(assignMode.value === 'transfer' ? '已转派' : '已分配')
  assignVisible.value = false
  await load()
}

function openAudit(item: RectificationTask): void {
  current.value = item
  auditForm.pass = true
  auditForm.reason = ''
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitAudit(): Promise<void> {
  if (!current.value) return
  if (!auditForm.pass && !auditForm.reason.trim()) {
    ElMessage.warning('驳回必须填写原因')
    return
  }
  await auditRectificationTask(current.value.id, { ...auditForm })
  ElMessage.success(auditForm.pass ? '审核通过，已进入效果验证' : '已驳回')
  auditVisible.value = false
  detailVisible.value = false
  await load()
}

function openVerify(item: RectificationTask): void {
  current.value = item
  verifyForm.pass = true
  verifyForm.actualSaving = item.expectedSaving
  verifyForm.reason = ''
  verifyForm.remark = ''
  verifyVisible.value = true
}

async function submitVerify(): Promise<void> {
  if (!current.value) return
  if (!verifyForm.pass && !verifyForm.reason.trim()) {
    ElMessage.warning('验证不通过必须填写原因')
    return
  }
  await verifyRectificationTask(current.value.id, { ...verifyForm })
  ElMessage.success(verifyForm.pass ? '验证通过，任务已完成' : '已退回整改')
  verifyVisible.value = false
  detailVisible.value = false
  await load()
}

async function openDetail(item: RectificationTask): Promise<void> {
  current.value = item
  detail.value = await fetchRectificationTask(item.id)
  detailVisible.value = true
}

async function cancel(item: RectificationTask): Promise<void> {
  const { value } = await ElMessageBox.prompt('请填写撤销原因', '撤销任务', {
    inputPlaceholder: '撤销原因',
    type: 'warning'
  })
  await cancelRectificationTask(item.id, value)
  ElMessage.success('已撤销')
  await load()
}

function format(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(async () => {
  await loadBase()
  await load()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>整改任务</h2>
          <p class="muted">从诊断到整改、审核、效果验证的闭环管理；状态只能由后端流转。</p>
        </div>
        <div class="actions">
          <el-button type="primary" @click="openCreate">创建整改任务</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
      <div class="stats">
        <div><span>总数</span><b>{{ stats?.total ?? 0 }}</b></div>
        <div><span>待分配</span><b>{{ stats?.byStatus?.['待分配'] ?? 0 }}</b></div>
        <div><span>待整改</span><b>{{ stats?.byStatus?.['待整改'] ?? 0 }}</b></div>
        <div><span>待审核</span><b>{{ stats?.byStatus?.['待审核'] ?? 0 }}</b></div>
        <div><span>效果验证中</span><b>{{ stats?.byStatus?.['效果验证中'] ?? 0 }}</b></div>
        <div><span>已驳回</span><b class="danger">{{ stats?.byStatus?.['已驳回'] ?? 0 }}</b></div>
        <div><span>超期</span><b class="danger">{{ stats?.overdue ?? 0 }}</b></div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.assigneeId" placeholder="全部负责人" clearable style="width: 150px" @change="load">
          <el-option v-for="item in assignees" :key="item.id" :label="item.realName || item.username" :value="item.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="任务号/标题" clearable style="width: 180px" @keyup.enter="load" />
        <el-checkbox v-model="query.overdue" @change="load">仅看超期</el-checkbox>
        <el-button :loading="loading" @click="load">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="taskNo" label="任务号" width="160" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="130" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="设备" width="110" />
        <el-table-column prop="assigneeName" label="负责人" width="90" />
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="90">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            <span :class="{ danger: row.overdue }">{{ format(row.deadline) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="214" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" text type="success" @click="openAssign(row, false)">分配</el-button>
            <el-button v-if="[1, 2, 3, 5].includes(row.status)" text @click="openAssign(row, true)">转派</el-button>
            <el-button v-if="row.status === 3" text type="warning" @click="openAudit(row)">审核</el-button>
            <el-button v-if="row.status === 7" text type="success" @click="openVerify(row)">验证</el-button>
            <el-button v-if="[0, 1, 2, 3, 5, 7].includes(row.status)" text type="danger" @click="cancel(row)">撤销</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无整改任务" /></template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="createVisible" title="创建整改任务" width="620px">
      <el-form label-width="110px">
        <el-form-item label="设备">
          <el-select v-model="createForm.deviceId" filterable style="width: 100%">
            <el-option v-for="item in devices" :key="item.id" :label="`${item.name}（${item.code}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="缺省按设备生成" />
        </el-form-item>
        <el-form-item label="整改措施">
          <el-input v-model="createForm.measure" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预期节能量">
          <el-input-number v-model="createForm.expectedSaving" :min="0" :precision="2" /> kWh
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="createForm.assigneeId" clearable style="width: 100%">
            <el-option v-for="item in assignees" :key="item.id" :label="item.realName || item.username" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="createForm.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" :title="assignMode === 'transfer' ? '转派任务' : '分配任务'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="负责人">
          <el-select v-model="assignForm.assigneeId" style="width: 100%">
            <el-option v-for="item in assignees" :key="item.id" :label="item.realName || item.username" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="assignForm.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="assignMode === 'transfer'" label="转派原因">
          <el-input v-model="assignForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auditVisible" title="审核整改" width="560px">
      <el-form label-width="100px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :value="true">通过（进入效果验证）</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!auditForm.pass" label="驳回原因" required>
          <el-input v-model="auditForm.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="verifyVisible" title="效果验证" width="560px">
      <el-form label-width="100px">
        <el-form-item label="验证结果">
          <el-radio-group v-model="verifyForm.pass">
            <el-radio :value="true">通过（完成任务）</el-radio>
            <el-radio :value="false">不通过（退回整改）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实际节能量">
          <el-input-number v-model="verifyForm.actualSaving" :min="0" :precision="2" /> kWh
        </el-form-item>
        <el-form-item v-if="!verifyForm.pass" label="退回原因" required>
          <el-input v-model="verifyForm.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="验证备注">
          <el-input v-model="verifyForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitVerify">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="整改任务详情" size="620px">
      <div v-if="detail" class="detail">
        <div class="detail-head">
          <h3>{{ detail.title }}</h3>
          <el-tag :type="statusMap[detail.status]?.type || 'info'">{{ detail.statusLabel }}</el-tag>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务号">{{ detail.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ detail.deviceName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detail.assigneeName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="截止">{{ format(detail.deadline) }}</el-descriptions-item>
          <el-descriptions-item label="预期节能">{{ detail.expectedSaving ?? '—' }} kWh</el-descriptions-item>
          <el-descriptions-item label="进度">{{ detail.progress }}%</el-descriptions-item>
          <el-descriptions-item label="整改措施" :span="2">{{ detail.measure || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">
            <span class="danger">{{ detail.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h4>诊断结论</h4>
        <p class="report">{{ detail.reportConclusion || detail.content || '—' }}</p>
        <ul v-if="detail.anomalies.length" class="anomalies">
          <li v-for="item in detail.anomalies" :key="item">{{ item }}</li>
        </ul>

        <h4>整改反馈</h4>
        <el-timeline v-if="detail.feedbacks.length">
          <el-timeline-item
            v-for="item in detail.feedbacks"
            :key="item.id"
            :timestamp="format(item.submitTime)"
            :type="item.feedbackType === 4 ? 'danger' : item.feedbackType === 3 || item.feedbackType === 5 ? 'success' : 'primary'"
          >
            <div class="feedback">
              <b>{{ item.feedbackTypeLabel }}</b>
              <span v-if="item.submitByName"> · {{ item.submitByName }}</span>
              <p>{{ item.content }}</p>
              <p v-if="item.beforeDesc" class="muted">整改前：{{ item.beforeDesc }}</p>
              <p v-if="item.afterDesc" class="muted">整改后：{{ item.afterDesc }}</p>
              <div v-if="item.images.length" class="images">
                <el-image
                  v-for="url in item.images"
                  :key="url"
                  :src="url"
                  :preview-src-list="item.images"
                  fit="cover"
                  style="width: 88px; height: 88px; border-radius: 6px"
                />
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="尚无整改反馈" :image-size="70" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head h2 { margin: 0; font-size: 20px; color: #0f2a43; }
.muted { color: #909399; font-size: 12px; }
.danger { color: #e5484d; }
.actions { display: flex; gap: 10px; }
.stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 12px; margin-top: 16px; }
.stats div { display: flex; flex-direction: column; gap: 4px; padding: 10px 12px; background: #f7f9fc; border-radius: 8px; }
.stats span { font-size: 12px; color: #909399; }
.stats b { font-size: 20px; color: #0f2a43; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.detail-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.detail h4 { margin: 18px 0 8px; font-size: 14px; color: #0f2a43; }
.report { margin: 0; color: #606266; line-height: 1.8; }
.anomalies { margin: 8px 0 0; padding-left: 18px; color: #606266; font-size: 13px; }
.feedback p { margin: 6px 0 0; font-size: 13px; color: #606266; }
.images { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
</style>
