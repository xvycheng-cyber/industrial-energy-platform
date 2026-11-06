import type { EChartsOption } from 'echarts'

export const CHART_COLORS = [
  '#2563eb',
  '#0ea5e9',
  '#f59e0b',
  '#dc2626',
  '#10b981',
  '#7c3aed',
  '#64748b',
  '#0891b2'
]

export const CHART_FONT = "'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif"

const LIGHT_AXIS = {
  axisLine: { lineStyle: { color: '#e2e8f0' } },
  axisTick: { show: false },
  axisLabel: { color: '#64748b', fontSize: 11 },
  splitLine: { lineStyle: { color: '#eef2f7', type: 'dashed' as const } }
}

/**
 * 统一图表基础主题：字体、配色、坐标轴、提示框、图例。
 * 页面传入的 option 优先，仅补齐未设置的项。
 */
export function withChartTheme(option: EChartsOption, dark = false): EChartsOption {
  const text = dark ? '#c7d9ec' : '#334155'
  const muted = dark ? '#7f9ab5' : '#94a3b8'
  const line = dark ? 'rgba(125, 176, 224, 0.22)' : '#e2e8f0'
  const split = dark ? 'rgba(125, 176, 224, 0.10)' : '#eef2f7'

  const axis = {
    axisLine: { lineStyle: { color: line } },
    axisTick: { show: false },
    axisLabel: { color: muted, fontSize: 11 },
    splitLine: { show: true, lineStyle: { color: split, type: 'dashed' as const } }
  }

  return {
    color: CHART_COLORS,
    textStyle: { fontFamily: CHART_FONT, fontSize: 12, color: text },
    animationDuration: 300,
    animationEasing: 'cubicOut',
    grid: { left: 12, right: 16, top: 36, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: dark ? 'rgba(8, 26, 43, 0.94)' : 'rgba(255, 255, 255, 0.98)',
      borderColor: dark ? 'rgba(120, 169, 216, 0.28)' : '#e2e8f0',
      borderWidth: 1,
      padding: [8, 12],
      textStyle: { color: dark ? '#e2e8f0' : '#0f172a', fontSize: 12 },
      axisPointer: {
        type: 'line',
        lineStyle: { color: dark ? 'rgba(125, 176, 224, 0.45)' : '#cbd5e1', type: 'dashed' }
      }
    },
    legend: {
      top: 0,
      right: 0,
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 14,
      textStyle: { color: muted, fontSize: 11 }
    },
    ...option,
    // 仅当页面未显式提供时补齐坐标轴样式
    xAxis: mergeAxis(option.xAxis, axis, dark) as EChartsOption['xAxis'],
    yAxis: mergeAxis(option.yAxis, axis, dark) as EChartsOption['yAxis']
  }
}

type AnyObject = Record<string, any>

function mergeAxis(value: unknown, base: Record<string, unknown>, dark: boolean): unknown {
  if (!value) {
    return {
      ...base,
      axisLabel: { ...(base.axisLabel as AnyObject), color: dark ? '#7f9ab5' : '#64748b' }
    }
  }
  const apply = (item: AnyObject) => ({
    ...base,
    ...item,
    axisLine: { ...(base.axisLine as AnyObject), ...(item.axisLine ?? {}) },
    axisLabel: { ...(base.axisLabel as AnyObject), ...(item.axisLabel ?? {}) },
    splitLine: { ...(base.splitLine as AnyObject), ...(item.splitLine ?? {}) }
  })
  return Array.isArray(value) ? value.map((item) => apply(item as AnyObject)) : apply(value as AnyObject)
}

export const PALETTE = {
  baseline: '#f59e0b',
  actual: '#2563eb',
  saving: '#10b981',
  anomaly: '#dc2626',
  normal: '#16a34a',
  muted: '#94a3b8'
}

export const LIGHT_AXIS_STYLE = LIGHT_AXIS
