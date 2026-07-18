<template>
  <div class="upload-page">
    <div class="upload-container">
      <h2 class="page-title text-gradient">音频分析推荐</h2>
      
      <UploadZone
        :uploading="uploading"
        :upload-progress="uploadProgress"
        @upload="handleUpload"
      />
      
      <div v-if="uploadResult" class="upload-result fade-in">
        <el-alert
          title="上传成功"
          type="success"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>文件已上传，正在跳转到分析页面...</p>
          </template>
        </el-alert>
      </div>
    </div>

    <!-- 测试历史列表 -->
    <div class="history-container fade-in-up">
      <div class="section-header">
        <h3 class="section-title">
          <el-icon><Clock /></el-icon>
          测试历史
        </h3>
        <el-button link type="primary" @click="loadHistory">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div class="history-list">
        <div v-for="item in historyList" :key="item.id" class="history-item">
          <div class="item-left">
            <div class="item-icon">
              <el-icon><Headset /></el-icon>
            </div>
            <div class="item-info">
              <div class="item-name">{{ item.fileName }}</div>
              <div class="item-meta">
                <span class="meta-tag">{{ formatFileSize(item.fileSize) }}</span>
                <span class="meta-separator">•</span>
                <span class="meta-date">{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>
          
          <div class="item-right">
            <el-tag :type="getStatusType(item.status)" class="status-tag" effect="dark" round>
              {{ getStatusText(item.status) }}
            </el-tag>
            
            <div class="item-actions">
              <el-button 
                type="primary" 
                circle
                @click="viewAnalysis(item.id)"
                :disabled="item.status === 'UPLOADING'"
                title="查看分析"
              >
                <el-icon><DataAnalysis /></el-icon>
              </el-button>
              <el-button 
                type="danger" 
                circle
                plain
                @click="deleteRecord(item.id)"
                title="删除记录"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        
        <el-empty v-if="historyList.length === 0" description="暂无测试历史" />
      </div>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
          background
          small
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Refresh, Headset, DataAnalysis, Delete } from '@element-plus/icons-vue'
import UploadZone from '@/components/common/UploadZone.vue'
import { uploadFile as uploadFileAPI, getUploadHistory, deleteUploadRecord as deleteUploadRecordAPI } from '@/api/upload'
import type { UploadRecord } from '@/types/music'

const router = useRouter()
const uploadFile = ref<File | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadResult = ref<any>(null)

// History related refs
const historyList = ref<UploadRecord[]>([])
const loadingHistory = ref(false)
const currentPage = ref(1)
const pageSize = ref(5) // 每页显示5条，避免列表过长
const total = ref(0)

const handleUpload = async (file: File) => {
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const result = await uploadFileAPI(formData, (progress) => {
      uploadProgress.value = Math.round(progress * 100)
    })
    
    uploadResult.value = result
    
    ElMessage.success('上传成功')
    
    // Refresh history
    loadHistory()
    
    setTimeout(() => {
      router.push(`/analysis/${result.uploadId}`)
    }, 1500)
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const loadHistory = async () => {
  loadingHistory.value = true
  try {
    const res = await getUploadHistory(currentPage.value, pageSize.value)
    if (res && (res as any).list) {
      historyList.value = (res as any).list
      total.value = (res as any).total || 0
    } else if (Array.isArray(res)) {
      historyList.value = res
      total.value = res.length
    }
  } catch (error) {
    console.error('Failed to load history', error)
  } finally {
    loadingHistory.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadHistory()
}

const viewAnalysis = (id: number) => {
  router.push(`/analysis/${id}`)
}

const deleteRecord = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    
    await deleteUploadRecordAPI(id)
    ElMessage.success('删除成功')
    
    if (historyList.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    
    loadHistory()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// Format helpers
const formatFileSize = (size: number) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'COMPLETED': 'success',
    'ANALYZING': 'warning',
    'FAILED': 'danger',
    'UPLOADING': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'COMPLETED': '完成',
    'ANALYZING': '分析中',
    'FAILED': '失败',
    'UPLOADING': '上传中'
  }
  return map[status] || status
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 如果是今天，显示具体时间
  if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
    return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.upload-page {
  min-height: calc(100vh - var(--header-height) - var(--footer-height) - 80px);
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 48px;
  
  .upload-container {
    width: 100%;
    max-width: 800px;
    
    .page-title {
      font-size: 36px;
      font-weight: 700;
      text-align: center;
      margin-bottom: 48px;
      font-family: 'Roboto', sans-serif;
    }
    
    .upload-result {
      margin-top: 24px;
    }
  }

  .history-container {
    width: 100%;
    max-width: 800px;
    
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding: 0 8px;

      .section-title {
        font-size: 18px;
        font-weight: 700;
        color: var(--text-dark);
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        
        .el-icon {
          color: var(--primary-color);
        }
      }
    }

    .history-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
      
      .history-item {
        background: white;
        border-radius: 16px;
        padding: 16px 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
        border: 1px solid rgba(0, 0, 0, 0.03);
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
        }
        
        [data-theme="dark"] & {
          background: rgba(30, 41, 59, 0.6);
          border-color: rgba(255, 255, 255, 0.05);
          
          &:hover {
            background: rgba(30, 41, 59, 0.8);
          }
        }
        
        .item-left {
          display: flex;
          align-items: center;
          gap: 16px;
          
          .item-icon {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: rgba(var(--primary-color-rgb), 0.1);
            color: var(--primary-color);
            @include flex-center;
            font-size: 24px;
            
            [data-theme="dark"] & {
              background: rgba(var(--primary-color-rgb), 0.2);
            }
          }
          
          .item-info {
            .item-name {
              font-size: 16px;
              font-weight: 600;
              color: var(--text-dark);
              margin-bottom: 4px;
              max-width: 300px;
              @include text-ellipsis;
            }
            
            .item-meta {
              font-size: 12px;
              color: var(--text-gray);
              display: flex;
              align-items: center;
              
              .meta-separator {
                margin: 0 8px;
                opacity: 0.5;
              }
            }
          }
        }
        
        .item-right {
          display: flex;
          align-items: center;
          gap: 20px;
          
          .status-tag {
            min-width: 60px;
            text-align: center;
          }
          
          .item-actions {
            display: flex;
            gap: 8px;
            opacity: 0;
            transform: translateX(10px);
            transition: all 0.3s;
          }
        }
        
        &:hover .item-right .item-actions {
          opacity: 1;
          transform: translateX(0);
        }
      }
    }

    .pagination-wrapper {
      margin-top: 24px;
      display: flex;
      justify-content: center;
    }
  }
}

.fade-in-up {
  animation: fadeInUp 0.6s ease-out forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
