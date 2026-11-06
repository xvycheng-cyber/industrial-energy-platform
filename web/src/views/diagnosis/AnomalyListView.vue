<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { handleAnomaly, pageAnomalies, runScan } from '@/api/diagnosis'
import type { AnomalyItem } from '@/types/diagnosis'

const router = useRouter()
const loading = ref(false)
const scanning = ref(false)
const records = ref<AnomalyItem[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  ruleCode: undefined as string | undefined,
  severity: undefined as number | undefined,
  status: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const rules = [
  { code: 'R001', name: '能耗突增' },
  { code: 'R002', name: '持续高能耗' },
  { code: 'R003', name: '空载高能耗' },
  { code: 'R004', name: '设备空转' },
  { code: 'R005', name: '单位产品能耗异常' },
  { code: 'R006', name: '夜间异常耗能' },
  { code: 'R007', name: '设备能耗波动' },
  { code: 'R008', name: '同类设备能耗异常' }
]

const severityMap: Record<number, { label: string; type: 'info' | 'warning' | 'danger' }> = {
  1: { label: '低风险', type: 'info' },
  2: { label: '中风险', type: 'warning' },
  3: { label: '高风险', type: 'danger' },
  4: { label: '严重', type: 'danger' }
}

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
  0: { label: '待确认', type: 'warning' },
  1: { label: '已确认', type: 'success' },
  2: { label: '误报', type: 'info' },
  3: { label: '已转诊断', type: 'success' },
  4: { label: '已关闭', type: 'info' }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pageAnomalies({ ...query })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function scan(): Promise<void> {
  scanning.value = true
  try {
    const result = await runScan()
    if (result.skipped) {
      ElMessage.warning('诊断任务正在执行，请稍后')
    } else {
      ElMessage.success(`扫描完成：新增 ${result.newAnomalies} 条，合并 ${result.mergedAnomalies} 条`)
      await load()
    }
  } finally {
    scanning.value = false
  }
}

async function updateStatus(item: AnomalyItem, status: number): Promise<void> {
  const labels: Record<number, string> = { 1: '确认', 2: '标记误报', 4: '关闭' }
  if (status !== 1) {
    await ElMessageBox.confirm(`确认${labels[status]}该异常？`, '处理异常', { type: 'warning' })
  }
  await handleAnomaly(item.id, status)
  ElMessage.success('已更新')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>异常监控</h2>
        <p>规则识别、风险分级与诊断处置，按企业范围隔离。</p>
      </div>
      <div class="page-actions">
        <el-tag type="info" effect="plain">{{ records.length ? `本页 ${records.length} 条` : '实时监测' }}</el-tag>
      </div>
    </div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.ruleCode" placeholder="全部规则" clearable style="width: 170px" @change="load">
          <el-option v-for="item in rules" :key="item.code" :label="`${item.code} ${item.name}`" :value="item.code" />
        </el-select>
        <el-select v-model="query.severity" placeholder="全部风险" clearable style="width: 120px" @change="load">
          <el-option label="低风险" :value="1" />
          <el-option label="中风险" :value="2" />
          <el-option label="高风险" :value="3" />
          <el-option label="严重" :value="4" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="load">
          <el-option label="待确认" :value="0" />
          <el-option label="已确认" :value="1" />
          <el-option label="误报" :value="2" />
          <el-option label="已转诊断" :value="3" />
          <el-option label="已关闭" :value="4" />
        </el-select>
        <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始" style="width: 140px" @change="load" />
        <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束" style="width: 140px" @change="load" />
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :loading="scanning" @click="scan">立即诊断</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="ruleCode" label="规则" width="64" />
        <el-table-column prop="ruleName" label="规则名称" min-width="120" />
        <el-table-column prop="deviceName" label="设备" width="110" />
        <el-table-column label="风险" width="84">
          <template #default="{ row }">
            <el-tag :type="severityMap[row.severity]?.type || 'info'" size="small">
              {{ severityMap[row.severity]?.label || row.riskLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="评分" width="70" />
        <el-table-column prop="periodStart" label="异常开始" width="160" />
        <el-table-column prop="durationMinutes" label="持续(分)" width="78" />
        <el-table-column label="偏差率" width="86">
          <template #default="{ row }">
            <span :class="Number(row.deviationRate) > 0 ? 'up' : 'down'">
              {{ row.deviationRate != null ? Number(row.deviationRate).toFixed(1) : '—' }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="router.push(`/diagnosis/anomalies/${row.id}`)">详情</el-button>
            <el-button v-if="row.reportId" text type="primary" @click="router.push(`/diagnosis/reports/${row.reportId}`)">报告</el-button>
            <el-button v-if="row.status === 0" text type="success" @click="updateStatus(row, 1)">确认</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无异常，可点击「立即诊断」" /></template>
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
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.up { color: #e5484d; }
.down { color: #2f9e44; }
</style>
