<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'
import { withChartTheme } from '@/utils/chartTheme'

const props = withDefaults(
  defineProps<{
    option: echarts.EChartsOption
    height?: string
    dark?: boolean
    loading?: boolean
    empty?: boolean
    emptyText?: string
  }>(),
  { height: '300px', dark: false, loading: false, empty: false, emptyText: '暂无数据' }
)

const emit = defineEmits<{ (e: 'chart-click', params: unknown): void }>()

const container = ref<HTMLDivElement>()
const chart = shallowRef<echarts.ECharts>()
let observer: ResizeObserver | undefined

function render(): void {
  if (!chart.value || !props.option) {
    return
  }
  chart.value.setOption(withChartTheme(props.option, props.dark), true)
}

onMounted(() => {
  if (!container.value) {
    return
  }
  chart.value = echarts.init(container.value, undefined, { renderer: 'canvas' })
  chart.value.setOption(withChartTheme(props.option, props.dark), true)
  chart.value.on('click', (params) => emit('chart-click', params))
  observer = new ResizeObserver(() => chart.value?.resize())
  observer.observe(container.value)
})

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

watch(
  () => props.loading,
  (value) => {
    if (!chart.value) {
      return
    }
    if (value) {
      chart.value.showLoading('default', {
        text: '加载中…',
        color: '#2563eb',
        textColor: '#64748b',
        maskColor: props.dark ? 'rgba(7, 26, 43, 0.35)' : 'rgba(255, 255, 255, 0.65)',
        zlevel: 0
      })
    } else {
      chart.value.hideLoading()
    }
  }
)

watch(
  () => props.dark,
  () => {
    chart.value?.dispose()
    if (container.value) {
      chart.value = echarts.init(container.value, undefined, { renderer: 'canvas' })
      chart.value.on('click', (params) => emit('chart-click', params))
      render()
    }
  }
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart.value?.dispose()
  chart.value = undefined
})
</script>

<template>
  <div class="base-chart-wrap" :style="{ height }">
    <el-empty v-if="empty" :description="emptyText" :image-size="72" class="chart-empty" />
    <div v-show="!empty" ref="container" class="base-chart"></div>
  </div>
</template>

<style scoped>
.base-chart-wrap {
  position: relative;
  width: 100%;
}

.base-chart {
  width: 100%;
  height: 100%;
}

.chart-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>
