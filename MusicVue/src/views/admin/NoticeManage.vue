<template>
  <div class="notice-manage-page">
    <h2 class="page-title text-gradient">通知公告管理</h2>
    
    <!-- 搜索栏 -->
    <NextCard variant="shadow" hover class="search-card">
      <div class="search-actions">
        <NextInput
          v-model="searchKeyword"
          placeholder="搜索标题、内容..."
          :prefix-icon="Search"
          clearable
          style="width: 300px"
          @keyup.enter="loadNotices"
        />
        <el-select v-model="filterType" placeholder="类型" clearable style="width: 150px" @change="loadNotices">
          <el-option label="系统" value="SYSTEM" />
          <el-option label="更新" value="UPDATE" />
          <el-option label="新歌" value="NEW_MUSIC" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 150px" @change="loadNotices">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
        <NextButton variant="primary" :icon="Plus" @click="handleAdd">添加公告</NextButton>
      </div>
    </NextCard>
    
    <!-- 表格容器 -->
    <NextCard variant="shadow" hover class="table-container">
      <el-table 
        :data="noticeList" 
        v-loading="loading"
        class="custom-table"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="{ row }">
            {{ row.publishTime ? formatDate(row.publishTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        
        <!-- 操作栏 -->
        <el-table-column label="操作" width="200" align="right" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="编辑" placement="top">
                <el-button circle :icon="Edit" @click="handleEdit(row)" />
              </el-tooltip>
              <el-tooltip :content="row.status === 1 ? '取消发布' : '发布'" placement="top">
                <el-button 
                  circle 
                  :icon="row.status === 1 ? Close : Promotion" 
                  :type="row.status === 1 ? 'warning' : 'success'"
                  @click="handleTogglePublish(row)" 
                />
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
          @size-change="loadNotices"
          @current-change="loadNotices"
        />
      </div>
    </NextCard>
    
    <!-- 添加/编辑对话框 -->
    <NextModal
      v-model="editDialogVisible"
      :title="editForm.id ? '编辑公告' : '添加公告'"
      size="lg"
      :mask-closable="false"
      class="notice-edit-dialog"
    >
      <el-form :model="editForm" label-width="100px" ref="noticeFormRef">
        <el-form-item label="标题" prop="title" required>
          <NextInput v-model="editForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type" required>
          <el-select v-model="editForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="系统" value="SYSTEM" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="新歌" value="NEW_MUSIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content" required>
          <NextInput
            v-model="editForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="0">草稿</el-radio>
            <el-radio :value="1">发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="handleCancelEdit">取消</el-button>
        <NextButton variant="primary" @click="handleSaveEdit" :loading="saving">
          {{ saving ? '保存中...' : '保存' }}
        </NextButton>
      </template>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete, Close, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElForm } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextInput from '@/components/ui/NextInput.vue'
import NextModal from '@/components/ui/NextModal.vue'
import NextButton from '@/components/ui/NextButton.vue'
import request from '@/api/request'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref<number | null>(null)
const noticeList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editDialogVisible = ref(false)
const noticeFormRef = ref<InstanceType<typeof ElForm>>()

const editForm = reactive({
  id: 0,
  title: '',
  type: 'SYSTEM',
  content: '',
  status: 0
})

const loadNotices = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value) params.type = filterType.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    
    const result: any = await request.get('/admin/notices', { params })
    noticeList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  Object.assign(editForm, {
    id: 0,
    title: '',
    type: 'SYSTEM',
    content: '',
    status: 0
  })
  editDialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(editForm, {
    id: row.id,
    title: row.title,
    type: row.type,
    content: row.content,
    status: row.status
  })
  editDialogVisible.value = true
}

const handleSaveEdit = async () => {
  if (!noticeFormRef.value) return
  
  await noticeFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写完整信息')
      return
    }
    
    saving.value = true
    try {
      if (editForm.id) {
        await request.put(`/admin/notices/${editForm.id}`, editForm)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/notices', editForm)
        ElMessage.success('添加成功')
      }
      editDialogVisible.value = false
      handleCancelEdit()
      loadNotices()
    } catch (error: any) {
      ElMessage.error(error.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
  noticeFormRef.value?.resetFields()
}

const handleTogglePublish = async (row: any) => {
  try {
    const action = row.status === 1 ? '取消发布' : '发布'
    await ElMessageBox.confirm(`确定要${action}这条公告吗？`, '提示', {
      type: 'warning'
    })
    
    await request.put(`/admin/notices/${row.id}/publish`, {
      publish: row.status === 0
    })
    ElMessage.success(`${action}成功`)
    loadNotices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/admin/notices/${id}`)
    ElMessage.success('删除成功')
    loadNotices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const getTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    'SYSTEM': 'info',
    'UPDATE': 'warning',
    'NEW_MUSIC': 'success'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'SYSTEM': '系统',
    'UPDATE': '更新',
    'NEW_MUSIC': '新歌'
  }
  return typeMap[type] || type
}

onMounted(() => {
  loadNotices()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.notice-manage-page {
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
.search-card {
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
    
    .add-btn {
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
  padding: 20px;
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 0 0;
  }
}

.notice-edit-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--text-dark);
  }
  
  :deep(.el-input__wrapper), :deep(.el-select__wrapper) {
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    border: 1px solid rgba(0, 0, 0, 0.08);
    
    &:hover {
      border-color: rgba(0, 195, 255, 0.2);
    }
    
    &.is-focus {
      border-color: var(--primary-color);
      box-shadow: 0 0 0 2px rgba(0, 195, 255, 0.1);
    }
  }
}
</style>

