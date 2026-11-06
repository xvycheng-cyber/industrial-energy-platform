<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listRules, toggleRule, updateRule } from '@/api/diagnosis'
import type { DiagnosisRule } from '@/types/diagnosis'

const loading = ref(false)
const saving = ref(false)
const rules = ref<DiagnosisRule[]>([])
const dialogVisible = ref(false)
const editing = ref<DiagnosisRule | null>(null)
const form = reactive({ conditionText: '', priority: 100, weight: 1, remark: '' })

async function load(): Promise<void> {
  loading.value = true
  try {
    rules.value = await listRules()
  } finally {
    loading.value = false
  }
}

function openEdit(rule: DiagnosisRule): void {
  editing.value = rule
  form.conditionText = JSON.stringify({ ...rule.condition, ...rule.threshold }, null, 2)
  form.priority = rule.priority
  form.weight = rule.weight
  form.remark = rule.remark ?? ''
  dialogVisible.value = true
}

async function save(): Promise<void> {
  if (!editing.value) return
  let parsed: Record<string, unknown>
  try {
    parsed = JSON.parse(form.conditionText)
  } catch {
    ElMessage.error('规则 JSON 格式不正确')
    return
  }
  saving.value = true
  try {
    await updateRule(editing.value.id, {
      condition: parsed,
      threshold: parsed,
      priority: form.priority,
      weight: form.weight,
      remark: form.remark
    })
    ElMessage.success('规则已保存，版本号已递增')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onToggle(rule: DiagnosisRule, value: boolean): Promise<void> {
  await toggleRule(rule.id, value)
  ElMessage.success(value ? '规则已启用' : '规则已停用')
  await load()
}

function summary(condition: Record<string, unknown>): string {
  return Object.entries(condition)
    .map(([key, value]) => `${key}=${Array.isArray(value) ? value.join('/') : value}`)
    .join('，')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>诊断规则</h2>
          <p class="muted">规则以结构化 JSON 表达，仅允许白名单字段；保存后版本号递增并立即生效。</p>
        </div>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rules" border stripe>
        <el-table-column prop="ruleCode" label="编码" width="80" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column label="条件参数" min-width="300">
          <template #default="{ row }">
            <span class="condition">{{ summary({ ...row.condition, ...row.threshold }) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === 1" @change="(value: string | number | boolean) => onToggle(row, Boolean(value))" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无规则" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑规则" width="680px">
      <el-form label-width="110px">
        <el-form-item label="规则">
          <span>{{ editing?.ruleCode }} · {{ editing?.ruleName }}</span>
        </el-form-item>
        <el-form-item label="条件 JSON">
          <el-input v-model="form.conditionText" type="textarea" :rows="12" spellcheck="false" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="999" />
          <span class="hint">数值越小越先执行</span>
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="form.weight" :min="0.1" :max="10" :step="0.1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 14px; }
.head h2 { margin: 0; font-size: 20px; color: #0f2a43; }
.muted { margin: 6px 0 0; color: #909399; font-size: 12px; }
.condition { font-size: 12px; color: #606266; }
.hint { margin-left: 10px; font-size: 12px; color: #909399; }
</style>
