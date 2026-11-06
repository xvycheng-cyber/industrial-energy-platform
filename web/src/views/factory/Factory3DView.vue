<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FactorySceneController, RISK_COLORS, RISK_LABELS } from '@/three'
import type { DeviceDetail, FactorySceneData, LineDetail, RiskLevel } from '@/three/types'
import type { HoverInfo } from '@/three/interaction'
import { fetchFactoryDevice, fetchFactoryLine, fetchFactoryScene } from '@/api/factory'

const route = useRoute()

const container = ref<HTMLDivElement>()
const tooltip = ref<HTMLDivElement>()

const loading = ref(true)
const error = ref('')
const scene = ref<FactorySceneData | null>(null)
const controller = shallowRef<FactorySceneController | null>(null)

const hover = ref<HoverInfo | null>(null)
const hoverStyle = ref({ left: '0px', top: '0px' })

const selected = ref<{ kind: 'device' | 'line'; id: number } | null>(null)
const deviceDetail = ref<DeviceDetail | null>(null)
const lineDetail = ref<LineDetail | null>(null)
const detailLoading = ref(false)

const legend = (Object.keys(RISK_LABELS) as RiskLevel[]).map((risk) => ({
  risk,
  label: RISK_LABELS[risk],
  color: `#${RISK_COLORS[risk].toString(16).padStart(6, '0')}`
}))

const riskColor = computed(() => {
  const risk = deviceDetail.value?.risk
  return risk ? `#${RISK_COLORS[risk].toString(16).padStart(6, '0')}` : '#8ba3b8'
})

function showTooltip(info: HoverInfo | null, clientX: number, clientY: number): void {
  hover.value = info
  if (!info || !container.value) {
    return
  }
  const rect = container.value.getBoundingClientRect()
  hoverStyle.value = {
    left: `${clientX - rect.left + 14}px`,
    top: `${clientY - rect.top + 14}px`
  }
}

async function selectDevice(id: number): Promise<void> {
  selected.value = { kind: 'device', id }
  detailLoading.value = true
  try {
    deviceDetail.value = await fetchFactoryDevice(id)
    lineDetail.value = null
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '设备详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function selectLine(id: number): Promise<void> {
  selected.value = { kind: 'line', id }
  detailLoading.value = true
  try {
    lineDetail.value = await fetchFactoryLine(id)
    deviceDetail.value = null
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '产线详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function ensureController(): FactorySceneController | null {
  if (!container.value) {
    return null
  }
  if (!controller.value) {
    controller.value = new FactorySceneController(container.value, {
      onHover: showTooltip,
      onDeviceClick: (id) => void selectDevice(id),
      onLineClick: (id) => void selectLine(id)
    })
  }
  return controller.value
}

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    scene.value = await fetchFactoryScene()
    await nextTick()
    const instance = ensureController()
    if (instance && scene.value) {
      instance.setData(scene.value)
    }
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '三维场景加载失败'
  } finally {
    loading.value = false
  }
}

function resetView(): void {
  controller.value?.resetView()
}

function closePanel(): void {
  selected.value = null
  deviceDetail.value = null
  lineDetail.value = null
}

onMounted(async () => {
  await load()
  const deviceId = Number(route.query.deviceId)
  const lineId = Number(route.query.lineId)
  if (deviceId) {
    await selectDevice(deviceId)
  } else if (lineId) {
    await selectLine(lineId)
  }
})

onBeforeUnmount(() => {
  controller.value?.dispose()
  controller.value = null
})
</script>

<template>
  <div class="factory-page">
    <div ref="container" class="scene-container"></div>

    <div class="hud hud-left">
      <div class="title-block">
        <h1>三维工厂</h1>
        <p>{{ scene?.enterpriseName || '工业企业' }} · {{ scene?.dataTime || '—' }}</p>
      </div>
      <div v-if="scene" class="stats">
        <span><b>{{ scene.workshopCount }}</b> 车间</span>
        <span><b>{{ scene.lineCount }}</b> 产线</span>
        <span><b>{{ scene.deviceCount }}</b> 设备</span>
        <span><b>{{ scene.energyToday }}</b> kWh 今日</span>
      </div>
    </div>

    <div class="hud hud-right">
      <el-button size="small" type="primary" plain @click="resetView">复位视角</el-button>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="legend">
      <span v-for="item in legend" :key="item.risk" class="legend-item">
        <i :style="{ background: item.color }"></i>{{ item.label }}
      </span>
      <span class="legend-tip">左键旋转 · 右键平移 · 滚轮缩放 · 点击查看详情</span>
    </div>

    <div
      v-if="hover"
      ref="tooltip"
      class="tooltip"
      :style="hoverStyle"
    >
      <strong>{{ hover.title }}</strong>
      <span class="tooltip-sub">{{ hover.subtitle }}</span>
      <span v-if="hover.risk" class="tooltip-risk">{{ RISK_LABELS[hover.risk as RiskLevel] }}</span>
    </div>

    <el-alert v-if="error" class="error-bar" type="error" :title="error" show-icon :closable="false">
      <template #default><el-button size="small" @click="load">重新加载</el-button></template>
    </el-alert>

    <div v-if="loading && !scene" class="loading-mask">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
      <span>正在构建三维场景…</span>
    </div>

    <aside v-if="selected" class="panel">
      <header class="panel-head">
        <span>{{ selected.kind === 'device' ? '设备详情' : '产线详情' }}</span>
        <el-icon class="close" @click="closePanel"><Close /></el-icon>
      </header>

      <div v-loading="detailLoading" class="panel-body">
        <template v-if="selected.kind === 'device' && deviceDetail">
          <div class="device-title">
            <h2>{{ deviceDetail.name }}</h2>
            <span class="risk-dot" :style="{ background: riskColor }"></span>
            <span class="risk-text">{{ deviceDetail.riskLabel }}</span>
          </div>
          <p class="muted">{{ deviceDetail.code }} · {{ deviceDetail.deviceType }}</p>

          <dl class="kv">
            <div><dt>运行状态</dt><dd>{{ deviceDetail.statusLabel }}</dd></div>
            <div><dt>所属产线</dt><dd>{{ deviceDetail.lineName || '—' }}</dd></div>
            <div><dt>所属车间</dt><dd>{{ deviceDetail.workshopName || '—' }}</dd></div>
            <div><dt>额定功率</dt><dd>{{ deviceDetail.ratedPower ? `${deviceDetail.ratedPower} kW` : '—' }}</dd></div>
            <div><dt>今日能耗</dt><dd class="strong">{{ deviceDetail.energyToday }} kWh</dd></div>
            <div>
              <dt>动态基线</dt>
              <dd v-if="deviceDetail.baselineAvailable">{{ deviceDetail.baseline }} kWh</dd>
              <dd v-else class="pending">暂无基线</dd>
            </div>
            <div>
              <dt>偏差率</dt>
              <dd v-if="deviceDetail.baselineAvailable">{{ deviceDetail.deviationRate }} %</dd>
              <dd v-else class="pending">暂无基线</dd>
            </div>
            <div>
              <dt>异常等级</dt>
              <dd v-if="deviceDetail.anomalyAvailable">{{ deviceDetail.anomalyLevel }}</dd>
              <dd v-else class="pending">暂无异常</dd>
            </div>
          </dl>

          <div class="diagnosis">
            <h3>最近诊断结论</h3>
            <p v-if="deviceDetail.anomalyAvailable">{{ deviceDetail.diagnosis }}</p>
            <p v-else class="pending">暂无诊断结论</p>
          </div>

          <p class="risk-reason">状态依据：{{ deviceDetail.riskReason }}</p>
        </template>

        <template v-else-if="selected.kind === 'line' && lineDetail">
          <div class="device-title">
            <h2>{{ lineDetail.name }}</h2>
          </div>
          <p class="muted">{{ lineDetail.code }} · {{ lineDetail.workshopName || '—' }}</p>

          <dl class="kv">
            <div><dt>今日能耗</dt><dd class="strong">{{ lineDetail.energyToday }} kWh</dd></div>
            <div><dt>今日产量</dt><dd>{{ lineDetail.outputQty ?? '—' }} {{ lineDetail.outputUnit || '' }}</dd></div>
            <div>
              <dt>能耗基线</dt>
              <dd v-if="lineDetail.baselineAvailable">{{ lineDetail.baseline }} kWh</dd>
              <dd v-else class="pending">暂无基线</dd>
            </div>
            <div>
              <dt>偏差率</dt>
              <dd v-if="lineDetail.baselineAvailable">{{ lineDetail.deviationRate }} %</dd>
              <dd v-else class="pending">暂无基线</dd>
            </div>
            <div><dt>设备总数</dt><dd>{{ lineDetail.deviceCount }}</dd></div>
            <div><dt>运行设备</dt><dd>{{ lineDetail.runningCount }}</dd></div>
            <div>
              <dt>异常设备</dt>
              <dd v-if="lineDetail.anomalyDeviceCount != null">{{ lineDetail.anomalyDeviceCount }}</dd>
              <dd v-else class="pending">暂无异常</dd>
            </div>
          </dl>

          <div class="risk-stats">
            <h3>设备风险分布</h3>
            <div v-for="item in legend" :key="item.risk" class="risk-row">
              <span class="legend-item"><i :style="{ background: item.color }"></i>{{ item.label }}</span>
              <b>{{ lineDetail.riskCounts[item.risk] ?? 0 }}</b>
            </div>
          </div>
        </template>

        <el-empty v-else description="暂无详情" :image-size="70" />
      </div>
    </aside>
  </div>
</template>

<style scoped>
.factory-page {
  position: relative;
  height: calc(100vh - 92px);
  margin: -18px;
  overflow: hidden;
  background: #0a1726;
}

.scene-container {
  position: absolute;
  inset: 0;
}

.hud {
  position: absolute;
  z-index: 5;
  display: flex;
  gap: 12px;
  align-items: center;
  color: #dbeafe;
}

.hud-left {
  top: 18px;
  left: 22px;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.title-block h1 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 3px;
}

.title-block p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #7d97b3;
}

.stats {
  display: flex;
  gap: 14px;
  padding: 8px 14px;
  background: rgba(18, 46, 74, 0.72);
  border: 1px solid rgba(46, 163, 255, 0.16);
  border-radius: 8px;
  font-size: 12px;
  color: #8ba9c4;
}

.stats b {
  color: #ffffff;
  font-size: 15px;
  margin-right: 2px;
}

.hud-right {
  top: 18px;
  right: 22px;
}

.legend {
  position: absolute;
  z-index: 5;
  left: 22px;
  bottom: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 14px;
  background: rgba(18, 46, 74, 0.72);
  border: 1px solid rgba(46, 163, 255, 0.16);
  border-radius: 8px;
  color: #cfe3f5;
  font-size: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-item i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

.legend-tip {
  color: #6f8aa6;
  margin-left: 6px;
}

.tooltip {
  position: absolute;
  z-index: 8;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  background: rgba(8, 20, 33, 0.92);
  border: 1px solid rgba(46, 163, 255, 0.35);
  border-radius: 6px;
  color: #dbeafe;
  font-size: 12px;
  pointer-events: none;
  white-space: nowrap;
}

.tooltip strong {
  font-size: 13px;
}

.tooltip-sub {
  color: #7d97b3;
}

.tooltip-risk {
  color: #f2b84b;
}

.error-bar {
  position: absolute;
  z-index: 9;
  top: 90px;
  left: 50%;
  transform: translateX(-50%);
  width: min(560px, 80%);
}

.loading-mask {
  position: absolute;
  inset: 0;
  z-index: 7;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  color: #7d97b3;
  background: rgba(10, 23, 38, 0.85);
}

.panel {
  position: absolute;
  z-index: 6;
  top: 84px;
  right: 18px;
  bottom: 64px;
  width: 360px;
  display: flex;
  flex-direction: column;
  background: rgba(13, 32, 51, 0.95);
  border: 1px solid rgba(46, 163, 255, 0.2);
  border-radius: 10px;
  color: #cfe3f5;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(46, 163, 255, 0.14);
  font-size: 14px;
  color: #dbeafe;
}

.panel-head .close {
  cursor: pointer;
  color: #7d97b3;
}

.panel-head .close:hover {
  color: #ffffff;
}

.panel-body {
  padding: 14px 16px 20px;
  overflow-y: auto;
}

.device-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-title h2 {
  margin: 0;
  font-size: 17px;
  color: #ffffff;
}

.risk-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.risk-text {
  font-size: 12px;
  color: #8ba9c4;
}

.muted {
  margin: 6px 0 12px;
  font-size: 12px;
  color: #6f8aa6;
}

.kv {
  margin: 0 0 12px;
  display: grid;
  gap: 8px;
}

.kv div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.kv dt {
  color: #7d97b3;
}

.kv dd {
  margin: 0;
  color: #dbeafe;
  text-align: right;
}

.kv dd.strong {
  font-size: 15px;
  font-weight: 600;
}

.kv dd.pending,
.pending {
  color: #e6a23c;
}

.diagnosis {
  padding-top: 10px;
  border-top: 1px dashed rgba(46, 163, 255, 0.14);
}

.diagnosis h3,
.risk-stats h3 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #dbeafe;
  font-weight: 500;
}

.diagnosis p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #a9c1d8;
}

.risk-reason {
  margin: 14px 0 0;
  font-size: 12px;
  color: #6f8aa6;
  line-height: 1.6;
}

.risk-stats {
  padding-top: 12px;
  border-top: 1px dashed rgba(46, 163, 255, 0.14);
}

.risk-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
}

.risk-row b {
  color: #ffffff;
}

@media (max-width: 900px) {
  .panel {
    width: calc(100% - 36px);
    top: auto;
    bottom: 18px;
    max-height: 46vh;
  }

  .legend-tip {
    display: none;
  }
}
</style>
