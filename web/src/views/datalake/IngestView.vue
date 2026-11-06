<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ingestBatch, pageLogs, simulate } from '@/api/datalake'
import type { ReadingPayload } from '@/api/datalake'
import { pageMeters } from '@/api/master'
import type { MeterItem } from '@/types/models'
import type { IngestLogItem, IngestResult } from '@/types/datalake'

const meterOptions = ref<MeterItem[]>([])
const submitting = ref(false)
const result = ref<IngestResult | null>(null)
const logs = ref<IngestLogItem[]>([])

const form = reactive({
  meterId: undefined as number | undefined,
  startTime: '',
  points: 60,
  intervalMinutes: 1,
  baseValue: 1000,
  stepValue: 0.5,
  source: 'MANUAL'
})

const simulateForm = reactive({ start: '', end: '', stepMinutes: 1 })
const simulating = ref(false)

function format(date: Date): string {
  const pad = (v: number) => (v < 10 ? `0${v}` : `${v}`)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function loadBase(): Promise<void> {
  const [meters, logPage] = await Promise.all([
    pageMeters({ pageNum: 1, pageSize: 500 }),
    pageLogs({ pageNum: 1, pageSize: 5 })
  ])
  meterOptions.value = meters.records
  logs.value = logPage.records
}

async function submit(): Promise<void> {
  if (!form.meterId) {
    ElMessage.warning('请选择仪表')
    return
  }
  if (!form.startTime) {
    ElMessage.warning('请选择起始时间')
    return
  }
  submitting.value = true
  try {
    const start = new Date(form.startTime.replace(/-/g, '/')).getTime()
    const readings: ReadingPayload[] = []
    for (let i = 0; i < form.points; i += 1) {
      const time = new Date(start + i * form.intervalMinutes * 60 * 1000)
      readings.push({
        meterId: form.meterId,
        collectTime: format(time),
        value: Number((form.baseValue + i * form.stepValue).toFixed(4)),
        readingType: 1,
        pointCode: 'TOTAL'
      })
    }
    result.value = await ingestBatch({
      batchNo: `MAN-${Date.now()}`,
      source: form.source,
      readings
    })
    ElMessage.success(`接入完成：新增 ${result.value.insertedCount} 条`)
    const logPage = await pageLogs({ pageNum: 1, pageSize: 5 })
    logs.value = logPage.records
  } finally {
    submitting.value = false
  }
}

async function runSimulate(): Promise<void> {
  simulating.value = true
  try {
    const payload: { start?: string; end?: string; stepMinutes?: number } = {
      stepMinutes: simulateForm.stepMinutes
    }
    if (simulateForm.start) payload.start = simulateForm.start
    if (simulateForm.end) payload.end = simulateForm.end
    const count = await simulate(payload)
    ElMessage.success(`模拟采集完成：${count} 条`)
  } finally {
    simulating.value = false
  }
}

onMounted(loadBase)
</script>

<template>
  <div class="datalake-page">
    <el-card shadow="never">
      <template #header><span class="card-title">批量接入（ODS）</span></template>
      <el-form :model="form" label-width="110px" class="ingest-form">
        <el-form-item label="仪表">
          <el-select v-model="form.meterId" placeholder="请选择计量点" filterable style="width: 320px">
            <el-option
              v-for="item in meterOptions"
              :key="item.id"
              :label="`${item.meterCode} ${item.meterName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="起始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 320px" />
        </el-form-item>
        <el-form-item label="采集点数">
          <el-input-number v-model="form.points" :min="1" :max="2000" />
          <span class="hint">最多 2000 点/批</span>
        </el-form-item>
        <el-form-item label="采集间隔(分)">
          <el-input-number v-model="form.intervalMinutes" :min="1" :max="60" />
        </el-form-item>
        <el-form-item label="起始读数">
          <el-input-number v-model="form.baseValue" :min="0" :precision="4" />
        </el-form-item>
        <el-form-item label="每点递增">
          <el-input-number v-model="form.stepValue" :min="0" :precision="4" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交接入</el-button>
          <el-button @click="form.startTime = ''">清空时间</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="result" type="success" :closable="false" show-icon class="result">
        <template #title>
          批次 {{ result.batchNo }}：接收 {{ result.receivedCount }}，新增 {{ result.insertedCount }}，
          失败 {{ result.failedCount }}，完整率 {{ result.completenessRate }}%
        </template>
      </el-alert>
    </el-card>

    <el-card shadow="never" class="mt">
      <template #header><span class="card-title">模拟采集</span></template>
      <el-form :model="simulateForm" inline label-width="90px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="simulateForm.start" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="simulateForm.end" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="步长(分)">
          <el-input-number v-model="simulateForm.stepMinutes" :min="1" :max="60" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="simulating" @click="runSimulate">生成模拟数据</el-button>
          <span class="hint">留空则生成最近 1 分钟；生产环境由定时任务每分钟生成</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt">
      <template #header><span class="card-title">最近批次</span></template>
      <el-table :data="logs" border stripe>
        <el-table-column prop="batchNo" label="批次号" min-width="220" />
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="receivedCount" label="接收" width="90" align="right" />
        <el-table-column prop="validCount" label="有效" width="90" align="right" />
        <el-table-column prop="successRate" label="完整率(%)" width="110" align="right" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : row.status === 2 ? '部分成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="时间" width="180" />
        <template #empty><el-empty description="暂无批次" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.datalake-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-weight: 600;
}

.ingest-form {
  max-width: 720px;
}

.hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.result {
  margin-top: 8px;
}

.mt {
  margin-top: 0;
}
</style>
