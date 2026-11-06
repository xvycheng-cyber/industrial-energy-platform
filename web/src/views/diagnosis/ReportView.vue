<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmReport, fetchReport } from '@/api/diagnosis'
import type { DiagnosisReport } from '@/types/diagnosis'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const report = ref<DiagnosisReport | null>(null)
const confirmedCause = ref('')

const snapshot = computed(() => JSON.stringify(report.value?.ruleSnapshot ?? {}, null, 2))

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    report.value = await fetchReport(Number(route.params.id))
    confirmedCause.value = report.value.confirmedCause ?? ''
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '报告加载失败'
  } finally {
    loading.value = false
  }
}

async function saveCause(): Promise<void> {
  if (!report.value || !confirmedCause.value.trim()) {
    ElMessage.warning('请填写人工确认原因')
    return
  }
  saving.value = true
  try {
    await confirmReport(report.value.id, confirmedCause.value.trim())
    ElMessage.success('已保存人工确认原因')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>诊断报告 {{ report?.reportNo }}</h2>
          <p class="muted">
            {{ report?.ruleCode }} {{ report?.ruleName }} · {{ report?.deviceName }}（{{ report?.deviceCode }}） ·
            生成于 {{ report?.generatedAt }}
          </p>
        </div>
        <div class="actions">
          <el-tag type="danger">{{ report?.riskLabel }}</el-tag>
          <el-button size="small" @click="router.push(`/diagnosis/anomalies/${report?.anomalyId}`)">查看异常</el-button>
          <el-button size="small" @click="router.push('/diagnosis/anomalies')">返回列表</el-button>
        </div>
      </div>
      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    </el-card>

    <el-card v-if="report" shadow="never">
      <template #header><span class="title">异常描述</span></template>
      <p class="text">{{ report.description }}</p>
      <div class="metrics">
        <span>实际 <b>{{ report.actualValue }}</b> kWh</span>
        <span>基线 <b>{{ report.baselineValue }}</b> kWh</span>
        <span>偏差率 <b>{{ report.deviationRate }}%</b></span>
        <span>预计节能量 <b>{{ report.estimatedSaving }}</b> kWh</span>
      </div>
    </el-card>

    <el-card v-if="report" shadow="never">
      <template #header><span class="title">诊断结论</span></template>
      <p class="conclusion">{{ report.conclusion }}</p>
    </el-card>

    <div class="grid">
      <el-card v-if="report" shadow="never" class="span-6">
        <template #header><span class="title">系统推断的可能原因</span></template>
        <ul class="causes">
          <li v-for="item in report.causes" :key="item">{{ item }}</li>
        </ul>
        <p class="hint">以上为规则与工况推断，不代表已确认故障。</p>
      </el-card>

      <el-card v-if="report" shadow="never" class="span-6">
        <template #header><span class="title">人工确认的故障原因</span></template>
        <el-input v-model="confirmedCause" type="textarea" :rows="4" placeholder="请填写现场确认的故障原因" />
        <div class="confirm-actions">
          <el-button type="primary" :loading="saving" @click="saveCause">保存确认</el-button>
          <span v-if="report.confirmedTime" class="hint">确认于 {{ report.confirmedTime }}</span>
        </div>
      </el-card>

      <el-card v-if="report" shadow="never" class="span-6">
        <template #header><span class="title">整改建议</span></template>
        <ul class="causes">
          <li v-for="item in report.suggestions" :key="item">{{ item }}</li>
        </ul>
      </el-card>

      <el-card v-if="report" shadow="never" class="span-6">
        <template #header><span class="title">规则快照</span></template>
        <pre class="evidence">{{ snapshot }}</pre>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head h2 { margin: 0; font-size: 20px; color: #0f2a43; }
.muted { margin: 6px 0 0; color: #909399; font-size: 12px; }
.actions { display: flex; gap: 10px; align-items: center; }
.title { font-weight: 600; }
.text { margin: 0; color: #303133; line-height: 1.8; }
.metrics { display: flex; gap: 24px; margin-top: 14px; font-size: 13px; color: #606266; }
.metrics b { color: #0f2a43; font-size: 16px; }
.conclusion { margin: 0; font-size: 15px; color: #0f2a43; line-height: 1.9; }
.grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 16px; }
.span-6 { grid-column: span 6; }
.causes { margin: 0; padding-left: 18px; color: #606266; line-height: 2; }
.hint { margin: 10px 0 0; color: #909399; font-size: 12px; }
.confirm-actions { display: flex; gap: 12px; align-items: center; margin-top: 12px; }
.evidence { margin: 0; padding: 12px; background: #f7f9fc; border-radius: 8px; font-size: 12px; color: #606266; max-height: 220px; overflow: auto; }
@media (max-width: 1200px) { .span-6 { grid-column: span 12; } }
</style>
