<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseChart from '@/components/BaseChart.vue'
import { confirmSavingResult, fetchSavingResult } from '@/api/saving'
import type { SavingResult } from '@/types/saving'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const result = ref<SavingResult | null>(null)

const conclusionMap: Record<number, { label: string; type: 'success' | 'info' | 'danger' | 'warning' }> = {
  1: { label: '节能达成', type: 'success' },
  2: { label: '无明显变化', type: 'info' },
  3: { label: '能耗恶化', type: 'danger' },
  4: { label: '数据不足待验证', type: 'warning' },
  5: { label: '工况不可比', type: 'warning' },
  6: { label: '停机不判定节能', type: 'info' }
}

const compareOption = computed<EChartsOption>(() => {
  const points = result.value?.points ?? []
  return {
    grid: { left: 52, right: 20, top: 36, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 4, textStyle: { color: '#606266', fontSize: 12 } },
    xAxis: {
      type: 'category',
      data: points.map((item) => item.time.slice(5, 16)),
      axisLabel: { color: '#909399', fontSize: 10, interval: Math.max(0, Math.floor(points.length / 10)) },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      name: 'kWh',
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        name: '整改后实际',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: points.map((item) => item.actual),
        lineStyle: { width: 2, color: '#2f6fed' },
        areaStyle: { color: 'rgba(47,111,237,0.08)' }
      },
      {
        name: '修正后动态基线',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: points.map((item) => item.baseline),
        lineStyle: { width: 2, color: '#e6a23c', type: 'dashed' }
      }
    ]
  }
})

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    result.value = await fetchSavingResult(Number(route.params.id))
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '核算详情加载失败'
  } finally {
    loading.value = false
  }
}

async function audit(pass: boolean): Promise<void> {
  if (!result.value) return
  let remark = ''
  if (pass) {
    await ElMessageBox.confirm('审核通过后整改任务将标记为已完成，确认？', '审核核算结果', { type: 'warning' })
    remark = '核算结果审核通过'
  } else {
    const input = await ElMessageBox.prompt('请填写驳回原因', '驳回核算结果', {
      type: 'warning',
      inputPlaceholder: '例如：观察期不满足 7 天'
    })
    remark = input.value
  }
  await confirmSavingResult(result.value.id, pass, remark)
  ElMessage.success(pass ? '已通过，任务完成' : '已驳回，任务退回整改')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>{{ result?.title || '节能核算详情' }}</h2>
          <p class="muted">
            {{ result?.taskNo }} · {{ result?.deviceName }} · 观察期 {{ result?.verifyStart?.slice(0, 10) }} ~ {{ result?.verifyEnd?.slice(0, 10) }}
            · 版本 v{{ result?.version }}
          </p>
        </div>
        <div class="actions">
          <el-tag :type="conclusionMap[result?.conclusionCode ?? 0]?.type || 'info'">
            {{ conclusionMap[result?.conclusionCode ?? 0]?.label || result?.conclusion }}
          </el-tag>
          <el-button size="small" @click="router.push('/saving/results')">返回列表</el-button>
          <template v-if="result?.status === 0">
            <el-button size="small" type="success" @click="audit(true)">审核通过</el-button>
            <el-button size="small" type="danger" @click="audit(false)">驳回</el-button>
          </template>
        </div>
      </div>
      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />

      <div class="kpi-row">
        <div class="kpi"><span>修正后基线</span><strong>{{ result?.baselineTotal }}<em>kWh</em></strong></div>
        <div class="kpi"><span>实际能耗</span><strong>{{ result?.actualTotal }}<em>kWh</em></strong></div>
        <div class="kpi">
          <span>节能量</span>
          <strong :class="Number(result?.savingValue) < 0 ? 'red' : 'green'">{{ result?.savingValue }}<em>kWh</em></strong>
        </div>
        <div class="kpi"><span>节能率</span><strong>{{ result?.savingRate != null ? Number(result.savingRate).toFixed(2) : '—' }}<em>%</em></strong></div>
        <div class="kpi"><span>节能金额</span><strong>{{ result?.savingAmount != null ? Number(result.savingAmount).toFixed(2) : '—' }}<em>元</em></strong></div>
        <div class="kpi"><span>数据完整率</span><strong>{{ result?.completeness }}<em>%</em></strong></div>
      </div>
    </el-card>

    <el-card v-if="result" shadow="never">
      <template #header><span class="title">整改前后对比（修正基线 vs 实际）</span></template>
      <BaseChart v-if="(result.points?.length ?? 0) > 0" :option="compareOption" height="320px" />
      <el-empty v-else description="暂无逐小时数据" />
    </el-card>

    <div class="grid">
      <el-card v-if="result" shadow="never" class="span-6">
        <template #header><span class="title">核算口径</span></template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="观察天数">{{ result.actualDays }} / 7 天</el-descriptions-item>
          <el-descriptions-item label="基线版本">{{ result.baselineModelVersion }}</el-descriptions-item>
          <el-descriptions-item label="能源单价">{{ result.unitPrice }} 元/kWh</el-descriptions-item>
          <el-descriptions-item label="工况可比">{{ result.comparable === 1 ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="整改前产量">{{ result.outputBefore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="观察期产量">{{ result.outputAfter ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="产量变化">{{ result.outputChangeRate ?? 0 }}%</el-descriptions-item>
          <el-descriptions-item label="状态">{{ result.statusLabel }}</el-descriptions-item>
        </el-descriptions>
        <p class="reason">{{ result.reason }}</p>
      </el-card>

      <el-card v-if="result" shadow="never" class="span-6">
        <template #header><span class="title">核算结论</span></template>
        <el-alert
          :type="conclusionMap[result.conclusionCode]?.type || 'info'"
          :title="conclusionMap[result.conclusionCode]?.label || result.conclusion"
          :description="result.reason"
          :closable="false"
          show-icon
        />
        <div class="audit">
          <p><span>审核时间：</span>{{ result.auditedAt || '—' }}</p>
          <p><span>审核意见：</span>{{ result.auditRemark || '—' }}</p>
        </div>
      </el-card>

      <el-card v-if="result" shadow="never" class="span-12">
        <template #header><span class="title">历史版本</span></template>
        <el-table :data="result.versions" border size="small">
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column prop="conclusion" label="结论" min-width="150" />
          <el-table-column prop="savingValue" label="节能量" width="120" />
          <el-table-column prop="createTime" label="计算时间" min-width="160" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.archived === 1 ? 'info' : 'success'" size="small">{{ row.archived === 1 ? '历史版本' : '当前版本' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.id !== result.id" text type="primary" @click="router.push(`/saving/results/${row.id}`)">查看</el-button>
              <span v-else class="muted">当前</span>
            </template>
          </el-table-column>
        </el-table>
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
.kpi-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-top: 16px; }
.kpi { display: flex; flex-direction: column; gap: 6px; padding: 12px 14px; background: #f7f9fc; border-radius: 10px; }
.kpi span { font-size: 12px; color: #909399; }
.kpi strong { font-size: 20px; color: #0f2a43; }
.kpi em { margin-left: 4px; font-size: 12px; color: #909399; font-style: normal; }
.green { color: #2f9e44; }
.red { color: #e5484d; }
.grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 16px; }
.span-6 { grid-column: span 6; }
.span-12 { grid-column: span 12; }
.title { font-weight: 600; }
.reason { margin: 14px 0 0; color: #606266; line-height: 1.7; }
.audit p { margin: 10px 0 0; font-size: 13px; color: #606266; }
.audit span { color: #909399; }
@media (max-width: 1200px) { .span-6 { grid-column: span 12; } }
</style>
