<template>
  <div class="site-content-manage-page">
    <h2 class="page-title text-gradient">站点内容管理</h2>
    
    <!-- 类型切换 -->
    <NextCard variant="shadow" hover class="type-tabs-card">
      <div class="type-tabs">
        <el-radio-group v-model="currentType" @change="handleTypeChange">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="CAROUSEL">轮播图</el-radio-button>
          <el-radio-button label="HOT_PLAYLIST">热门歌单</el-radio-button>
          <el-radio-button label="BANNER">横幅</el-radio-button>
        </el-radio-group>
        <NextButton variant="primary" :icon="Plus" @click="handleAdd">添加内容</NextButton>
      </div>
    </NextCard>
    
    <!-- 内容卡片网格 -->
    <div class="content-grid" v-loading="loading">
      <div
        v-for="item in contentList"
        :key="item.id"
        class="content-card"
        :class="{ disabled: item.status === 0 }"
      >
        <div class="card-image" v-if="item.imageUrl">
          <img :src="item.imageUrl" :alt="item.title" />
          <div class="image-overlay">
            <el-button circle :icon="View" @click="handlePreview(item)" />
            <el-button circle :icon="Edit" @click="handleEdit(item)" />
            <el-button circle :icon="Delete" type="danger" @click="handleDelete(item.id)" />
          </div>
        </div>
        <div class="card-content">
          <div class="card-header">
            <h3 class="card-title">{{ item.title || '未命名' }}</h3>
            <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small">
              {{ item.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
          <div class="card-info">
            <div class="info-item">
              <el-icon><Link /></el-icon>
              <span class="info-label">类型：</span>
              <span>{{ getTypeText(item.type) }}</span>
            </div>
            <div class="info-item" v-if="item.linkUrl">
              <el-icon><Connection /></el-icon>
              <span class="info-label">链接：</span>
              <el-link :href="item.linkUrl" target="_blank" type="primary" underline="never">
                {{ item.linkUrl }}
              </el-link>
            </div>
            <div class="info-item">
              <el-icon><Sort /></el-icon>
              <span class="info-label">排序：</span>
              <span>{{ item.sortOrder }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" :icon="Edit" @click="handleEdit(item)">编辑</el-button>
            <el-button 
              size="small" 
              :type="item.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(item)"
            >
              {{ item.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(item.id)">
              删除
            </el-button>
          </div>
        </div>
      </div>
      
      <el-empty v-if="!loading && contentList.length === 0" description="暂无内容" />
    </div>
    
    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadContent"
        @current-change="loadContent"
      />
    </div>
    
    <!-- 添加/编辑对话框 -->
    <NextModal
      v-model="editDialogVisible"
      :title="editForm.id ? '编辑内容' : '添加内容'"
      size="xl"
      :mask-closable="false"
      class="content-edit-dialog"
    >
      <el-form :model="editForm" label-width="100px" ref="contentFormRef">
        <el-form-item label="类型" prop="type" required>
          <el-select v-model="editForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="轮播图" value="CAROUSEL" />
            <el-option label="热门歌单" value="HOT_PLAYLIST" />
            <el-option label="横幅" value="BANNER" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <NextInput v-model="editForm.title" placeholder="请输入标题（可选）" />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <div class="image-upload-section">
            <el-upload
              ref="imageUploadRef"
              :auto-upload="false"
              :on-change="handleImageChange"
              :on-remove="handleImageRemove"
              :show-file-list="false"
              accept="image/*"
              :limit="1"
              class="image-uploader"
            >
              <div v-if="imagePreviewUrl" class="image-preview">
                <img :src="imagePreviewUrl" alt="预览" />
                <div class="image-mask">
                  <el-icon><Camera /></el-icon>
                  <span>更换图片</span>
                </div>
              </div>
              <div v-else class="image-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <div class="upload-text">上传图片</div>
              </div>
            </el-upload>
            <NextInput
              v-model="editForm.imageUrl"
              placeholder="或输入图片URL"
              :prefix-icon="Link"
              clearable
              style="margin-top: 12px"
            />
          </div>
        </el-form-item>
        <el-form-item label="链接" prop="linkUrl">
          <NextInput v-model="editForm.linkUrl" placeholder="请输入链接URL（可选）" />
        </el-form-item>
        <el-form-item label="内容" prop="content" v-if="editForm.type === 'HOT_PLAYLIST'">
          <NextInput
            v-model="editForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入JSON格式的内容（可选）"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="editForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
    
    <!-- 预览对话框 -->
    <NextModal v-model="previewDialogVisible" title="预览" size="md">
      <div v-if="previewItem" class="preview-content">
        <img v-if="previewItem.imageUrl" :src="previewItem.imageUrl" alt="预览" class="preview-image" />
        <h3>{{ previewItem.title }}</h3>
        <p v-if="previewItem.linkUrl">
          <el-link :href="previewItem.linkUrl" target="_blank" type="primary" underline="hover">
            {{ previewItem.linkUrl }}
          </el-link>
        </p>
      </div>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { Plus, Edit, Delete, View, Link, Connection, Sort, Camera } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElForm } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextInput from '@/components/ui/NextInput.vue'
import NextModal from '@/components/ui/NextModal.vue'
import NextButton from '@/components/ui/NextButton.vue'
import request from '@/api/request'
import axios from 'axios'
import { getToken } from '@/utils/auth'

const loading = ref(false)
const saving = ref(false)
const currentType = ref('')
const contentList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const editDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const contentFormRef = ref<InstanceType<typeof ElForm>>()
const imageUploadRef = ref()
const imageFile = ref<File | null>(null)
const imagePreviewUrl = ref<string | null>(null)
const previewItem = ref<any>(null)

const editForm = reactive({
  id: 0,
  type: 'CAROUSEL',
  title: '',
  content: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  status: 1
})

// 监听图片URL变化
watch(() => editForm.imageUrl, (newUrl) => {
  if (newUrl && !imageFile.value) {
    imagePreviewUrl.value = newUrl
  } else if (!newUrl && !imageFile.value) {
    imagePreviewUrl.value = null
  }
})

const loadContent = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (currentType.value) params.type = currentType.value
    
    const result: any = await request.get('/admin/site-content', { params })
    contentList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  pageNum.value = 1
  loadContent()
}

const handleAdd = () => {
  Object.assign(editForm, {
    id: 0,
    type: currentType.value || 'CAROUSEL',
    title: '',
    content: '',
    imageUrl: '',
    linkUrl: '',
    sortOrder: 0,
    status: 1
  })
  imageFile.value = null
  imagePreviewUrl.value = null
  editDialogVisible.value = true
}

const handleEdit = (item: any) => {
  Object.assign(editForm, {
    id: item.id,
    type: item.type,
    title: item.title || '',
    content: item.content || '',
    imageUrl: item.imageUrl || '',
    linkUrl: item.linkUrl || '',
    sortOrder: item.sortOrder || 0,
    status: item.status
  })
  imageFile.value = null
  imagePreviewUrl.value = item.imageUrl || null
  editDialogVisible.value = true
}

const handleImageChange = (file: any) => {
  const isImage = file.raw.type.startsWith('image/')
  const isLt5M = file.raw.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    imageUploadRef.value?.clearFiles()
    return
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    imageUploadRef.value?.clearFiles()
    return
  }
  
  imageFile.value = file.raw
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreviewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  editForm.imageUrl = '' // 清空URL输入框
}

const handleImageRemove = () => {
  imageFile.value = null
  imagePreviewUrl.value = null
  if (editForm.imageUrl) {
    imagePreviewUrl.value = editForm.imageUrl
  }
}

const handleSaveEdit = async () => {
  if (!contentFormRef.value) return
  
  await contentFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写完整信息')
      return
    }
    
    saving.value = true
    try {
      const token = getToken()
      
      if (editForm.id) {
        // 编辑模式
        if (imageFile.value) {
          // 先上传图片
          const imageFormData = new FormData()
          imageFormData.append('file', imageFile.value)
          await axios.post(
            `/api/admin/site-content/${editForm.id}/image`,
            imageFormData,
            {
              headers: {
                'Content-Type': 'multipart/form-data',
                'Authorization': token ? `Bearer ${token}` : ''
              }
            }
          )
        }
        
        // 更新其他信息
        await request.put(`/admin/site-content/${editForm.id}`, {
          type: editForm.type,
          title: editForm.title,
          content: editForm.content,
          imageUrl: editForm.imageUrl,
          linkUrl: editForm.linkUrl,
          sortOrder: editForm.sortOrder,
          status: editForm.status
        })
        ElMessage.success('更新成功')
      } else {
        // 新增模式
        const formData = new FormData()
        formData.append('type', editForm.type)
        if (editForm.title) formData.append('title', editForm.title)
        if (editForm.content) formData.append('content', editForm.content)
        if (editForm.linkUrl) formData.append('linkUrl', editForm.linkUrl)
        formData.append('sortOrder', String(editForm.sortOrder))
        
        if (imageFile.value) {
          formData.append('imageFile', imageFile.value)
        } else if (editForm.imageUrl) {
          formData.append('imageUrl', editForm.imageUrl)
        }
        
        await axios.post(
          '/api/admin/site-content',
          formData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
              'Authorization': token ? `Bearer ${token}` : ''
            }
          }
        )
        ElMessage.success('添加成功')
      }
      
      editDialogVisible.value = false
      handleCancelEdit()
      loadContent()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || error.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
  contentFormRef.value?.resetFields()
  imageFile.value = null
  imagePreviewUrl.value = null
}

const handleToggleStatus = async (item: any) => {
  try {
    const action = item.status === 1 ? '禁用' : '启用'
    await request.put(`/admin/site-content/${item.id}/status`, {
      status: item.status === 1 ? 0 : 1
    })
    ElMessage.success(`${action}成功`)
    loadContent()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条内容吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/admin/site-content/${id}`)
    ElMessage.success('删除成功')
    loadContent()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handlePreview = (item: any) => {
  previewItem.value = item
  previewDialogVisible.value = true
}

const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'CAROUSEL': '轮播图',
    'HOT_PLAYLIST': '热门歌单',
    'BANNER': '横幅'
  }
  return typeMap[type] || type
}

onMounted(() => {
  loadContent()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.site-content-manage-page {
  max-width: 1600px;
  margin: 0 auto;
  
  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 24px;
    font-family: 'Roboto', sans-serif;
  }
}

.type-tabs-card {
  margin-bottom: 24px;
  
  .type-tabs {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .el-radio-group {
      :deep(.el-radio-button__inner) {
        border-radius: 8px;
        border: 1px solid rgba(0, 0, 0, 0.1);
        transition: all 0.2s;
        
        &:hover {
          border-color: rgba(0, 195, 255, 0.3);
        }
      }
      
      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: var(--primary-gradient);
        border-color: var(--primary-color);
        color: #fff;
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

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
  
  .content-card {
    @include glass-card;
    overflow: hidden;
    transition: all var(--transition-base);
    
    &.disabled {
      opacity: 0.6;
    }
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-lg);
      
      .image-overlay {
        opacity: 1;
      }
    }
    
    .card-image {
      width: 100%;
      height: 200px;
      position: relative;
      overflow: hidden;
      background: var(--bg-light);
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .image-overlay {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        opacity: 0;
        transition: opacity 0.3s;
      }
    }
    
    .card-content {
      padding: 20px;
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 16px;
        
        .card-title {
          font-size: 18px;
          font-weight: 600;
          color: var(--text-dark);
          margin: 0;
          flex: 1;
          @include text-ellipsis(2);
        }
      }
      
      .card-info {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-bottom: 16px;
        
        .info-item {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          color: var(--text-gray);
          
          .el-icon {
            color: var(--primary-color);
          }
          
          .info-label {
            font-weight: 500;
          }
        }
      }
      
      .card-actions {
        display: flex;
        gap: 8px;
        padding-top: 16px;
        border-top: 1px solid var(--border-color);
      }
    }
  }
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.content-edit-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  .image-upload-section {
    .image-uploader {
      width: 100%;
      
      :deep(.el-upload) {
        width: 100%;
      }
      
      .image-preview {
        width: 100%;
        height: 200px;
        position: relative;
        border-radius: 8px;
        overflow: hidden;
        
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        
        .image-mask {
          position: absolute;
          inset: 0;
          background: rgba(0, 0, 0, 0.6);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 8px;
          color: #fff;
          opacity: 0;
          transition: opacity 0.3s;
          cursor: pointer;
          
          .el-icon {
            font-size: 32px;
          }
        }
        
        &:hover .image-mask {
          opacity: 1;
        }
      }
      
      .image-placeholder {
        width: 100%;
        height: 200px;
        border: 2px dashed var(--border-color);
        border-radius: 8px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        color: var(--text-gray);
        cursor: pointer;
        transition: all 0.2s;
        
        &:hover {
          border-color: var(--primary-color);
          background: rgba(0, 195, 255, 0.05);
        }
        
        .upload-icon {
          font-size: 48px;
          color: var(--primary-color);
        }
        
        .upload-text {
          font-size: 14px;
          font-weight: 500;
        }
      }
    }
  }
}

.preview-content {
  text-align: center;
  
  .preview-image {
    width: 100%;
    max-height: 400px;
    object-fit: contain;
    border-radius: 8px;
    margin-bottom: 16px;
  }
  
  h3 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-dark);
    margin-bottom: 12px;
  }
}
</style>

