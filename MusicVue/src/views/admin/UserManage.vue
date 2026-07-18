<template>
  <div class="user-manage-page">
    <h2 class="page-title text-gradient">用户管理</h2>
    
    <NextCard variant="shadow" hover>
      <template #header>
        <div class="card-header">
          <NextInput
            v-model="searchKeyword"
            placeholder="搜索用户名、手机号、邮箱..."
            :prefix-icon="Search"
            clearable
            style="width: 300px"
            @keyup.enter="loadUsers"
          />
          <div class="header-actions">
            <el-select v-model="filterRole" placeholder="角色" clearable style="width: 120px" @change="loadUsers">
              <el-option label="用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px" @change="loadUsers">
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </div>
        </div>
      </template>
      
      <NextTable
        :data="userList"
        :columns="tableColumns"
        :loading="loading"
        stripe
        hover
        :pagination="{ pageSize, currentPage: pageNum, total }"
        @row-click="handleRowClick"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      >
        <template #column-role="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
            {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
        </template>
        
        <template #column-status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
        
        <template #column-createTime="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
        
        <template #column-actions="{ row }">
          <div class="action-buttons">
            <el-button type="primary" size="small" @click.stop="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click.stop="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click.stop="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </NextTable>
    </NextCard>
    
    <!-- 编辑对话框 -->
    <NextModal v-model="editDialogVisible" title="编辑用户" size="md">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="昵称">
          <NextInput v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <NextInput v-model="editForm.phone" type="number" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <NextInput v-model="editForm.email" type="email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <NextButton variant="primary" @click="handleSaveEdit">保存</NextButton>
      </template>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextInput from '@/components/ui/NextInput.vue'
import NextTable, { type TableColumn } from '@/components/ui/NextTable.vue'
import NextModal from '@/components/ui/NextModal.vue'
import NextButton from '@/components/ui/NextButton.vue'
import request from '@/api/request'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref<number | null>(null)
const userList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editDialogVisible = ref(false)
const editForm = reactive({
  id: 0,
  nickname: '',
  phone: '',
  email: '',
  role: ''
})

// 表格列配置
const tableColumns = computed<TableColumn[]>(() => [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'username', label: '用户名', width: 150 },
  { prop: 'nickname', label: '昵称', width: 150 },
  { prop: 'phone', label: '手机号', width: 150 },
  { prop: 'email', label: '邮箱', width: 200 },
  { prop: 'role', label: '角色', width: 100 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'createTime', label: '注册时间', width: 180 },
  { prop: 'actions', label: '操作', width: 200 }
])

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  loadUsers()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  loadUsers()
}

const handleRowClick = (row: any) => {
  // 行点击事件（可选）
}

const loadUsers = async () => {
  loading.value = true
  try {
    const result = await request.get('/admin/users', {
      params: {
        keyword: searchKeyword.value,
        role: filterRole.value,
        status: filterStatus.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    userList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (row: any) => {
  Object.assign(editForm, row)
  editDialogVisible.value = true
}

const handleSaveEdit = async () => {
  try {
    await request.put(`/admin/users/${editForm.id}`, editForm)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const handleToggleStatus = async (row: any) => {
  try {
    await request.put(`/admin/users/${row.id}/status`, {
      status: row.status === 1 ? 0 : 1
    })
    ElMessage.success('操作成功')
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用户吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/admin/users/${id}`)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.user-manage-page {
  max-width: 1600px;
  margin: 0 auto;
  
  .page-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 24px;
    font-family: 'Roboto', sans-serif;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
  
  .action-buttons {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>

