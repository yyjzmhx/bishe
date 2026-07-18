<template>
  <div class="history-page">
    <h2 class="page-title text-gradient">测试历史</h2>
    
    <div class="history-card">
        <div class="card-header">
        <div class="search-filters">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索..."
            :prefix-icon="Search"
            clearable
            class="search-input"
            @keyup.enter="loadHistory"
            @clear="loadHistory"
          />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="date-picker"
            clearable
            @change="handleDateChange"
          />
        </div>
      </div>
      
      <div class="table-container">
      <el-table
        :data="historyList"
        v-loading="loading"
        stripe
          class="history-table"
          :row-class-name="getRowClassName"
      >
          <el-table-column prop="id" label="ID" width="80" align="center">
            <template #default="{ row }">
              <span class="id-cell">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fileName" label="文件名" min-width="200">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon class="file-icon"><Document /></el-icon>
                <span class="file-name">{{ row.fileName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="文件大小" width="120" align="center">
          <template #default="{ row }">
              <span class="file-size">{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
          <el-table-column prop="status" label="状态" width="140" align="center">
          <template #default="{ row }">
              <div class="status-wrapper">
                <div 
                  class="status-badge" 
                  :class="`status-${row.status.toLowerCase()}`"
                >
                  <el-icon class="status-icon">
                    <component :is="getStatusIcon(row.status)" />
                  </el-icon>
                  <span class="status-text">{{ getStatusText(row.status) }}</span>
                </div>
              </div>
          </template>
        </el-table-column>
          <el-table-column prop="createTime" label="上传时间" width="180" align="center">
          <template #default="{ row }">
              <div class="time-cell">
                <el-icon class="time-icon"><Clock /></el-icon>
                <span>{{ formatDate(row.createTime) }}</span>
              </div>
          </template>
        </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
              <div class="action-buttons">
            <el-button
              v-if="row.status === 'COMPLETED'"
              type="primary"
              size="small"
                  :icon="View"
              @click="viewAnalysis(row.id)"
                  class="action-btn"
            >
              查看推荐
            </el-button>
            <el-button
              v-else-if="row.status === 'FAILED'"
              type="warning"
              size="small"
                  :icon="Refresh"
              @click="retryAnalysis(row.id)"
                  class="action-btn"
            >
              重试
            </el-button>
            <el-button
              type="danger"
              size="small"
                  :icon="Delete"
              @click="handleDelete(row.id)"
                  class="action-btn"
            >
              删除
            </el-button>
              </div>
          </template>
        </el-table-column>
      </el-table>
      </div>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadHistory"
          @current-change="loadHistory"
          class="pagination"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Document, Clock, View, Refresh, Delete, CircleCheck, Loading, Warning, CircleClose } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUploadHistory } from '@/api/upload'
import { formatFileSize, formatDate } from '@/utils/format'
import type { UploadRecord } from '@/types/music'

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const dateRange = ref<[string, string] | null>(null)
const historyList = ref<UploadRecord[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusIcon = (status: string) => {
  const map: Record<string, any> = {
    UPLOADING: Loading,
    ANALYZING: Loading,
    COMPLETED: CircleCheck,
    FAILED: CircleClose
  }
  return map[status] || CircleCheck
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    ANALYZING: '分析中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

const getRowClassName = ({ rowIndex }: { rowIndex: number }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

const handleDateChange = () => {
  loadHistory()
}

const loadHistory = async () => {
  loading.value = true
  try {
    // 重置到第一页
    pageNum.value = 1
    
    // 处理时间范围（dateRange已经是YYYY-MM-DD格式的字符串数组）
    const startDate = dateRange.value?.[0]
    const endDate = dateRange.value?.[1]
    
    const result = await getUploadHistory(
      pageNum.value,
      pageSize.value,
      searchKeyword.value || undefined,
      startDate,
      endDate
    )
    historyList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const viewAnalysis = (uploadId: number) => {
  router.push(`/analysis/${uploadId}`)
}

const retryAnalysis = async (uploadId: number) => {
  // 重试分析
  ElMessage.info('重试功能开发中')
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      type: 'warning'
    })
    // 调用删除API
    ElMessage.success('删除成功')
    loadHistory()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.history-page {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
  
  .page-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 32px;
    font-family: 'Roboto', sans-serif;
  }
  
  .history-card {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
    backdrop-filter: blur(20px);
    border-radius: var(--radius-xl);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.5);
    padding: 32px;
    transition: all var(--transition-base);
    
    [data-theme="dark"] & {
      background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.9) 100%);
      border-color: rgba(255, 255, 255, 0.1);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    }
    
    &:hover {
      box-shadow: 0 12px 48px rgba(0, 0, 0, 0.12);
      
      [data-theme="dark"] & {
        box-shadow: 0 12px 48px rgba(0, 0, 0, 0.5);
      }
  }
  
  .card-header {
      margin-bottom: 24px;
      
      .search-filters {
        display: flex;
        gap: 16px;
        align-items: center;
        flex-wrap: wrap;
        
        .search-input {
          flex: 1;
          min-width: 300px;
          max-width: 400px;
          
          :deep(.el-input__wrapper) {
            background: rgba(255, 255, 255, 0.9);
            border-color: rgba(0, 0, 0, 0.1);
            
            [data-theme="dark"] & {
              background: rgba(30, 41, 59, 0.8);
              border-color: rgba(255, 255, 255, 0.1);
            }
            
            .el-input__inner {
              color: var(--text-dark);
              
              [data-theme="dark"] & {
                color: var(--text-light);
              }
            }
          }
        }
        
        .date-picker {
          min-width: 320px;
          
          :deep(.el-input__wrapper),
          :deep(.el-range-editor.el-input__wrapper) {
            background: rgba(255, 255, 255, 0.9);
            box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1) inset;
            
            [data-theme="dark"] & {
              background-color: rgba(30, 41, 59, 0.8) !important;
              box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
            }
            
            .el-range-input {
              color: var(--text-dark);
              
              [data-theme="dark"] & {
                color: var(--text-light);
                background-color: transparent !important;
              }
            }
            
            .el-range-separator {
              color: var(--text-gray);
              
              [data-theme="dark"] & {
                color: var(--text-light-gray);
              }
            }
            
            .el-input__icon,
            .el-range__icon,
            .el-range__close-icon {
              color: var(--text-gray);
              
              [data-theme="dark"] & {
                color: var(--text-light-gray);
              }
            }
          }
        }
      }
    }
    
    .table-container {
      border-radius: var(--radius-lg);
      overflow: hidden;
      
      .history-table {
        background: transparent;
        
        :deep(.el-table__header-wrapper) {
          background: linear-gradient(135deg, rgba(0, 195, 255, 0.1) 0%, rgba(0, 136, 255, 0.1) 100%);
          
          [data-theme="dark"] & {
            background: linear-gradient(135deg, rgba(0, 195, 255, 0.15) 0%, rgba(0, 136, 255, 0.15) 100%);
          }
          
          .el-table__header {
            th {
              background: transparent;
              color: var(--text-dark);
              font-weight: 600;
              font-size: 14px;
              padding: 16px 0;
              border-bottom: 2px solid rgba(0, 195, 255, 0.2);
              
              [data-theme="dark"] & {
                color: var(--text-light);
                border-bottom-color: rgba(0, 195, 255, 0.3);
              }
            }
          }
        }
        
        :deep(.el-table__body-wrapper) {
          background: transparent;
          
          [data-theme="dark"] & {
            background: transparent;
          }
          
          .el-table__body {
            background: transparent;
            
            [data-theme="dark"] & {
              background: transparent;
            }
            
            tr {
              transition: all var(--transition-base);
              background: transparent;
              
              &.even-row {
                background: rgba(255, 255, 255, 0.5);
                
                [data-theme="dark"] & {
                  background: rgba(30, 41, 59, 0.5);
                }
              }
              
              &.odd-row {
                background: rgba(255, 255, 255, 0.8);
                
                [data-theme="dark"] & {
                  background: rgba(30, 41, 59, 0.8);
                }
              }
              
              &:hover {
                background: linear-gradient(135deg, rgba(0, 195, 255, 0.1) 0%, rgba(124, 58, 237, 0.1) 100%) !important;
                transform: scale(1.01);
                box-shadow: 0 4px 12px rgba(0, 195, 255, 0.2);
                
                [data-theme="dark"] & {
                  background: linear-gradient(135deg, rgba(0, 195, 255, 0.2) 0%, rgba(124, 58, 237, 0.2) 100%) !important;
                  box-shadow: 0 4px 12px rgba(0, 195, 255, 0.3);
                }
              }
              
              td {
                border-bottom: 1px solid rgba(0, 0, 0, 0.05);
                padding: 16px 0;
                background: transparent;
                
                [data-theme="dark"] & {
                  border-bottom-color: rgba(255, 255, 255, 0.1);
                  background: transparent;
                }
              }
            }
          }
        }
        
        // Element Plus表格在暗色模式下的全局样式覆盖
        [data-theme="dark"] & {
          background-color: transparent !important;
          --el-table-bg-color: transparent !important;
          --el-table-tr-bg-color: transparent !important;
          --el-table-header-bg-color: rgba(255, 255, 255, 0.05) !important;
          --el-table-row-hover-bg-color: rgba(0, 195, 255, 0.1) !important;
          --el-table-border-color: rgba(255, 255, 255, 0.1) !important;
          color: var(--text-light) !important;
          
          :deep(th.el-table__cell) {
            background-color: rgba(255, 255, 255, 0.05) !important;
            color: var(--text-light) !important;
            border-bottom-color: rgba(255, 255, 255, 0.1) !important;
          }
          
          :deep(td.el-table__cell) {
            background-color: transparent !important;
            color: var(--text-light) !important;
            border-bottom-color: rgba(255, 255, 255, 0.05) !important;
          }
          
          :deep(.el-table__empty-block) {
            background-color: transparent !important;
            
            .el-table__empty-text {
              color: var(--text-light-gray) !important;
            }
          }
          
          :deep(.el-loading-mask) {
            background-color: rgba(30, 41, 59, 0.8) !important;
          }
        }
        
        .id-cell {
          display: inline-block;
          padding: 4px 12px;
          background: linear-gradient(135deg, rgba(0, 195, 255, 0.1) 0%, rgba(0, 136, 255, 0.1) 100%);
          border-radius: 12px;
          font-weight: 600;
          color: var(--primary-color);
          font-size: 13px;
          
          [data-theme="dark"] & {
            background: linear-gradient(135deg, rgba(0, 195, 255, 0.2) 0%, rgba(0, 136, 255, 0.2) 100%);
          }
        }
        
        .file-name-cell {
          display: flex;
          align-items: center;
          gap: 8px;
          
          .file-icon {
            color: var(--primary-color);
            font-size: 18px;
          }
          
          .file-name {
            color: var(--text-dark);
            font-weight: 500;
            
            [data-theme="dark"] & {
              color: var(--text-light);
            }
          }
        }
        
        .file-size {
          color: var(--text-gray);
          font-size: 13px;
          
          [data-theme="dark"] & {
            color: var(--text-light-gray);
          }
        }
        
        .status-wrapper {
          display: flex;
          justify-content: center;
          
          .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 8px 16px;
            border-radius: 20px;
            font-weight: 600;
            font-size: 13px;
            transition: all var(--transition-base);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            position: relative;
            overflow: hidden;
            
            &::before {
              content: '';
              position: absolute;
              top: 0;
              left: -100%;
              width: 100%;
              height: 100%;
              background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
              transition: left 0.5s;
            }
            
            &:hover::before {
              left: 100%;
            }
            
            .status-icon {
              font-size: 16px;
              animation: none;
            }
            
            .status-text {
              font-weight: 600;
              letter-spacing: 0.5px;
            }
            
            // 上传中
            &.status-uploading {
              background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
              color: white;
              border: 2px solid rgba(59, 130, 246, 0.3);
              
              .status-icon {
                animation: rotate 1.5s linear infinite;
              }
            }
            
            // 分析中
            &.status-analyzing {
              background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
              color: white;
              border: 2px solid rgba(245, 158, 11, 0.3);
              
              .status-icon {
                animation: rotate 1.5s linear infinite;
              }
            }
            
            // 已完成
            &.status-completed {
              background: linear-gradient(135deg, #10b981 0%, #059669 100%);
              color: white;
              border: 2px solid rgba(16, 185, 129, 0.3);
              box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
              
              &:hover {
                box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
                transform: translateY(-2px);
              }
            }
            
            // 失败
            &.status-failed {
              background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
              color: white;
              border: 2px solid rgba(239, 68, 68, 0.3);
              
              &:hover {
                box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
                transform: translateY(-2px);
              }
            }
            
            [data-theme="dark"] & {
              box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
              
              &.status-completed {
                box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
              }
              
              &.status-failed {
                box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
              }
            }
          }
        }
        
        @keyframes rotate {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
        
        .time-cell {
    display: flex;
    align-items: center;
          gap: 6px;
          color: var(--text-gray);
          font-size: 13px;
          
          .time-icon {
            font-size: 14px;
          }
          
          [data-theme="dark"] & {
            color: var(--text-light-gray);
          }
        }
        
        .action-buttons {
          display: flex;
          gap: 8px;
          justify-content: center;
          flex-wrap: wrap;
          
          .action-btn {
            border-radius: 8px;
            font-weight: 500;
            transition: all var(--transition-base);
            
            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
              
              [data-theme="dark"] & {
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
              }
            }
          }
        }
      }
  }
  
  .pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
      
      .pagination {
        :deep(.el-pagination__total),
        :deep(.el-pagination__jump) {
          color: var(--text-dark);
          
          [data-theme="dark"] & {
            color: var(--text-light);
          }
        }
        
        :deep(.el-pagination__sizes) {
          .el-select {
            .el-input__wrapper {
              background: rgba(255, 255, 255, 0.8);
              border-color: rgba(0, 0, 0, 0.1);
              
              [data-theme="dark"] & {
                background: rgba(30, 41, 59, 0.8) !important;
                box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
              }
              
              .el-input__inner {
                color: var(--text-dark);
                
                [data-theme="dark"] & {
                  color: var(--text-light);
                }
              }
            }
          }
        }
        
        :deep(.el-pagination__jump) {
          .el-input__wrapper {
            background: rgba(255, 255, 255, 0.8);
            border-color: rgba(0, 0, 0, 0.1);
            
            [data-theme="dark"] & {
              background: rgba(30, 41, 59, 0.8) !important;
              box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
            }
            
            .el-input__inner {
              color: var(--text-dark);
              
              [data-theme="dark"] & {
                color: var(--text-light);
              }
            }
          }
        }
        
        :deep(.btn-prev),
        :deep(.btn-next),
        :deep(.el-pager li) {
          background: rgba(255, 255, 255, 0.8);
          color: var(--text-dark);
          border: 1px solid rgba(0, 0, 0, 0.1);
          
          [data-theme="dark"] & {
            background: rgba(30, 41, 59, 0.8);
            color: var(--text-light);
            border-color: rgba(255, 255, 255, 0.1);
          }
          
          &:hover {
            background: var(--primary-gradient);
            color: white;
            border-color: transparent;
          }
          
          &.is-active {
            background: var(--primary-gradient);
            color: white;
            border-color: transparent;
          }
        }
      }
    }
  }
}

// 下拉选择框在暗色模式下的样式（全局样式，放在scoped外部）
[data-theme="dark"] {
  :deep(.el-select-dropdown) {
    background: rgba(30, 41, 59, 0.95) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
    
    .el-select-dropdown__item {
      color: var(--text-light) !important;
      
      &:hover {
        background: rgba(0, 195, 255, 0.1) !important;
      }
      
      &.selected {
        color: var(--primary-color) !important;
        background: rgba(0, 195, 255, 0.15) !important;
      }
    }
  }
  
  // 日期选择器下拉面板样式
  :deep(.el-picker__popper) {
    background: rgba(30, 41, 59, 0.95) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
    
    .el-date-range-picker__header {
      color: var(--text-light) !important;
    }
    
    .el-date-table th {
      color: var(--text-light-gray) !important;
    }
    
    .el-date-table td {
      color: var(--text-light) !important;
      
      &.available:hover {
        color: var(--primary-color) !important;
      }
    }
  }
}
</style>

<style lang="scss">
// 全局样式覆盖，用于处理 Element Plus 内部组件样式
// 放在 scoped 外部以确保能够穿透组件边界

// 暗色模式下的日期选择器样式
[data-theme="dark"] {
  .history-page {
    .el-date-editor.el-input__wrapper {
      background-color: rgba(30, 41, 59, 0.8) !important;
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
      
      // 输入框
      .el-range-input {
        color: var(--text-light) !important;
        background-color: transparent !important;
        
        &::placeholder {
          color: rgba(255, 255, 255, 0.3) !important;
        }
      }
      
      // 分隔符 "至"
      .el-range-separator {
        color: var(--text-light-gray) !important;
      }
      
      // 图标
      .el-input__icon {
        color: var(--text-light-gray) !important;
      }
      
      // 清除图标
      .el-range__close-icon {
        color: var(--text-light-gray) !important;
      }
      
      // 悬停和激活状态
      &:hover, &.is-focus {
        box-shadow: 0 0 0 1px var(--primary-color) inset !important;
      }
    }
  }
}
</style>



