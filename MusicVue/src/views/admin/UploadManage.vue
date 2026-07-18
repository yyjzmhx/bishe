<template>
  <div class="upload-manage-page">
    <h2 class="page-title text-gradient">上传管理</h2>
    
    <!-- 搜索栏 -->
    <NextCard variant="shadow" hover class="search-card">
      <div class="search-actions">
        <NextInput
          v-model="searchKeyword"
          placeholder="搜索文件名、用户名..."
          :prefix-icon="Search"
          clearable
          style="width: 300px"
          @keyup.enter="loadUploads"
        />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 150px" @change="loadUploads">
          <el-option label="上传中" value="UPLOADING" />
          <el-option label="分析中" value="ANALYZING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 300px"
          @change="handleDateChange"
        />
        <NextButton variant="primary" :icon="Download" @click="handleExport">
          导出数据
        </NextButton>
      </div>
    </NextCard>
    
    <!-- 表格容器 -->
    <NextCard variant="shadow" hover class="table-container">
      <el-table 
        :data="uploadList" 
        v-loading="loading"
        class="custom-table"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            <div class="user-info">
              <span class="username">{{ row.username || '未知' }}</span>
              <span class="nickname" v-if="row.nickname">({{ row.nickname }})</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize || 0) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="100" />
        
        <!-- 状态列 -->
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        
        <!-- 操作栏 -->
        <el-table-column label="操作" width="150" align="right" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="查看详情" placement="top">
                <el-button circle :icon="View" @click="handleView(row)" />
              </el-tooltip>
              <el-tooltip content="下载文件" placement="top">
                <el-button circle :icon="Download" type="success" @click="handleDownload(row)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button circle :icon="Delete" type="danger" @click="handleDelete(row.id)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUploads"
          @current-change="loadUploads"
        />
      </div>
    </NextCard>
    
    <!-- 详情对话框 -->
    <NextModal
      v-model="detailDialogVisible"
      title="上传详情"
      size="lg"
      class="detail-dialog"
    >
      <div v-if="currentRecord" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentRecord.id }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentRecord.username }} ({{ currentRecord.nickname }})</el-descriptions-item>
          <el-descriptions-item label="文件名" :span="2">{{ currentRecord.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatFileSize(currentRecord.fileSize || 0) }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ currentRecord.fileType }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentRecord.status)">
              {{ getStatusText(currentRecord.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatDate(currentRecord.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="文件URL" :span="2">
            <el-link :href="currentRecord.fileUrl" target="_blank" type="primary">
              {{ currentRecord.fileUrl }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="分析结果" :span="2" v-if="currentRecord.analysisResult">
            {{ currentRecord.analysisResult }}
          </el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2" v-if="currentRecord.errorMessage">
            <el-alert :title="currentRecord.errorMessage" type="error" :closable="false" />
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Download, Delete, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextInput from '@/components/ui/NextInput.vue'
import NextModal from '@/components/ui/NextModal.vue'
import NextButton from '@/components/ui/NextButton.vue'
import request from '@/api/request'
import { formatFileSize, formatDate } from '@/utils/format'

const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const uploadList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailDialogVisible = ref(false)
const currentRecord = ref<any>(null)

const loadUploads = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value) params.status = filterStatus.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = formatDate(dateRange.value[0], 'YYYY-MM-DD')
      params.endDate = formatDate(dateRange.value[1], 'YYYY-MM-DD')
    }
    
    const result: any = await request.get('/admin/uploads', { params })
    uploadList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleDateChange = () => {
  loadUploads()
}

const handleView = async (row: any) => {
  try {
    const result: any = await request.get(`/admin/uploads/${row.id}`)
    currentRecord.value = result
    detailDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取详情失败')
  }
}

const handleDownload = (row: any) => {
  if (row.fileUrl) {
    window.open(row.fileUrl, '_blank')
  } else {
    ElMessage.warning('文件URL不存在')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条上传记录吗？删除后文件也将被删除。', '提示', {
      type: 'warning'
    })
    await request.delete(`/admin/uploads/${id}`)
    ElMessage.success('删除成功')
    loadUploads()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'UPLOADING': 'info',
    'ANALYZING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'UPLOADING': '上传中',
    'ANALYZING': '分析中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return statusMap[status] || status
}

onMounted(() => {
  loadUploads()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.upload-manage-page {
  max-width: 1600px;
  margin: 0 auto;
  
  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 24px;
    font-family: 'Roboto', sans-serif;
  }
}

// 搜索栏
.search-header {
  @include glass-card;
  padding: 20px 24px;
  margin-bottom: 24px;
  
  .search-actions {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
    
    .glass-input {
      width: 280px;
      flex-shrink: 0;
      
      :deep(.el-input__wrapper) {
        border-radius: 8px;
        background: #fff;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        border: 1px solid rgba(0, 0, 0, 0.1);
        transition: all 0.2s;
        
        &:hover {
          border-color: rgba(0, 195, 255, 0.3);
          box-shadow: 0 2px 6px rgba(0, 195, 255, 0.1);
        }
        
        &.is-focus {
          border-color: var(--primary-color);
          box-shadow: 0 0 0 3px rgba(0, 195, 255, 0.15);
        }
      }
    }
    
    .glass-select {
      width: 140px;
      flex-shrink: 0;
      
      :deep(.el-input__wrapper) {
        border-radius: 8px;
        background: #fff;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        border: 1px solid rgba(0, 0, 0, 0.1);
        transition: all 0.2s;
        
        &:hover {
          border-color: rgba(0, 195, 255, 0.3);
          box-shadow: 0 2px 6px rgba(0, 195, 255, 0.1);
        }
        
        &.is-focus {
          border-color: var(--primary-color);
          box-shadow: 0 0 0 3px rgba(0, 195, 255, 0.15);
        }
      }
    }
    
    .glass-date-picker {
      width: 300px;
      flex-shrink: 0;
      
      :deep(.el-input__wrapper) {
        border-radius: 8px;
        background: #fff;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        border: 1px solid rgba(0, 0, 0, 0.1);
        transition: all 0.2s;
        
        &:hover {
          border-color: rgba(0, 195, 255, 0.3);
          box-shadow: 0 2px 6px rgba(0, 195, 255, 0.1);
        }
        
        &.is-focus {
          border-color: var(--primary-color);
          box-shadow: 0 0 0 3px rgba(0, 195, 255, 0.15);
        }
      }
    }
    
    .export-btn {
      background: var(--primary-gradient);
      border: none;
      border-radius: 8px;
      padding: 10px 20px;
      color: #fff;
      font-weight: 500;
      transition: all 0.2s;
      box-shadow: 0 2px 6px rgba(0, 195, 255, 0.25);
      
      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 195, 255, 0.35);
      }
    }
  }
}

// 表格容器
.table-container {
  @include glass-card;
  padding: 20px;
  @include hover-table;
  
  .user-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .username {
      font-weight: 600;
      color: var(--text-dark);
    }
    
    .nickname {
      font-size: 12px;
      color: var(--text-gray);
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 0 0;
  }
}

.detail-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  .detail-content {
    :deep(.el-descriptions__label) {
      font-weight: 600;
      color: var(--text-dark);
    }
  }
}
</style>

