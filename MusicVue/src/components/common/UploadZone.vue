<template>
  <div
    class="upload-zone"
    :class="{ 'is-dragover': isDragover, 'is-uploading': uploading }"
    @drop="handleDrop"
    @dragover.prevent="handleDragOver"
    @dragleave="handleDragLeave"
    @click="triggerFileInput"
  >
    <input
      ref="fileInputRef"
      type="file"
      accept="audio/*"
      style="display: none"
      @change="handleFileChange"
    />
    
    <div class="upload-content">
      <div class="upload-icon">
        <el-icon :size="64" :class="{ 'rotating': uploading }">
          <UploadFilled />
        </el-icon>
      </div>
      
      <h3 class="upload-title">
        {{ uploading ? '上传中...' : '拖拽文件到此处或点击上传' }}
      </h3>
      
      <p class="upload-desc">
        支持 MP3、WAV、FLAC、M4A、AAC、OGG、MGG 格式，文件大小不超过 50MB
      </p>
      
      <el-button
        v-if="!uploading"
        type="primary"
        size="large"
        @click.stop="triggerFileInput"
      >
        选择文件
      </el-button>
      
      <div v-if="file" class="file-info">
        <el-icon><Document /></el-icon>
        <span>{{ file.name }}</span>
        <span class="file-size">({{ formatFileSize(file.size) }})</span>
      </div>
    </div>
    
    <div v-if="uploading && uploadProgress > 0" class="upload-progress">
      <el-progress
        :percentage="uploadProgress"
        :stroke-width="8"
        :show-text="false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { formatFileSize } from '@/utils/format'

const props = defineProps<{
  uploading?: boolean
  uploadProgress?: number
}>()

const emit = defineEmits<{
  upload: [file: File]
}>()

const fileInputRef = ref<HTMLInputElement>()
const isDragover = ref(false)
const file = ref<File | null>(null)

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragover.value = false
  
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    handleFile(files[0])
  }
}

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDragover.value = true
}

const handleDragLeave = () => {
  isDragover.value = false
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    handleFile(files[0])
  }
}

const handleFile = (selectedFile: File) => {
  // 验证文件类型
  const fileExtension = selectedFile.name.split('.').pop()?.toLowerCase()
  const allowedExtensions = ['mp3', 'wav', 'flac', 'm4a', 'aac', 'ogg', 'mgg']
  
  if (!allowedExtensions.includes(fileExtension || '')) {
    ElMessage.error('不支持的文件类型，支持的类型: MP3, WAV, FLAC, M4A, AAC, OGG, MGG')
    return
  }
  
  // 验证文件大小（50MB）
  if (selectedFile.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    return
  }
  
  file.value = selectedFile
  emit('upload', selectedFile)
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.upload-zone {
  position: relative;
  width: 100%;
  min-height: 240px;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--bg-white);
  @include flex-center;
  cursor: pointer;
  transition: all var(--transition-base);
  
  &:hover,
  &.is-dragover {
    border-color: var(--primary-to);
    background: rgba(59, 130, 246, 0.05);
    
    .upload-icon {
      transform: scale(1.1);
    }
  }
  
  &.is-uploading {
    cursor: not-allowed;
    opacity: 0.8;
  }
  
  .upload-content {
    text-align: center;
    padding: 48px 24px;
    
    .upload-icon {
      margin-bottom: 24px;
      color: var(--primary-to);
      transition: transform var(--transition-base);
      
      &.rotating {
        animation: rotate 2s linear infinite;
      }
    }
    
    .upload-title {
      font-size: 20px;
      font-weight: 600;
      margin-bottom: 8px;
      color: var(--text-dark);
    }
    
    .upload-desc {
      font-size: 14px;
      color: var(--text-gray);
      margin-bottom: 24px;
    }
    
    .file-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-top: 16px;
      padding: 12px;
      background: var(--bg-light);
      border-radius: var(--radius-sm);
      color: var(--text-dark);
      
      .file-size {
        color: var(--text-gray);
      }
    }
  }
  
  .upload-progress {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 0 0 var(--radius-lg) var(--radius-lg);
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
</style>

