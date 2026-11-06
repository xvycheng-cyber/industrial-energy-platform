<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aggregateDay, completeness, detectGaps } from '@/api/datalake'
import { pageDevices } from '@/api/master'
import type { CompletenessResult } from '@/types/datalake'
import type { Device } from '@/types/models'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const error = ref('')
const date = ref(new Date().toISOString().substring(0, 10))
const deviceId = ref<number | ''>('')
const devices = ref<Device[]>([])
const data = ref<CompletenessResult | null>(null)
const detecting = ref(false)

const rateColor = computed(() => {
  const rate = data.value?.completenessRate ?? 0
  if (rate >= 99) return '#67c23a'
  if (rate >= 90) return '#e6a23c'
  return '#f56c6c'
})

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    data.value = await completeness({
      date: date.value,
      deviceId: deviceId.value === '' ? undefined : deviceId.value
    })
  } catch (exception) {
    data.value = null
    error.value = exception instanceof Error ? exception.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onDetect(): Promise<void> {
  detecting.value = true
  try {
    const count = await detectGaps(date.value)
    ElMessage.success(`检测完成：新增缺口 ${count} 个`)
  } finally {
    detecting.value = false
  }
}

async function onAggregate(): Promise<void> {
  const count = await aggregateDay(date.value)
  ElMessage.success(`聚合完成：${count} 个周期桶`)
}

onMounted(async () => {
  const result = await pageDevices({ pageNum: 1, pageSize: 500 })
  devices.value = result.records
  await load()
})
</script>

<template>
  <div class="datalake-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width: 160px" @change="load" />
          <el-select v-model="deviceId" placeholder="全部设备" clearable filterable style="width: 200px" @change="load">
            <el-option v-for="item in devices" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </div>
        <div class="toolbar-right">
          <el-button v-if="userStore.hasPermission('datalake:gap:manage')" type="warning" :loading="detecting" @click="onDetect">
            检测缺失
          </el-button>
          <el-button v-if="userStore.hasPermission('datalake:clean')" type="primary" plain @click="onAggregate">
            重算聚合
          </el-button>
        </div>
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
        <template #default><el-button size="small" @click="load">重新加载</el-button></template>
      </el-alert>

      <div v-loading="loading" class="rate-box">
        <el-progress
          type="dashboard"
          :percentage="data?.completenessRate ?? 0"
          :color="rateColor"
          :width="150"
        >
          <template #default="{ percentage }">
            <div class="percent">{{ percentage }}%</div>
            <div class="percent-label">完整率</div>
          </template>
        </el-progress>
        <div class="rate-metrics">
          <div class="metric">
            <span class="metric-label">应采点数</span>
            <span class="metric-value">{{ data?.expectedCount ?? 0 }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">有效入库</span>
            <span class="metric-value ok">{{ data?.validCount ?? 0 }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">缺失点数</span>
            <span class="metric-value bad">{{ data?.missingCount ?? 0 }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">达标线</span>
            <span class="metric-value">≥ 99%</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><span class="card-title">按设备完整率</span></template>
      <el-table :data="data?.devices ?? []" border stripe>
        <el-table-column prop="meterCode" label="仪表编号" width="140" />
        <el-table-column prop="meterName" label="计量点" min-width="160" />
        <el-table-column prop="deviceName" label="设备" width="150" />
        <el-table-column prop="expectedCount" label="应采" width="100" align="right" />
        <el-table-column prop="validCount" label="有效" width="100" align="right" />
        <el-table-column prop="missingCount" label="缺失" width="100" align="right" />
        <el-table-column label="完整率" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="row.completenessRate"
              :color="row.completenessRate >= 99 ? '#67c23a' : row.completenessRate >= 90 ? '#e6a23c' : '#f56c6c'"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无数据" /></template>
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

.error-bar {
  margin-bottom: 12px;
}

.card-title {
  font-weight: 600;
}

.rate-box {
  display: flex;
  align-items: center;
  gap: 48px;
  padding: 12px 8px;
  flex-wrap: wrap;
}

.percent {
  font-size: 26px;
  font-weight: 700;
  color: #0f2a43;
}

.percent-label {
  font-size: 12px;
  color: #909399;
}

.rate-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(160px, 1fr));
  gap: 16px 40px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  color: #909399;
}

.metric-value {
  font-size: 20px;
  font-weight: 600;
}

.metric-value.ok {
  color: #67c23a;
}

.metric-value.bad {
  color: #f56c6c;
}
</style>
