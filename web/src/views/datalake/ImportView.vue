<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { importFile } from '@/api/datalake'
import type { ImportResult } from '@/types/datalake'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const file = ref<File | null>(null)
const fileName = ref('')
const uploading = ref(false)
const result = ref<ImportResult | null>(null)

function onChange(uploadFile: UploadFile): void {
  if (!uploadFile.raw) {
    return
  }
  file.value = uploadFile.raw
  fileName.value = uploadFile.name
  result.value = null
}

async function submit(): Promise<void> {
  if (!file.value) {
    ElMessage.warning('请选择 CSV 或 Excel 文件')
    return
  }
  uploading.value = true
  try {
    result.value = await importFile(file.value)
    ElMessage.success(`导入完成：成功 ${result.value.successRows} 行`)
  } finally {
    uploading.value = false
  }
}

function reset(): void {
  file.value = null
  fileName.value = ''
  result.value = null
}
</script>

<template>
  <div class="datalake-page">
    <el-card shadow="never">
      <template #header><span class="card-title">Excel / CSV 导入</span></template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="文件列顺序：仪表编号、采集时间（yyyy-MM-dd HH:mm:ss）、读数、读数类型（1 累计读数 / 2 周期增量，可选）"
        description="首行表头可省略；导入按批次幂等，失败行写入失败记录，不丢弃原始内容。"
      />

      <div class="upload-row">
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept=".csv,.xlsx,.xls"
          :on-change="onChange"
          :on-exceed="() => ElMessage.warning('一次只能导入一个文件')"
        >
          <el-button>选择文件</el-button>
        </el-upload>
        <span v-if="fileName" class="file-name">{{ fileName }}</span>
        <el-button v-if="userStore.hasPermission('datalake:import')" type="primary" :loading="uploading" @click="submit">
          开始导入
        </el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-alert v-if="result" :type="result.failedRows > 0 ? 'warning' : 'success'" :closable="false" show-icon class="result">
        <template #title>
          批次 {{ result.batchNo }}：共 {{ result.totalRows }} 行，成功 {{ result.successRows }}，失败 {{ result.failedRows }}
        </template>
      </el-alert>

      <el-table v-if="result && result.errors.length" :data="result.errors" border class="errors" max-height="260">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="解析错误">
          <template #default="{ row }">{{ row }}</template>
        </el-table-column>
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

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0;
  flex-wrap: wrap;
}

.file-name {
  color: #606266;
  font-size: 13px;
}

.result {
  margin-top: 8px;
}

.errors {
  margin-top: 16px;
}
</style>
