<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAnomaly, fetchReportByAnomaly, handleAnomaly } from '@/api/diagnosis'
import type { AnomalyItem, DiagnosisReport } from '@/types/diagnosis'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const anomaly = ref<AnomalyItem | null>(null)
const report = ref<DiagnosisReport | null>(null)

const evidenceText = computed(() => JSON.stringify(anomaly.value?.evidence ?? {}, null, 2))
const severityMap: Record<number, { label: string; type: 'info' | 'warning' | 'danger' }> = {
  1: { label: '低风险', type: 'info' },
  2: { label: '中风险', type: 'warning' },
  3: { label: '高风险', type: 'danger' },
  4: { label: '严重', type: 'danger' }
}

async function load(): Promise<void> {
  const id = Number(route.params.id)
  loading.value = true
  error.value = ''
  try {
    anomaly.value = await fetchAnomaly(id)
    if (anomaly.value.reportId) {
      try {
        report.value = await fetchReportByAnomaly(id)
      } catch {
        report.value = null
      }
    }
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '异常详情加载失败'
  } finally {
    loading.value = false
  }
}

async function handle(status: number): Promise<void> {
  if (!anomaly.value) return
  if (status === 2 || status === 4) {
    await ElMessageBox.confirm('确认执行该操作？', '处理异常', { type: 'warning' })
  }
  await handleAnomaly(anomaly.value.id, status)
  ElMessage.success('已更新处理状态')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>{{ anomaly?.ruleName || '异常详情' }}</h2>
          <p class="muted">
            {{ anomaly?.ruleCode }} · {{ anomaly?.deviceName }}（{{ anomaly?.deviceCode }}） ·
            {{ anomaly?.lineName || '—' }} / {{ anomaly?.workshopName || '—' }}
          </p>
        </div>
        <div class="actions">
          <el-tag v-if="anomaly" :type="severityMap[anomaly.severity]?.type || 'info'">
            {{ anomaly.riskLabel }} · {{ anomaly.riskScore }} 分
          </el-tag>
          <el-button size="small" @click="router.push('/diagnosis/anomalies')">返回列表</el-button>
          <el-button v-if="report" size="small" type="primary" @click="router.push(`/diagnosis/reports/${report.id}`)">
            查看诊断报告
          </el-button>
        </div>
      </div>
      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />

      <div v-if="anomaly" class="kpi-row">
        <div class="kpi"><span>实际能耗</span><strong>{{ anomaly.actualValue }}<em>kWh</em></strong></div>
        <div class="kpi"><span>动态基线</span><strong>{{ anomaly.baselineValue }}<em>kWh</em></strong></div>
        <div class="kpi"><span>偏差率</span><strong :class="Number(anomaly.deviationRate) > 0 ? 'up' : 'down'">{{ anomaly.deviationRate }}<em>%</em></strong></div>
        <div class="kpi"><span>持续时长</span><strong>{{ anomaly.durationMinutes }}<em>分钟</em></strong></div>
        <div class="kpi"><span>合并次数</span><strong>{{ anomaly.mergeCount }}</strong></div>
        <div class="kpi"><span>处理状态</span><strong>{{ anomaly.statusLabel }}</strong></div>
      </div>
    </el-card>

    <el-card shadow="never" v-if="anomaly">
      <template #header><span class="title">异常信息</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="异常区间">{{ anomaly.periodStart }} ~ {{ anomaly.periodEnd }}</el-descriptions-item>
        <el-descriptions-item label="首次发现">{{ anomaly.detectTime }}</el-descriptions-item>
        <el-descriptions-item label="最近命中">{{ anomaly.lastDetectTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="触发阈值">{{ anomaly.thresholdValue ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ anomaly.deviceType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="报告编号">{{ anomaly.reportNo || '—' }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="sub">规则证据</h3>
      <pre class="evidence">{{ evidenceText }}</pre>

      <div class="handle">
        <el-button type="success" @click="handle(1)">确认异常</el-button>
        <el-button type="warning" @click="handle(3)">转诊断</el-button>
        <el-button @click="handle(2)">标记误报</el-button>
        <el-button type="info" @click="handle(4)">关闭</el-button>
      </div>
    </el-card>

    <el-card v-if="report" shadow="never">
      <template #header><span class="title">诊断报告摘要</span></template>
      <p class="conclusion">{{ report.conclusion }}</p>
      <div class="tags">
        <el-tag v-for="cause in report.causes" :key="cause" type="warning" effect="plain">{{ cause }}</el-tag>
      </div>
      <ul v-if="report.suggestions.length" class="suggest">
        <li v-for="item in report.suggestions" :key="item">{{ item }}</li>
      </ul>
    </el-card>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head h2 { margin: 0; font-size: 20px; color: #0f2a43; }
.muted { margin: 6px 0 0; color: #909399; font-size: 12px; }
.actions { display: flex; gap: 10px; align-items: center; }
.kpi-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-top: 16px; }
.kpi { display: flex; flex-direction: column; gap: 6px; padding: 12px 14px; background: #f7f9fc; border-radius: 10px; }
.kpi span { font-size: 12px; color: #909399; }
.kpi strong { font-size: 20px; color: #0f2a43; }
.kpi em { margin-left: 4px; font-size: 12px; color: #909399; font-style: normal; }
.up { color: #e5484d; }
.down { color: #2f9e44; }
.title { font-weight: 600; }
.sub { margin: 18px 0 8px; font-size: 14px; color: #0f2a43; }
.evidence { margin: 0; padding: 12px 14px; background: #f7f9fc; border-radius: 8px; font-size: 12px; color: #606266; max-height: 220px; overflow: auto; }
.handle { display: flex; gap: 10px; margin-top: 18px; flex-wrap: wrap; }
.conclusion { margin: 0 0 12px; color: #0f2a43; line-height: 1.7; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; }
.suggest { margin: 14px 0 0; padding-left: 18px; color: #606266; line-height: 2; }
</style>
