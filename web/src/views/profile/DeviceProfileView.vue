<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/BaseChart.vue'
import { fetchDeviceProfile } from '@/api/profile'
import type { DeviceProfile } from '@/types/profile'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const profile = ref<DeviceProfile | null>(null)
const days = ref(7)

const palette = ['#2f6fed', '#20b2aa', '#e6a23c', '#e06c75']

const statusText = computed(() => {
  const map: Record<number, string> = { 1: '运行', 2: '停机', 3: '检修', 0: '停用' }
  return map[profile.value?.status ?? 0] ?? '未知'
})

const trendOption = computed<EChartsOption>(() => {
  const points = profile.value?.trend ?? []
  return {
    grid: { left: 50, right: 18, top: 26, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: (v: unknown) => `${v} kWh` },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.label),
      axisLabel: { color: '#909399', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: points.map((p) => p.energyValue),
        lineStyle: { width: 2, color: '#2f6fed' },
        areaStyle: { color: 'rgba(47,111,237,0.10)' }
      }
    ]
  }
})

const structureOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', valueFormatter: (v: unknown) => `${v} kWh` },
  legend: { bottom: 0, textStyle: { color: '#606266', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
  series: [
    {
      type: 'pie',
      radius: ['46%', '68%'],
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{d}%', fontSize: 11, color: '#606266' },
      data: (profile.value?.structure ?? []).map((item, index) => ({
        name: item.energyTypeName,
        value: item.energyValue,
        itemStyle: { color: palette[index % palette.length] }
      }))
    }
  ]
}))

async function load(): Promise<void> {
  const id = Number(route.query.id)
  if (!id) {
    error.value = '缺少设备参数'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    profile.value = await fetchDeviceProfile(id, { days: days.value })
  } catch (exception) {
    profile.value = null
    error.value = exception instanceof Error ? exception.message : '设备画像加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="profile-page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>{{ profile?.name || '设备能耗画像' }}</h2>
          <p class="muted">
            {{ profile?.enterpriseName || '—' }} / {{ profile?.workshopName || '—' }} / {{ profile?.lineName || '—' }}
          </p>
        </div>
        <div class="head-actions">
          <el-radio-group v-model="days" size="small" @change="load">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="90">近 90 天</el-radio-button>
          </el-radio-group>
          <el-button size="small" @click="router.back()">返回</el-button>
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
        <template #default><el-button size="small" @click="load">重试</el-button></template>
      </el-alert>

      <el-descriptions v-if="profile" :column="4" border class="basic">
        <el-descriptions-item label="设备编码">{{ profile.code }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ profile.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ profile.model || '—' }}</el-descriptions-item>
        <el-descriptions-item label="额定功率">{{ profile.ratedPower ? `${profile.ratedPower} kW` : '—' }}</el-descriptions-item>
        <el-descriptions-item label="运行状态">{{ statusText }}</el-descriptions-item>
        <el-descriptions-item label="投运日期">{{ profile.commissionDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="所属产线">{{ profile.lineName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="所属车间">{{ profile.workshopName || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div v-loading="loading" class="kpi-row">
        <div class="kpi"><span>能源消耗</span><strong>{{ profile?.energy ?? '—' }}<em>kWh</em></strong></div>
        <div class="kpi"><span>运行时长</span><strong>{{ profile?.runMinutes ?? '—' }}<em>分钟</em></strong></div>
        <div class="kpi">
          <span>平均负荷率</span>
          <strong v-if="profile?.loadRate">{{ profile.loadRate }}<em>%</em></strong>
          <strong v-else class="pending">待工况数据</strong>
        </div>
        <div class="kpi">
          <span>产量</span>
          <strong v-if="profile?.outputQty">{{ profile.outputQty }}<em>件</em></strong>
          <strong v-else class="pending">待生产数据</strong>
        </div>
        <div class="kpi">
          <span>单位产量能耗</span>
          <strong v-if="profile?.unitProductEnergy">{{ profile.unitProductEnergy }}<em>kWh/件</em></strong>
          <strong v-else class="pending">待生产数据</strong>
        </div>
        <div class="kpi"><span>历史异常</span><strong class="pending">P6 接入</strong></div>
      </div>
    </el-card>

    <div class="grid">
      <el-card shadow="never" class="span-8">
        <template #header><span class="card-title">能耗趋势</span></template>
        <BaseChart v-if="profile && profile.trend.length" :option="trendOption" height="300px" />
        <el-empty v-else description="暂无趋势数据" />
      </el-card>
      <el-card shadow="never" class="span-4">
        <template #header><span class="card-title">能源结构</span></template>
        <BaseChart v-if="profile && profile.structure.length" :option="structureOption" height="300px" />
        <el-empty v-else description="暂无结构数据" />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.head h2 {
  margin: 0;
  font-size: 20px;
  color: #0f2a43;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.muted {
  margin: 6px 0 0;
  color: #909399;
  font-size: 12px;
}

.error-bar {
  margin-top: 12px;
}

.basic {
  margin-top: 16px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.kpi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  background: #f7f9fc;
  border-radius: 10px;
}

.kpi span {
  font-size: 12px;
  color: #909399;
}

.kpi strong {
  font-size: 20px;
  color: #0f2a43;
  font-variant-numeric: tabular-nums;
}

.kpi em {
  margin-left: 4px;
  font-size: 12px;
  color: #909399;
  font-style: normal;
  font-weight: 400;
}

.kpi .pending {
  font-size: 14px;
  color: #e6a23c;
}

.grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px;
}

.span-8 {
  grid-column: span 8;
}

.span-4 {
  grid-column: span 4;
}

.card-title {
  font-weight: 600;
}

@media (max-width: 1200px) {
  .span-8,
  .span-4 {
    grid-column: span 12;
  }
}
</style>
