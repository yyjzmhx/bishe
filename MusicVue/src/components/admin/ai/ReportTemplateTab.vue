<template>
  <div class="report-template-tab">
    <!-- 模板列表 -->
    <NextCard class="template-list-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Document /></el-icon>
            报告模板列表
          </h3>
          <el-button type="primary" :icon="Plus" @click="handleCreate">
            创建模板
          </el-button>
        </div>
      </template>
      
      <NextTable
        :data="templateList"
        :columns="templateColumns"
        :loading="loading"
        stripe
        hover
      >
        <template #column-isDefault="{ row }">
          <el-tag v-if="row.isDefault" type="success">默认</el-tag>
          <span v-else style="color: var(--text-gray)">-</span>
        </template>
        
        <template #column-status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
        
        <template #column-actions="{ row }">
          <el-button
            text
            type="primary"
            size="small"
            @click="handleEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            text
            type="success"
            size="small"
            @click="handlePreview(row)"
          >
            预览
          </el-button>
          <el-button
            v-if="!row.isDefault"
            text
            type="warning"
            size="small"
            @click="handleSetDefault(row.id)"
          >
            设为默认
          </el-button>
          <el-button
            text
            type="danger"
            size="small"
            @click="handleDelete(row.id)"
          >
            删除
          </el-button>
        </template>
      </NextTable>
    </NextCard>
    
    <!-- 编辑/创建对话框 -->
    <NextModal
      v-model="showDialog"
      :title="dialogTitle"
      width="900px"
    >
      <el-form
        ref="templateFormRef"
        :model="templateForm"
        :rules="templateRules"
        label-width="120px"
      >
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        
        <el-form-item label="模板描述" prop="description">
          <el-input
            v-model="templateForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
          />
        </el-form-item>
        
        <el-form-item label="显示字段">
          <el-checkbox-group v-model="templateForm.fields">
            <el-checkbox label="recommendationReason">推荐理由</el-checkbox>
            <el-checkbox label="style">音乐风格</el-checkbox>
            <el-checkbox label="emotion">情感色彩</el-checkbox>
            <el-checkbox label="rhythm">节奏特点</el-checkbox>
            <el-checkbox label="atmosphere">整体氛围</el-checkbox>
            <el-checkbox label="instruments">乐器类型</el-checkbox>
            <el-checkbox label="description">整体描述</el-checkbox>
            <el-checkbox label="featureVector">特征向量</el-checkbox>
            <el-checkbox label="visualization">可视化图表</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        
        <el-form-item label="布局样式">
          <el-radio-group v-model="templateForm.layout">
            <el-radio label="card">卡片布局</el-radio>
            <el-radio label="list">列表布局</el-radio>
            <el-radio label="grid">网格布局</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-switch
            v-model="templateForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存
        </el-button>
      </template>
    </NextModal>
    
    <!-- 预览对话框 -->
    <NextModal
      v-model="showPreview"
      title="模板预览"
      width="1000px"
    >
      <div class="preview-container">
        <div v-if="previewTemplate" class="preview-content">
          <!-- 模拟分析报告预览 -->
          <div class="preview-section">
            <h4>推荐理由</h4>
            <p>这是一首旋律流畅、节奏明快的音乐，适合放松时聆听。</p>
          </div>
          
          <div v-if="previewTemplate.fields.includes('style')" class="preview-section">
            <h4>音乐风格</h4>
            <p>流行</p>
          </div>
          
          <div v-if="previewTemplate.fields.includes('emotion')" class="preview-section">
            <h4>情感色彩</h4>
            <p>欢快</p>
          </div>
          
          <div v-if="previewTemplate.fields.includes('rhythm')" class="preview-section">
            <h4>节奏特点</h4>
            <p>快节奏</p>
          </div>
          
          <div v-if="previewTemplate.fields.includes('atmosphere')" class="preview-section">
            <h4>整体氛围</h4>
            <p>轻松</p>
          </div>
          
          <div v-if="previewTemplate.fields.includes('instruments')" class="preview-section">
            <h4>乐器类型</h4>
            <div>
              <el-tag>钢琴</el-tag>
              <el-tag>吉他</el-tag>
            </div>
          </div>
          
          <div v-if="previewTemplate.fields.includes('description')" class="preview-section">
            <h4>整体描述</h4>
            <p>这是一首充满活力的流行音乐，旋律优美，节奏感强。</p>
          </div>
          
          <div v-if="previewTemplate.fields.includes('featureVector')" class="preview-section">
            <h4>特征向量</h4>
            <el-progress :percentage="80" />
            <el-progress :percentage="70" />
            <el-progress :percentage="90" />
          </div>
          
          <div v-if="previewTemplate.fields.includes('visualization')" class="preview-section">
            <h4>可视化图表</h4>
            <div class="preview-chart-placeholder">
              [图表预览区域]
            </div>
          </div>
        </div>
      </div>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Document, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextTable from '@/components/ui/NextTable.vue'
import NextModal from '@/components/ui/NextModal.vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/api/request'

interface ReportTemplate {
  id?: number
  name: string
  description?: string
  fields: string[]
  layout: string
  status: number
  isDefault?: boolean
}

const templateFormRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const showPreview = ref(false)
const isEdit = ref(false)
const previewTemplate = ref<ReportTemplate | null>(null)

const templateList = ref<any[]>([])

const templateForm = reactive<ReportTemplate>({
  name: '',
  description: '',
  fields: ['recommendationReason', 'style', 'emotion', 'rhythm', 'atmosphere', 'instruments', 'description', 'featureVector', 'visualization'],
  layout: 'card',
  status: 1
})

const templateRules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ]
}

const templateColumns = [
  { prop: 'name', label: '模板名称', width: '200px' },
  { prop: 'description', label: '描述', width: '250px' },
  { prop: 'layout', label: '布局', width: '120px' },
  { prop: 'isDefault', label: '默认', width: '100px' },
  { prop: 'status', label: '状态', width: '100px' },
  { prop: 'updateTime', label: '更新时间', width: '180px' },
  { prop: 'actions', label: '操作', width: '300px' }
]

const dialogTitle = computed(() => isEdit.value ? '编辑模板' : '创建模板')

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true
  try {
    const result: any = await request.get('/admin/ai/template/list')
    templateList.value = result || []
  } catch (error: any) {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

// 创建模板
const handleCreate = () => {
  isEdit.value = false
  Object.assign(templateForm, {
    name: '',
    description: '',
    fields: ['recommendationReason', 'style', 'emotion', 'rhythm', 'atmosphere', 'instruments', 'description', 'featureVector', 'visualization'],
    layout: 'card',
    status: 1
  })
  showDialog.value = true
}

// 编辑模板
const handleEdit = (row: any) => {
  isEdit.value = true
  Object.assign(templateForm, {
    id: row.id,
    name: row.name,
    description: row.description || '',
    fields: row.config?.fields || [],
    layout: row.config?.layout || 'card',
    status: row.status
  })
  showDialog.value = true
}

// 预览模板
const handlePreview = (row: any) => {
  previewTemplate.value = {
    name: row.name,
    fields: row.config?.fields || [],
    layout: row.config?.layout || 'card',
    status: row.status
  }
  showPreview.value = true
}

// 保存模板
const handleSave = async () => {
  if (!templateFormRef.value) return
  
  await templateFormRef.value.validate()
  
  saving.value = true
  try {
    const data = {
      ...templateForm,
      config: {
        fields: templateForm.fields,
        layout: templateForm.layout
      }
    }
    
    if (isEdit.value) {
      await request.put(`/admin/ai/template/${templateForm.id}`, data)
      ElMessage.success('模板更新成功')
    } else {
      await request.post('/admin/ai/template/create', data)
      ElMessage.success('模板创建成功')
    }
    
    showDialog.value = false
    await loadTemplates()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 设为默认
const handleSetDefault = async (id: number) => {
  try {
    await request.post(`/admin/ai/template/${id}/set-default`)
    ElMessage.success('已设为默认模板')
    await loadTemplates()
  } catch (error: any) {
    ElMessage.error('设置失败')
  }
}

// 删除模板
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此模板吗？', '提示', {
      type: 'warning'
    })
    
    await request.delete(`/admin/ai/template/${id}`)
    ElMessage.success('删除成功')
    await loadTemplates()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.report-template-tab {
  .template-list-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .card-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 600;
        margin: 0;
        color: var(--text-dark);
      }
    }
  }
  
  .preview-container {
    .preview-content {
      .preview-section {
        margin-bottom: 24px;
        padding: 16px;
        background: rgba(0, 0, 0, 0.02);
        border-radius: 8px;
        
        h4 {
          font-size: 16px;
          font-weight: 600;
          margin: 0 0 12px 0;
          color: var(--text-dark);
        }
        
        p {
          margin: 0;
          color: var(--text-gray);
          line-height: 1.6;
        }
        
        .el-tag {
          margin-right: 8px;
        }
        
        .preview-chart-placeholder {
          height: 200px;
          background: rgba(0, 0, 0, 0.05);
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: var(--text-gray);
        }
      }
    }
  }
}
</style>

