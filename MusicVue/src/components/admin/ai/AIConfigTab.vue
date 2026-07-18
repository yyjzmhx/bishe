<template>
  <div class="ai-config-tab">
    <NextCard class="config-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Setting /></el-icon>
            AI服务配置
          </h3>
          <el-button type="primary" :icon="Check" @click="testConnection" :loading="testing">
            测试连接
          </el-button>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="configForm"
        :rules="formRules"
        label-width="140px"
        label-position="left"
      >
        <el-form-item label="服务提供商" prop="provider">
          <el-select v-model="configForm.provider" placeholder="选择AI服务提供商" style="width: 100%">
            <el-option label="阿里云通义千问 (DashScope)" value="dashscope" />
          </el-select>
          <div class="form-tip">当前运行时配置仅支持 DashScope，其他提供商暂未接入。</div>
        </el-form-item>
        
        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="configForm.apiKey"
            type="password"
            show-password
            placeholder="请输入API密钥"
            clearable
          />
          <div class="form-tip">请填写可用的 DashScope API Key，运行时会直接用于服务端调用。</div>
        </el-form-item>
        
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input
            v-model="configForm.baseUrl"
            placeholder="API基础URL（可选，使用默认值）"
            clearable
          />
          <div class="form-tip">留空则使用服务提供商的默认URL</div>
        </el-form-item>
        
        <el-form-item label="模型名称" prop="model">
          <el-input
            v-model="configForm.model"
            placeholder="例如：qwen-turbo, gpt-3.5-turbo"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="Temperature" prop="temperature">
          <el-slider
            v-model="configForm.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            show-input
            :show-input-controls="false"
            style="width: 100%"
          />
          <div class="form-tip">控制输出的随机性，值越大越随机（推荐：0.7）</div>
        </el-form-item>
        
        <el-form-item label="超时时间" prop="timeout">
          <el-input-number
            v-model="configForm.timeout"
            :min="5000"
            :max="60000"
            :step="1000"
            style="width: 100%"
          />
          <span style="margin-left: 8px; color: var(--text-gray)">毫秒</span>
        </el-form-item>
        
        <el-form-item label="分析策略" prop="analysisStrategy">
          <el-radio-group v-model="configForm.analysisStrategy">
            <el-radio label="description">直接生成描述</el-radio>
            <el-radio label="transcribe">结合歌词/文本线索分析</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="启用状态">
          <el-switch
            v-model="configForm.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :icon="Check" @click="saveConfig" :loading="saving">
            保存配置
          </el-button>
          <el-button :icon="Refresh" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </NextCard>
    
    <!-- 配置历史 -->
    <NextCard class="history-card" style="margin-top: 24px">
      <template #header>
        <h3 class="card-title">
          <el-icon><Clock /></el-icon>
          配置历史
        </h3>
      </template>
      
      <NextTable
        :data="configHistory"
        :columns="historyColumns"
        :loading="loadingHistory"
        stripe
        hover
      >
        <template #column-isActive="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'">
            {{ row.isActive ? '启用' : '禁用' }}
          </el-tag>
        </template>
        
        <template #column-actions="{ row }">
          <el-button
            text
            type="primary"
            size="small"
            @click="loadConfig(row.id)"
          >
            加载
          </el-button>
          <el-button
            text
            type="danger"
            size="small"
            @click="deleteConfig(row.id)"
          >
            删除
          </el-button>
        </template>
      </NextTable>
    </NextCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Setting, Check, Refresh, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextTable from '@/components/ui/NextTable.vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/api/request'

interface AIConfig {
  id?: number
  provider: string
  apiKey: string
  baseUrl?: string
  model: string
  temperature: number
  timeout: number
  analysisStrategy: string
  isActive: boolean
}

const formRef = ref<FormInstance>()
const testing = ref(false)
const saving = ref(false)
const loadingHistory = ref(false)

const configForm = reactive<AIConfig>({
  provider: 'dashscope',
  apiKey: '',
  baseUrl: '',
  model: 'qwen-turbo',
  temperature: 0.7,
  timeout: 30000,
  analysisStrategy: 'description',
  isActive: true
})

const formRules: FormRules = {
  provider: [
    { required: true, message: '请选择服务提供商', trigger: 'change' }
  ],
  apiKey: [
    { required: true, message: '请输入API密钥', trigger: 'blur' },
    { min: 10, message: 'API密钥长度至少10个字符', trigger: 'blur' }
  ],
  model: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  temperature: [
    { required: true, message: '请设置Temperature', trigger: 'blur' }
  ],
  timeout: [
    { required: true, message: '请设置超时时间', trigger: 'blur' }
  ]
}

const configHistory = ref<any[]>([])
const historyColumns = [
  { prop: 'provider', label: '提供商', width: '150px' },
  { prop: 'model', label: '模型', width: '150px' },
  { prop: 'temperature', label: 'Temperature', width: '120px' },
  { prop: 'isActive', label: '状态', width: '100px' },
  { prop: 'updateTime', label: '更新时间', width: '180px' },
  { prop: 'actions', label: '操作', width: '150px' }
]

// 加载当前配置
const loadCurrentConfig = async () => {
  try {
    const result: any = await request.get('/admin/ai/config/current')
    if (result) {
      Object.assign(configForm, {
        provider: result.provider || 'dashscope',
        apiKey: result.apiKey || '',
        baseUrl: result.baseUrl || '',
        model: result.model || 'qwen-turbo',
        temperature: result.temperature ?? 0.7,
        timeout: result.timeout ?? 30000,
        analysisStrategy: result.analysisStrategy || 'description',
        isActive: result.isActive !== undefined ? result.isActive : true
      })
    }
  } catch (error: any) {
    console.error('加载配置失败', error)
  }
}

// 加载配置历史
const loadConfigHistory = async () => {
  loadingHistory.value = true
  try {
    const result: any = await request.get('/admin/ai/config/history')
    configHistory.value = result || []
  } catch (error: any) {
    ElMessage.error('加载配置历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 测试连接
const testConnection = async () => {
  if (!formRef.value) return
  
  await formRef.value.validateField('apiKey')
  await formRef.value.validateField('model')
  
  testing.value = true
  try {
    const result: any = await request.post('/admin/ai/config/test', {
      provider: configForm.provider,
      apiKey: configForm.apiKey,
      baseUrl: configForm.baseUrl,
      model: configForm.model
    })
    
    if (result.success) {
      ElMessage.success('连接测试成功！')
    } else {
      ElMessage.error(result.message || '连接测试失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    testing.value = false
  }
}

// 保存配置
const saveConfig = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate()
  
  saving.value = true
  try {
    await request.post('/admin/ai/config/save', configForm)
    ElMessage.success('配置保存成功')
    await loadConfigHistory()
  } catch (error: any) {
    ElMessage.error(error.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  loadCurrentConfig()
}

// 加载指定配置
const loadConfig = async (id: number) => {
  try {
    const result: any = await request.get(`/admin/ai/config/${id}`)
    if (result) {
      Object.assign(configForm, {
        provider: result.provider,
        apiKey: result.apiKey,
        baseUrl: result.baseUrl || '',
        model: result.model,
        temperature: result.temperature,
        timeout: result.timeout,
        analysisStrategy: result.analysisStrategy || 'description',
        isActive: result.isActive
      })
      ElMessage.success('配置已加载')
    }
  } catch (error: any) {
    ElMessage.error('加载配置失败')
  }
}

// 删除配置
const deleteConfig = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此配置吗？', '提示', {
      type: 'warning'
    })
    
    await request.delete(`/admin/ai/config/${id}`)
    ElMessage.success('删除成功')
    await loadConfigHistory()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadCurrentConfig()
  loadConfigHistory()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.ai-config-tab {
  .config-card {
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
    
    .form-tip {
      font-size: 12px;
      color: var(--text-gray);
      margin-top: 4px;
    }
  }
  
  .history-card {
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
</style>

