<template>
  <div class="music-manage-page">
    <!-- 页面标题 -->
    <h2 class="page-title text-gradient">音乐库管理</h2>
    
    <!-- 搜索栏 -->
    <NextCard variant="shadow" hover class="search-card">
      <div class="search-actions">
        <NextInput
          v-model="searchKeyword"
          placeholder="搜索歌曲名、歌手..."
          :prefix-icon="Search"
          clearable
          style="width: 300px"
          @keyup.enter="loadMusic"
        />
        <el-select v-model="filterGenre" placeholder="风格" clearable style="width: 150px" @change="loadMusic">
          <el-option label="流行" value="流行" />
          <el-option label="摇滚" value="摇滚" />
          <el-option label="电子" value="电子" />
          <el-option label="爵士" value="爵士" />
          <el-option label="古典" value="古典" />
        </el-select>
        <NextButton variant="primary" :icon="Plus" @click="handleAdd">添加音乐</NextButton>
      </div>
    </NextCard>

    <!-- 悬浮行表格 -->
    <NextCard variant="shadow" hover class="table-container">
      <el-table 
        :data="musicList" 
        v-loading="loading"
        class="custom-table"
      >
        <!-- 封面与歌名合并列 -->
        <el-table-column label="歌曲信息" min-width="300">
          <template #default="{ row }">
            <div class="song-info-cell">
              <div class="cover-wrapper">
                <img :src="row.coverUrl || defaultCover" class="cover-img" />
                <div class="play-mask">
                  <el-icon><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="text-info">
                <div class="song-title">{{ row.title }}</div>
                <div class="song-artist">{{ row.artist }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="album" label="专辑" width="150" />
        <el-table-column prop="genre" label="风格" width="100" />
        <el-table-column prop="playCount" label="播放量" width="100" />
        <el-table-column prop="likeCount" label="点赞数" width="100" />
        
        <!-- 状态列 -->
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <div class="status-dot" :class="row.status === 1 ? 'online' : 'offline'">
              <span class="dot"></span>
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </div>
          </template>
        </el-table-column>

        <!-- 操作栏 -->
        <el-table-column label="操作" width="150" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="编辑" placement="top">
                <el-button circle :icon="Edit" @click="handleEdit(row)" />
              </el-tooltip>
              <el-tooltip content="替换音频" placement="top">
                <el-button circle :icon="UploadFilled" type="success" @click="handleUploadFile(row.id)" />
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
          @size-change="loadMusic"
          @current-change="loadMusic"
        />
      </div>
    </NextCard>
    
    <!-- 添加/编辑对话框 -->
    <NextModal
      v-model="editDialogVisible"
      :title="editForm.id ? '编辑音乐' : '上传歌曲到音乐库'"
      size="xl"
      :mask-closable="false"
      class="music-edit-dialog"
    >
      <!-- 步骤1：音频文件上传（必填，最优先） -->
      <div class="upload-step-section">
        <div class="step-header">
          <div class="step-number">1</div>
          <div class="step-title-wrapper">
            <div class="section-title">
              <el-icon><Headset /></el-icon>
              <span>上传音频文件</span>
              <el-tag type="danger" size="small" style="margin-left: 8px">必填</el-tag>
            </div>
            <div class="step-description">请先上传音频文件，这是添加歌曲到音乐库的必要步骤</div>
          </div>
        </div>
        
        <div class="audio-upload-wrapper-main">
          <el-upload
            ref="audioUploadRef"
            :auto-upload="false"
            :on-change="handleAudioChange"
            :on-remove="handleAudioRemove"
            accept=".mp3,.wav,.flac,.m4a,.aac,.ogg,.mgg"
            :limit="1"
            drag
            class="audio-upload-wrapper"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将音频文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                <el-icon><InfoFilled /></el-icon>
                支持 MP3、WAV、FLAC、M4A、AAC、OGG、MGG 格式，文件大小不超过 50MB
              </div>
            </template>
          </el-upload>
          
          <!-- 音频文件信息预览 -->
          <div v-if="audioFile" class="audio-file-info-card">
            <div class="file-info-header">
              <el-icon class="success-icon"><CircleCheck /></el-icon>
              <span class="file-info-title">已选择音频文件</span>
            </div>
            <div class="file-info-content">
              <div class="file-info-row">
                <el-icon><Document /></el-icon>
                <span class="file-label">文件名：</span>
                <span class="file-value">{{ audioFile.name }}</span>
              </div>
              <div class="file-info-row">
                <el-icon><FolderOpened /></el-icon>
                <span class="file-label">文件大小：</span>
                <span class="file-value">{{ formatFileSize(audioFile.size) }}</span>
              </div>
              <div class="file-info-row">
                <el-icon><Files /></el-icon>
                <span class="file-label">文件类型：</span>
                <span class="file-value">{{ audioFile.type || getFileType(audioFile.name) }}</span>
              </div>
            </div>
            <div class="file-info-actions">
              <el-button text type="danger" size="small" @click="handleAudioRemove">
                <el-icon><Delete /></el-icon>
                移除文件
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤2：歌曲信息（必填） -->
      <div class="upload-step-section">
        <div class="step-header">
          <div class="step-number">2</div>
          <div class="step-title-wrapper">
            <div class="section-title">
              <el-icon><EditPen /></el-icon>
              <span>填写歌曲信息</span>
              <el-tag type="danger" size="small" style="margin-left: 8px">必填</el-tag>
            </div>
            <div class="step-description">请填写歌曲的基本信息</div>
          </div>
        </div>
        
        <div class="form-section-main">
          <el-form :model="editForm" label-width="100px" class="music-form">
            <div class="form-row">
              <el-form-item label="歌曲名" required class="form-item-half">
                <el-input 
                  v-model="editForm.title" 
                  placeholder="请输入歌曲名"
                  clearable
                >
                  <template #prefix>
                    <el-icon><VideoCamera /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="歌手" required class="form-item-half">
                <el-input 
                  v-model="editForm.artist" 
                  placeholder="请输入歌手名"
                  clearable
                >
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </div>
            <div class="form-row">
              <el-form-item label="专辑" class="form-item-half">
                <el-input 
                  v-model="editForm.album" 
                  placeholder="请输入专辑名（可选）"
                  clearable
                >
                  <template #prefix>
                    <el-icon><Folder /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="风格" class="form-item-half">
                <el-select v-model="editForm.genre" placeholder="请选择风格（可选）" style="width: 100%">
                  <el-option label="流行" value="流行" />
                  <el-option label="摇滚" value="摇滚" />
                  <el-option label="电子" value="电子" />
                  <el-option label="爵士" value="爵士" />
                  <el-option label="古典" value="古典" />
                  <el-option label="民谣" value="民谣" />
                  <el-option label="说唱" value="说唱" />
                  <el-option label="R&B" value="R&B" />
                </el-select>
              </el-form-item>
            </div>
          </el-form>
        </div>
      </div>

      <!-- 步骤3：封面上传（可选） -->
      <div class="upload-step-section">
        <div class="step-header">
          <div class="step-number">3</div>
          <div class="step-title-wrapper">
            <div class="section-title">
              <el-icon><Picture /></el-icon>
              <span>上传封面图片</span>
              <el-tag type="info" size="small" style="margin-left: 8px">可选</el-tag>
            </div>
            <div class="step-description">为歌曲添加封面图片，提升视觉效果</div>
          </div>
        </div>
        
        <div class="cover-upload-section-main">
          <div class="cover-upload-box">
            <el-upload
              ref="coverUploadRef"
              :auto-upload="false"
              :on-change="handleCoverChange"
              :on-remove="handleCoverRemove"
              :show-file-list="false"
              accept="image/*"
              :limit="1"
              class="cover-uploader"
            >
              <div v-if="coverPreviewUrl" class="cover-preview">
                <img :src="coverPreviewUrl" alt="封面预览" />
                <div class="cover-mask">
                  <el-icon><Camera /></el-icon>
                  <span>更换封面</span>
                </div>
              </div>
              <div v-else class="cover-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <div class="upload-text">上传封面</div>
                <div class="upload-tip">支持 JPG、PNG、GIF、WEBP<br/>建议尺寸 500x500</div>
              </div>
            </el-upload>
            <div v-if="coverFile" class="cover-info">
              <el-icon><Document /></el-icon>
              <span>{{ coverFile.name }}</span>
              <el-button text type="danger" size="small" @click="handleCoverRemove">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <div class="cover-url-input">
              <el-input
                v-model="editForm.coverUrl"
                placeholder="或输入封面URL"
                clearable
              >
                <template #prefix>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤4：歌词编辑（可选） -->
      <div class="upload-step-section">
        <div class="step-header">
          <div class="step-number">4</div>
          <div class="step-title-wrapper">
            <div class="section-title">
              <el-icon><Document /></el-icon>
              <span>编辑歌词</span>
              <el-tag type="info" size="small" style="margin-left: 8px">可选</el-tag>
            </div>
            <div class="step-description">为歌曲添加LRC格式歌词，支持时间同步显示</div>
          </div>
        </div>
        
        <div class="lyrics-edit-section-main">
          <!-- 操作工具栏 -->
          <div class="lyrics-toolbar">
            <div class="toolbar-left">
              <el-button 
                type="primary" 
                :icon="UploadFilled" 
                size="small"
                @click="handleUploadLRC"
              >
                上传LRC文件
              </el-button>
              <el-button 
                :icon="View" 
                size="small"
                @click="showLyricsPreview = !showLyricsPreview"
                :type="showLyricsPreview ? 'primary' : 'default'"
              >
                {{ showLyricsPreview ? '隐藏预览' : '预览歌词' }}
              </el-button>
              <el-button 
                :icon="Delete" 
                size="small"
                @click="handleClearLyrics"
                :disabled="!editForm.lyrics"
              >
                清空
              </el-button>
            </div>
            <div class="toolbar-right">
              <el-tag v-if="lyricsLineCount > 0" type="success" size="small">
                共 {{ lyricsLineCount }} 行歌词
              </el-tag>
            </div>
          </div>

          <!-- 歌词编辑区域 -->
          <div class="lyrics-edit-box">
            <div class="lyrics-input-wrapper">
              <el-input
                v-model="editForm.lyrics"
                type="textarea"
                :rows="showLyricsPreview ? 8 : 12"
                placeholder="请输入LRC格式歌词，例如：&#10;[00:12.50]第一句歌词&#10;[00:15.30]第二句歌词&#10;[00:18.20]第三句歌词"
                class="lyrics-textarea"
                resize="vertical"
                @input="handleLyricsInput"
              />
            </div>

            <!-- 歌词预览区域 -->
            <div v-if="showLyricsPreview && lyricsPreviewLines.length > 0" class="lyrics-preview-box">
              <div class="preview-header">
                <el-icon><View /></el-icon>
                <span>歌词预览</span>
                <el-tag type="info" size="small">{{ lyricsPreviewLines.length }} 行</el-tag>
              </div>
              <div class="preview-content">
                <div 
                  v-for="(line, index) in lyricsPreviewLines.slice(0, 10)" 
                  :key="index"
                  class="preview-line"
                >
                  <span class="preview-time">{{ formatPreviewTime(line.time) }}</span>
                  <span class="preview-text">{{ line.text }}</span>
                </div>
                <div v-if="lyricsPreviewLines.length > 10" class="preview-more">
                  还有 {{ lyricsPreviewLines.length - 10 }} 行...
                </div>
              </div>
            </div>
            
            <!-- 格式说明 -->
            <div class="lyrics-format-tip">
              <div class="tip-header">
                <el-icon><InfoFilled /></el-icon>
                <span class="tip-title">格式说明</span>
              </div>
              <div class="tip-content">
                <div class="tip-item">
                  <span class="tip-label">格式：</span>
                  <code class="tip-code">[mm:ss.xx]歌词内容</code>
                </div>
                <div class="tip-item">
                  <span class="tip-label">说明：</span>
                  <span class="tip-text">mm为分钟（00-99），ss为秒（00-59），xx为百分秒（00-99）</span>
                </div>
                <div class="tip-item">
                  <span class="tip-label">示例：</span>
                  <code class="tip-code-example">[00:12.50]这一路上走走停停<br/>[00:27.24]顺着少年漂流的痕迹</code>
                </div>
                <div class="tip-link">
                  <el-link 
                    href="https://lzltool.com/audio-lrc" 
                    target="_blank" 
                    type="primary" 
                    :icon="Link"
                    style="font-size: 13px;"
                  >
                    使用在线工具生成LRC歌词
                  </el-link>
                </div>
              </div>
            </div>
          </div>

          <!-- 隐藏的LRC文件上传 -->
          <input
            ref="lrcFileInputRef"
            type="file"
            accept=".lrc,.txt"
            style="display: none"
            @change="handleLRCFileChange"
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEdit">取消</el-button>
          <NextButton 
            variant="primary"
            @click="handleSaveEdit"
            :loading="saving"
            :disabled="!editForm.title || !editForm.artist || (!editForm.id && !audioFile)"
          >
            {{ saving ? '保存中...' : (editForm.id ? '更新' : '添加到音乐库') }}
          </NextButton>
        </div>
      </template>
    </NextModal>
    
    <!-- 替换音频文件对话框 -->
    <NextModal
      v-model="uploadDialogVisible"
      title="替换音频文件"
      size="lg"
      :mask-closable="false"
      @close="handleDialogClose"
      class="upload-dialog"
    >
      <!-- 当前音频文件信息 -->
      <div v-if="currentMusicInfo && currentMusicInfo.fileUrl" class="current-file-info">
        <div class="section-header">
          <el-icon class="section-icon"><Document /></el-icon>
          <h4 class="section-title">当前音频文件</h4>
        </div>
        <div class="file-info-card">
          <div class="file-info-grid">
            <div class="file-info-item">
              <span class="label">
                <el-icon><Link /></el-icon>
                文件URL:
              </span>
              <span class="value" :title="currentMusicInfo.fileUrl">
                {{ currentMusicInfo.fileUrl || '未上传' }}
              </span>
            </div>
            <div class="file-info-item">
              <span class="label">
                <el-icon><Document /></el-icon>
                文件名:
              </span>
              <span class="value">{{ currentMusicInfo.fileName || '未知' }}</span>
            </div>
            <div v-if="currentMusicInfo.duration" class="file-info-item">
              <span class="label">
                <el-icon><Timer /></el-icon>
                时长:
              </span>
              <span class="value">{{ formatDuration(currentMusicInfo.duration) }}</span>
            </div>
          </div>
          <div v-if="currentMusicInfo.fileUrl" class="file-preview">
            <div class="preview-label">音频预览</div>
            <audio 
              :src="currentMusicInfo.fileUrl" 
              controls 
              class="audio-preview"
              preload="metadata"
            ></audio>
          </div>
        </div>
      </div>
      
      <el-divider v-if="currentMusicInfo && currentMusicInfo.fileUrl" class="section-divider">
        <el-icon><ArrowDown /></el-icon>
      </el-divider>
      
      <!-- 上传新音频文件 -->
      <div class="upload-section">
        <div class="section-header">
          <el-icon class="section-icon"><UploadFilled /></el-icon>
          <h4 class="section-title">上传新音频文件</h4>
        </div>
        
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          accept=".mp3,.wav,.flac,.m4a,.aac,.ogg,.mgg"
          :limit="1"
          drag
          class="upload-zone-wrapper"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              <el-icon><InfoFilled /></el-icon>
              支持 MP3、WAV、FLAC、M4A、AAC、OGG、MGG 格式，文件大小不超过 50MB
            </div>
          </template>
        </el-upload>
        
        <!-- 上传进度 -->
        <div v-if="uploadProgress > 0" class="upload-progress">
          <el-progress 
            :percentage="uploadProgress" 
            :status="uploadProgress === 100 ? 'success' : undefined"
            :stroke-width="8"
            striped
            :striped-flow="uploadProgress < 100"
          />
          <p class="progress-text">
            <el-icon v-if="uploadProgress < 100"><Loading /></el-icon>
            <el-icon v-else><CircleCheck /></el-icon>
            {{ uploadProgress === 100 ? '上传完成' : `上传中... ${uploadProgress}%` }}
          </p>
        </div>
        
        <!-- 文件信息预览 -->
        <div v-if="uploadFile" class="file-preview-info">
          <div class="preview-header">
            <el-icon><DocumentChecked /></el-icon>
            <span>已选择文件</span>
          </div>
          <el-descriptions :column="1" size="small" border class="file-descriptions">
            <el-descriptions-item label="文件名">
              <el-icon><Document /></el-icon>
              {{ uploadFile.name }}
            </el-descriptions-item>
            <el-descriptions-item label="文件大小">
              <el-icon><FolderOpened /></el-icon>
              {{ formatFileSize(uploadFile.size) }}
            </el-descriptions-item>
            <el-descriptions-item label="文件类型">
              <el-icon><Files /></el-icon>
              {{ uploadFile.type || '未知' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="handleCancelUpload">取消</el-button>
        <NextButton
          variant="primary"
          :loading="uploading"
          :disabled="!uploadFile"
          @click="handleUploadSubmit"
        >
          {{ uploading ? '上传中...' : '确认替换' }}
        </NextButton>
      </template>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { 
  Search, 
  UploadFilled, 
  ArrowDown, 
  Document, 
  Link, 
  Timer, 
  InfoFilled,
  Loading,
  CircleCheck,
  DocumentChecked,
  FolderOpened,
  Files,
  Plus,
  Edit,
  Delete,
  VideoPlay,
  Picture,
  Camera,
  EditPen,
  VideoCamera,
  User,
  Folder,
  Headset,
  View
} from '@element-plus/icons-vue'

// 获取文件类型
const getFileType = (fileName: string) => {
  const extension = fileName.split('.').pop()?.toLowerCase()
  const typeMap: Record<string, string> = {
    'mp3': 'audio/mpeg',
    'wav': 'audio/wav',
    'flac': 'audio/flac',
    'm4a': 'audio/mp4',
    'aac': 'audio/aac',
    'ogg': 'audio/ogg',
    'mgg': 'application/octet-stream'
  }
  return typeMap[extension || ''] || '未知'
}
import { ElMessage, ElMessageBox } from 'element-plus'
import NextCard from '@/components/ui/NextCard.vue'
import NextInput from '@/components/ui/NextInput.vue'
import NextModal from '@/components/ui/NextModal.vue'
import NextButton from '@/components/ui/NextButton.vue'
import request from '@/api/request'
import axios from 'axios'
import { getToken } from '@/utils/auth'
import { formatFileSize, formatDuration } from '@/utils/format'
import { parseLRC, type LyricsLine } from '@/utils/lyrics'

const loading = ref(false)
const searchKeyword = ref('')
const filterGenre = ref('')
const musicList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const uploadRef = ref()
const currentMusicId = ref(0)
const uploadFile = ref<File | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const currentMusicInfo = ref<any>(null)
const defaultCover = 'https://via.placeholder.com/64x64?text=Music'

const editForm = reactive({
  id: 0,
  title: '',
  artist: '',
  album: '',
  genre: '',
  coverUrl: '',
  lyrics: ''
})

const coverFile = ref<File | null>(null)
const coverPreviewUrl = ref<string>('')
const coverUploadRef = ref()
const audioFile = ref<File | null>(null)
const audioUploadRef = ref()
const saving = ref(false)
const showLyricsPreview = ref(false)
const lyricsPreviewLines = ref<LyricsLine[]>([])
const lyricsLineCount = ref(0)
const lrcFileInputRef = ref<HTMLInputElement>()

const loadMusic = async () => {
  loading.value = true
  try {
    const result: any = await request.get('/admin/music', {
      params: {
        keyword: searchKeyword.value,
        genre: filterGenre.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    musicList.value = result.list || []
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
    artist: '',
    album: '',
    genre: '',
    coverUrl: '',
    lyrics: ''
  })
  coverFile.value = null
  coverPreviewUrl.value = ''
  audioFile.value = null
  editDialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(editForm, row)
  coverFile.value = null
  // 如果有封面URL，显示封面预览
  coverPreviewUrl.value = row.coverUrl || ''
  audioFile.value = null
  // 清空上传组件
  if (coverUploadRef.value) {
    coverUploadRef.value.clearFiles()
  }
  if (audioUploadRef.value) {
    audioUploadRef.value.clearFiles()
  }
  editDialogVisible.value = true
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
  coverFile.value = null
  coverPreviewUrl.value = ''
  audioFile.value = null
  if (coverUploadRef.value) {
    coverUploadRef.value.clearFiles()
  }
  if (audioUploadRef.value) {
    audioUploadRef.value.clearFiles()
  }
}

const handleCoverChange = (file: any) => {
  const isImage = file.raw.type.startsWith('image/')
  const isLt5M = file.raw.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    coverUploadRef.value?.clearFiles()
    return
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    coverUploadRef.value?.clearFiles()
    return
  }

  coverFile.value = file.raw
  // 创建预览URL
  const reader = new FileReader()
  reader.onload = (e) => {
    coverPreviewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
}

const handleCoverRemove = () => {
  coverFile.value = null
  coverPreviewUrl.value = editForm.coverUrl || ''
  coverUploadRef.value?.clearFiles()
}

const handleAudioChange = (file: any) => {
  const allowedExtensions = ['mp3', 'wav', 'flac', 'm4a', 'aac', 'ogg', 'mgg']
  const fileExtension = file.raw.name.split('.').pop()?.toLowerCase()
  
  if (!allowedExtensions.includes(fileExtension || '')) {
    ElMessage.warning('不支持的文件格式，请上传音频文件')
    audioUploadRef.value?.clearFiles()
    return
  }
  
  if (file.raw.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过50MB')
    audioUploadRef.value?.clearFiles()
    return
  }
  
  audioFile.value = file.raw
}

const handleAudioRemove = () => {
  audioFile.value = null
  audioUploadRef.value?.clearFiles()
}

const handleSaveEdit = async () => {
  // 验证必填字段
  if (!editForm.title || !editForm.title.trim()) {
    ElMessage.warning('请填写歌曲名')
    return
  }
  if (!editForm.artist || !editForm.artist.trim()) {
    ElMessage.warning('请填写歌手名')
    return
  }
  
  // 新增模式下，音频文件必填
  if (!editForm.id && !audioFile.value) {
    ElMessage.warning('请上传音频文件')
    return
  }

  saving.value = true
  try {
    const token = getToken()
    
    if (editForm.id) {
      // 编辑模式：先更新基本信息
      await request.put(`/admin/music/${editForm.id}`, {
        title: editForm.title.trim(),
        artist: editForm.artist.trim(),
        album: editForm.album?.trim() || null,
        genre: editForm.genre || null,
        coverUrl: editForm.coverUrl?.trim() || null,
        lyrics: editForm.lyrics?.trim() || null
      })

      // 如果有新封面，上传封面
      if (coverFile.value) {
        const coverFormData = new FormData()
        coverFormData.append('file', coverFile.value)
        const coverResponse = await axios.post(
          `/api/admin/music/${editForm.id}/cover`,
          coverFormData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
              'Authorization': token ? `Bearer ${token}` : ''
            }
          }
        )
        // 更新封面URL
        if (coverResponse.data.code === 200 && coverResponse.data.data) {
          editForm.coverUrl = coverResponse.data.data.fileUrl
        }
      } else if (editForm.coverUrl) {
        // 如果没有新封面但有URL，确保URL被更新
        await request.put(`/admin/music/${editForm.id}`, {
          coverUrl: editForm.coverUrl.trim()
        })
      }

      // 如果有新音频文件，上传音频（使用现有的上传接口）
      if (audioFile.value) {
        const audioFormData = new FormData()
        audioFormData.append('file', audioFile.value)
        await axios.post(
          `/api/admin/music/${editForm.id}/upload`,
          audioFormData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
              'Authorization': token ? `Bearer ${token}` : ''
            }
          }
        )
      }
    } else {
      // 新增模式：使用FormData上传，音频文件必填
      const formData = new FormData()
      formData.append('title', editForm.title.trim())
      formData.append('artist', editForm.artist.trim())
      formData.append('audioFile', audioFile.value!)
      if (editForm.album?.trim()) formData.append('album', editForm.album.trim())
      if (editForm.genre) formData.append('genre', editForm.genre)
      if (editForm.lyrics?.trim()) {
        formData.append('lyrics', editForm.lyrics.trim())
      }
      if (coverFile.value) {
        formData.append('coverFile', coverFile.value)
      } else if (editForm.coverUrl?.trim()) {
        formData.append('coverUrl', editForm.coverUrl.trim())
      }
      
      const response = await axios.post(
        '/api/admin/music',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': token ? `Bearer ${token}` : ''
          }
        }
      )
      
      if (response.data.code === 200) {
        ElMessage.success('歌曲添加成功！已添加到音乐库')
      } else {
        throw new Error(response.data.message || '添加失败')
      }
    }

    ElMessage.success(editForm.id ? '更新成功' : '歌曲添加成功！已添加到音乐库')
    editDialogVisible.value = false
    handleCancelEdit()
    loadMusic()
  } catch (error: any) {
    const errorMsg = error.response?.data?.message || error.message || '保存失败'
    ElMessage.error(errorMsg)
  } finally {
    saving.value = false
  }
}

const handleUploadFile = async (id: number) => {
  // 先完全清理所有状态，避免残留数据
  resetUploadState()
  
  currentMusicId.value = id
  
  try {
    // 获取当前音乐文件信息
    const result = await request.get(`/admin/music/${id}/file-info`)
    currentMusicInfo.value = result
  } catch (error: any) {
    ElMessage.warning('获取文件信息失败: ' + (error.message || '未知错误'))
    currentMusicInfo.value = null
  }
  
  uploadDialogVisible.value = true
}

// 重置上传状态
const resetUploadState = () => {
  uploadFile.value = null
  uploadProgress.value = 0
  uploading.value = false
  currentMusicInfo.value = null
  // 清空上传组件的文件列表
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const handleFileChange = (file: any) => {
  // 验证文件类型
  const allowedExtensions = ['mp3', 'wav', 'flac', 'm4a', 'aac', 'ogg', 'mgg']
  const fileExtension = file.raw.name.split('.').pop()?.toLowerCase()
  
  if (!allowedExtensions.includes(fileExtension || '')) {
    ElMessage.warning('不支持的文件格式，请上传音频文件')
    uploadRef.value?.clearFiles()
    return
  }
  
  // 验证文件大小
  if (file.raw.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过50MB')
    uploadRef.value?.clearFiles()
    return
  }
  
  uploadFile.value = file.raw
}

const handleFileRemove = () => {
  uploadFile.value = null
  uploadProgress.value = 0
}

// 对话框关闭时的处理
const handleDialogClose = () => {
  resetUploadState()
}

// 监听对话框关闭，确保状态清理
watch(() => uploadDialogVisible.value, (newVal) => {
  if (!newVal) {
    // 对话框关闭时，延迟清理以确保动画完成
    setTimeout(() => {
      resetUploadState()
    }, 300)
  }
})

// 监听封面URL变化，更新预览
watch(() => editForm.coverUrl, (newUrl) => {
  if (newUrl && !coverFile.value) {
    coverPreviewUrl.value = newUrl
  }
})

const handleCancelUpload = () => {
  resetUploadState()
  uploadDialogVisible.value = false
}

const handleUploadSubmit = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  // 确认替换
  try {
    await ElMessageBox.confirm(
      '确定要替换当前音频文件吗？此操作不可撤销，旧文件将被删除。',
      '确认替换',
      {
        type: 'warning',
        confirmButtonText: '确认替换',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return // 用户取消
  }
  
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    
    // 使用 axios 直接调用以支持上传进度
    const token = getToken()
    const response = await axios.post(
      `/api/admin/music/${currentMusicId.value}/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        onUploadProgress: (progressEvent: any) => {
          if (progressEvent.total) {
            uploadProgress.value = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            )
          }
        }
      }
    )
    
    // 处理响应
    const res = response.data
    if (res.code === 200) {
      ElMessage.success(res.message || '音频文件替换成功')
      resetUploadState()
      uploadDialogVisible.value = false
      loadMusic() // 刷新列表
    } else {
      throw new Error(res.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || error.message || '上传失败')
    // 上传失败时也重置进度
    uploadProgress.value = 0
  } finally {
    uploading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这首音乐吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/admin/music/${id}`)
    ElMessage.success('删除成功')
    loadMusic()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 处理歌词输入
const handleLyricsInput = () => {
  if (editForm.lyrics) {
    lyricsPreviewLines.value = parseLRC(editForm.lyrics)
    lyricsLineCount.value = lyricsPreviewLines.value.filter(line => line.time >= 0).length
  } else {
    lyricsPreviewLines.value = []
    lyricsLineCount.value = 0
  }
}

// 格式化预览时间
const formatPreviewTime = (time: number) => {
  if (time < 0) return '--:--'
  const minutes = Math.floor(time / 60)
  const seconds = Math.floor(time % 60)
  const centiseconds = Math.floor((time % 1) * 100)
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}.${String(centiseconds).padStart(2, '0')}`
}

// 上传LRC文件
const handleUploadLRC = () => {
  lrcFileInputRef.value?.click()
}

// 处理LRC文件选择
const handleLRCFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  const fileName = file.name.toLowerCase()
  if (!fileName.endsWith('.lrc') && !fileName.endsWith('.txt')) {
    ElMessage.error('请上传LRC或TXT格式的文件')
    target.value = ''
    return
  }

  // 验证文件大小（最大1MB）
  if (file.size > 1024 * 1024) {
    ElMessage.error('文件大小不能超过1MB')
    target.value = ''
    return
  }

  try {
    const text = await file.text()
    editForm.lyrics = text
    handleLyricsInput()
    ElMessage.success('LRC文件导入成功')
  } catch (error: any) {
    ElMessage.error('读取文件失败: ' + (error.message || '未知错误'))
  } finally {
    target.value = ''
  }
}

// 清空歌词
const handleClearLyrics = () => {
  ElMessageBox.confirm('确定要清空歌词吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(() => {
    editForm.lyrics = ''
    handleLyricsInput()
    ElMessage.success('已清空歌词')
  }).catch(() => {})
}

// 监听歌词变化
watch(() => editForm.lyrics, () => {
  handleLyricsInput()
}, { immediate: true })

onMounted(() => {
  loadMusic()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.music-manage-page {
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
    
    // 输入框样式
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
      
      &:active {
        transform: translateY(0);
      }
    }
  }
}

// 表格容器
.table-container {
  padding: 20px;
  
  :deep(.el-table) {
    background: transparent;
    --el-table-border-color: transparent;
    --el-table-header-bg-color: rgba(249, 250, 251, 0.8);
    --el-table-row-hover-bg-color: rgba(0, 195, 255, 0.05);
    
    // 表头样式
    th.el-table__cell {
      background: rgba(249, 250, 251, 0.8);
      font-weight: 600;
      color: var(--text-gray);
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      padding: 16px 0;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    
    .el-table__inner-wrapper::before {
      display: none;
    }
    
    // 表格体
    .el-table__body {
      tr {
        background: transparent;
        border-bottom: 1px solid rgba(0, 0, 0, 0.04);
        transition: all 0.2s ease;
        
        &:hover {
          background: rgba(0, 195, 255, 0.05);
          
          .action-buttons {
            opacity: 1;
          }
        }
        
        td {
          padding: 16px 0;
          border-bottom: 1px solid rgba(0, 0, 0, 0.04);
          
          &:first-child {
            padding-left: 0;
          }
          
          &:last-child {
            padding-right: 0;
          }
        }
      }
    }
  }
  
  .song-info-cell {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .cover-wrapper {
      width: 48px;
      height: 48px;
      border-radius: 8px;
      overflow: hidden;
      position: relative;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
      flex-shrink: 0;
      
      .cover-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      // 播放遮罩
      .play-mask {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.2s;
        color: #fff;
        cursor: pointer;
        
        .el-icon {
          font-size: 20px;
        }
      }
      
      &:hover .play-mask {
        opacity: 1;
      }
    }
    
    .text-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
      
      .song-title {
        font-weight: 600;
        color: var(--text-dark);
        font-size: 14px;
        @include text-ellipsis(1);
      }
      
      .song-artist {
        font-size: 12px;
        color: var(--text-gray);
        @include text-ellipsis(1);
      }
    }
  }
  
  // 状态圆点
  .status-dot {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    
    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    
    &.online {
      color: #10B981;
      
      .dot {
        background: #10B981;
        box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
      }
    }
    
    &.offline {
      color: #9CA3AF;
      
      .dot {
        background: #9CA3AF;
      }
    }
  }
  
  // 操作按钮
  .action-buttons {
    opacity: 0.7;
    transition: opacity 0.2s;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    
    .el-button {
      border: 1px solid rgba(0, 0, 0, 0.08);
      background: rgba(255, 255, 255, 0.9);
      transition: all 0.2s;
      
      &.el-button--default {
        &:hover {
          background: var(--bg-light);
          border-color: var(--primary-color);
          color: var(--primary-color);
        }
      }
      
      &.el-button--success {
        &:hover {
          background: rgba(16, 185, 129, 0.1);
          border-color: #10B981;
          color: #10B981;
        }
      }
      
      &.el-button--danger {
        &:hover {
          background: rgba(239, 68, 68, 0.1);
          border-color: #EF4444;
          color: #EF4444;
        }
      }
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 0 0;
    
    :deep(.el-pagination) {
      .btn-prev, .btn-next, .el-pager li {
        background: rgba(255, 255, 255, 0.8);
        border: 1px solid rgba(0, 0, 0, 0.06);
        
        &:hover {
          background: #fff;
        }
      }
    }
  }
}

// 上传对话框样式
.upload-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  
  .section-icon {
    font-size: 20px;
    color: var(--primary-from);
  }
  
  .section-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-dark);
    margin: 0;
  }
}

.section-divider {
  margin: 24px 0;
  
  :deep(.el-divider__text) {
    background: var(--bg-white);
    padding: 0 12px;
    
    .el-icon {
      color: var(--primary-from);
    }
  }
}

.current-file-info {
  margin-bottom: 24px;
  
  .file-info-card {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.05), rgba(30, 64, 175, 0.05));
    padding: 20px;
    border-radius: var(--radius-md);
    border: 1px solid rgba(59, 130, 246, 0.2);
    box-shadow: var(--shadow-sm);
    
    .file-info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 16px;
      margin-bottom: 20px;
    }
    
    .file-info-item {
      display: flex;
      flex-direction: column;
      gap: 6px;
      
      .label {
        font-weight: 600;
        color: var(--text-gray);
        font-size: 13px;
        display: flex;
        align-items: center;
        gap: 6px;
        
        .el-icon {
          font-size: 16px;
        }
      }
      
      .value {
        color: var(--text-dark);
        font-size: 14px;
        word-break: break-all;
        padding: 8px 12px;
        background: var(--bg-white);
        border-radius: var(--radius-sm);
        border: 1px solid var(--border-color);
      }
    }
    
    .file-preview {
      margin-top: 20px;
      padding-top: 20px;
      border-top: 2px solid var(--border-color);
      
      .preview-label {
        font-weight: 600;
        color: var(--text-dark);
        margin-bottom: 12px;
        font-size: 14px;
      }
      
      .audio-preview {
        width: 100%;
        height: 40px;
        border-radius: var(--radius-sm);
      }
    }
  }
}

.upload-section {
  .upload-zone-wrapper {
    :deep(.el-upload) {
      width: 100%;
    }
    
    :deep(.el-upload-dragger) {
      width: 100%;
      padding: 40px 20px;
      background: var(--bg-light);
      border: 2px dashed var(--border-color);
      border-radius: var(--radius-md);
      transition: all var(--transition-base);
      
      &:hover {
        border-color: var(--primary-from);
        background: rgba(59, 130, 246, 0.05);
      }
      
      .el-icon--upload {
        font-size: 48px;
        color: var(--primary-from);
        margin-bottom: 16px;
      }
      
      .el-upload__text {
        color: var(--text-dark);
        font-size: 14px;
        
        em {
          color: var(--primary-from);
          font-style: normal;
          font-weight: 600;
        }
      }
    }
    
    .el-upload__tip {
      margin-top: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      color: var(--text-gray);
      font-size: 12px;
      
      .el-icon {
        font-size: 14px;
      }
    }
  }
  
  .upload-progress {
    margin-top: 20px;
    padding: 20px;
    background: var(--bg-light);
    border-radius: var(--radius-md);
    
    :deep(.el-progress) {
      margin-bottom: 12px;
    }
    
    .progress-text {
      text-align: center;
      margin-top: 8px;
      color: var(--text-dark);
      font-size: 14px;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      
      .el-icon {
        font-size: 16px;
        
        &.is-loading {
          animation: rotating 2s linear infinite;
        }
      }
    }
  }
  
  .file-preview-info {
    margin-top: 20px;
    
    .preview-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-weight: 600;
      color: var(--text-dark);
      font-size: 14px;
      
      .el-icon {
        color: var(--primary-from);
        font-size: 18px;
      }
    }
    
    .file-descriptions {
      :deep(.el-descriptions__label) {
        font-weight: 600;
        color: var(--text-gray);
        display: flex;
        align-items: center;
        gap: 6px;
      }
      
      :deep(.el-descriptions__content) {
        display: flex;
        align-items: center;
        gap: 6px;
        color: var(--text-dark);
      }
    }
  }
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 音乐编辑对话框样式
.music-edit-dialog {
  :deep(.el-dialog__body) {
    padding: 32px;
    max-height: 80vh;
    overflow-y: auto;
  }
  
  :deep(.el-dialog__header) {
    padding: 24px 32px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    background: linear-gradient(135deg, rgba(0, 195, 255, 0.05), rgba(0, 136, 255, 0.05));
  }
  
  :deep(.el-dialog__title) {
    font-size: 22px;
    font-weight: 700;
    background: var(--primary-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

// 步骤式布局
.upload-step-section {
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 2px dashed rgba(0, 0, 0, 0.06);
  
  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }
  
  .step-header {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 20px;
    
    .step-number {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--primary-gradient);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      font-weight: 700;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(0, 195, 255, 0.3);
    }
    
    .step-title-wrapper {
      flex: 1;
      
      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 600;
        color: var(--text-dark);
        margin-bottom: 8px;
        
        .el-icon {
          font-size: 22px;
          color: var(--primary-color);
        }
      }
      
      .step-description {
        font-size: 13px;
        color: var(--text-gray);
        line-height: 1.6;
      }
    }
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 16px;
  
  .el-icon {
    font-size: 20px;
    color: var(--primary-color);
  }
}

// 封面上传区域（步骤3）
.cover-upload-section-main {
  .cover-upload-box {
    @include glass-card;
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(10px);
  }
  
  .cover-uploader {
    width: 100%;
    
    :deep(.el-upload) {
      width: 100%;
    }
  }
  
  .cover-preview {
    width: 100%;
    aspect-ratio: 1;
    border-radius: var(--radius-md);
    overflow: hidden;
    position: relative;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transition: all 0.3s;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .cover-mask {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      opacity: 0;
      transition: opacity 0.3s;
      color: #fff;
      backdrop-filter: blur(4px);
      
      .el-icon {
        font-size: 32px;
      }
      
      span {
        font-size: 14px;
        font-weight: 500;
      }
    }
    
    &:hover .cover-mask {
      opacity: 1;
    }
  }
  
  .cover-placeholder {
    width: 100%;
    aspect-ratio: 1;
    border: 2px dashed var(--border-color);
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    cursor: pointer;
    transition: all 0.3s;
    background: rgba(0, 195, 255, 0.02);
    
    .upload-icon {
      font-size: 48px;
      color: var(--primary-color);
    }
    
    .upload-text {
      font-size: 16px;
      font-weight: 500;
      color: var(--text-dark);
    }
    
    .upload-tip {
      font-size: 12px;
      color: var(--text-gray);
      text-align: center;
      line-height: 1.5;
    }
    
    &:hover {
      border-color: var(--primary-color);
      background: rgba(0, 195, 255, 0.05);
      transform: translateY(-2px);
    }
  }
  
  .cover-info {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: rgba(0, 195, 255, 0.05);
    border-radius: var(--radius-sm);
    font-size: 13px;
    color: var(--text-dark);
    
    .el-icon {
      color: var(--primary-color);
    }
    
    span {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
  
  .cover-url-input {
    :deep(.el-input__wrapper) {
      border-radius: var(--radius-sm);
    }
  }
}

// 歌词编辑区域（步骤4）
.lyrics-edit-section-main {
  .lyrics-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: linear-gradient(135deg, rgba(0, 195, 255, 0.05), rgba(0, 136, 255, 0.05));
    border-radius: var(--radius-md);
    border: 1px solid rgba(0, 195, 255, 0.15);
    
    .toolbar-left {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    
    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .lyrics-edit-box {
    @include glass-card;
    padding: 24px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(10px);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    
    .lyrics-input-wrapper {
      margin-bottom: 20px;
      
      :deep(.lyrics-textarea) {
        .el-textarea__inner {
          font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
          font-size: 13px;
          line-height: 1.8;
          background: rgba(255, 255, 255, 0.95);
          border: 1px solid var(--border-color);
          border-radius: var(--radius-sm);
          transition: all 0.2s;
          color: var(--text-dark);
          resize: vertical;
          
          &:hover {
            border-color: var(--primary-color);
            box-shadow: 0 0 0 1px rgba(0, 195, 255, 0.2);
          }
          
          &:focus {
            border-color: var(--primary-color);
            box-shadow: 0 0 0 2px rgba(0, 195, 255, 0.15);
          }
        }
      }
    }

    // 歌词预览区域
    .lyrics-preview-box {
      margin-bottom: 20px;
      padding: 16px;
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.05), rgba(5, 150, 105, 0.05));
      border: 1px solid rgba(16, 185, 129, 0.2);
      border-radius: var(--radius-md);
      max-height: 300px;
      overflow-y: auto;

      .preview-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        padding-bottom: 12px;
        border-bottom: 1px solid rgba(16, 185, 129, 0.15);
        font-weight: 600;
        color: var(--text-dark);
        font-size: 14px;

        .el-icon {
          color: #10B981;
          font-size: 18px;
        }
      }

      .preview-content {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .preview-line {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 6px 8px;
          background: rgba(255, 255, 255, 0.6);
          border-radius: 4px;
          font-size: 13px;
          transition: all 0.2s;

          &:hover {
            background: rgba(255, 255, 255, 0.9);
            transform: translateX(4px);
          }

          .preview-time {
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            color: #10B981;
            font-weight: 600;
            min-width: 70px;
            flex-shrink: 0;
          }

          .preview-text {
            color: var(--text-dark);
            flex: 1;
          }
        }

        .preview-more {
          text-align: center;
          color: var(--text-gray);
          font-size: 12px;
          padding: 8px;
          font-style: italic;
        }
      }

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.05);
        border-radius: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(16, 185, 129, 0.3);
        border-radius: 3px;

        &:hover {
          background: rgba(16, 185, 129, 0.5);
        }
      }
    }
    
    .lyrics-format-tip {
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.05), rgba(30, 64, 175, 0.05));
      border: 1px solid rgba(59, 130, 246, 0.2);
      border-radius: var(--radius-md);
      padding: 16px;
      
      .tip-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        padding-bottom: 12px;
        border-bottom: 1px solid rgba(59, 130, 246, 0.15);
        
        .el-icon {
          font-size: 18px;
          color: var(--primary-color);
        }
        
        .tip-title {
          font-size: 14px;
          font-weight: 600;
          color: var(--text-dark);
        }
      }
      
      .tip-content {
        display: flex;
        flex-direction: column;
        gap: 10px;
        
        .tip-item {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          font-size: 13px;
          line-height: 1.6;
          
          .tip-label {
            font-weight: 600;
            color: var(--text-gray);
            min-width: 50px;
            flex-shrink: 0;
          }
          
          .tip-text {
            color: var(--text-dark);
            flex: 1;
          }
          
          .tip-code {
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 12px;
            background: rgba(0, 0, 0, 0.05);
            padding: 2px 6px;
            border-radius: 4px;
            color: #E53E3E;
            font-weight: 500;
          }
          
          .tip-code-example {
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 12px;
            background: rgba(0, 0, 0, 0.05);
            padding: 8px 12px;
            border-radius: 6px;
            color: var(--text-dark);
            display: block;
            flex: 1;
            line-height: 1.8;
            border-left: 3px solid var(--primary-color);
          }
        }
        
        .tip-link {
          margin-top: 4px;
          padding-top: 12px;
          border-top: 1px solid rgba(59, 130, 246, 0.15);
        }
      }
    }
  }
}

// 表单区域（步骤2）
.form-section-main {
  .music-form {
    @include glass-card;
    padding: 24px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(10px);
    
    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-bottom: 20px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .form-item-half {
        margin-bottom: 0;
      }
    }
    
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
    
    :deep(.el-form-item__label) {
      font-weight: 600;
      color: var(--text-dark);
      font-size: 14px;
    }
    
    :deep(.el-input__wrapper) {
      border-radius: var(--radius-sm);
      transition: all 0.2s;
      background: rgba(255, 255, 255, 0.9);
      
      &:hover {
        box-shadow: 0 0 0 1px rgba(0, 195, 255, 0.3);
        background: #fff;
      }
      
      &.is-focus {
        box-shadow: 0 0 0 2px rgba(0, 195, 255, 0.2);
        background: #fff;
      }
    }
    
    :deep(.el-select) {
      .el-input__wrapper {
        &:hover {
          box-shadow: 0 0 0 1px rgba(0, 195, 255, 0.3);
          background: #fff;
        }
      }
    }
  }
}

// 音频上传区域（步骤1）
.audio-upload-wrapper-main {
  .audio-upload-wrapper {
    :deep(.el-upload) {
      width: 100%;
    }
    
    :deep(.el-upload-dragger) {
      width: 100%;
      padding: 50px 30px;
      background: linear-gradient(135deg, rgba(0, 195, 255, 0.03), rgba(0, 136, 255, 0.03));
      backdrop-filter: blur(8px);
      border: 3px dashed var(--primary-color);
      border-radius: var(--radius-lg);
      transition: all var(--transition-base);
      position: relative;
      
      &:hover {
        border-color: var(--primary-color);
        background: linear-gradient(135deg, rgba(0, 195, 255, 0.08), rgba(0, 136, 255, 0.08));
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0, 195, 255, 0.15);
      }
      
      .el-icon--upload {
        font-size: 64px;
        color: var(--primary-color);
        margin-bottom: 20px;
        animation: pulse 2s ease-in-out infinite;
      }
      
      .el-upload__text {
        color: var(--text-dark);
        font-size: 16px;
        font-weight: 500;
        
        em {
          color: var(--primary-color);
          font-style: normal;
          font-weight: 700;
        }
      }
    }
    
    .el-upload__tip {
      margin-top: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: var(--text-gray);
      font-size: 13px;
      padding: 12px;
      background: rgba(0, 195, 255, 0.05);
      border-radius: var(--radius-sm);
      
      .el-icon {
        font-size: 16px;
        color: var(--primary-color);
      }
    }
  }
  
  .audio-file-info-card {
    margin-top: 20px;
    padding: 20px;
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(5, 150, 105, 0.08));
    border-radius: var(--radius-md);
    border: 2px solid rgba(16, 185, 129, 0.2);
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.1);
    
    .file-info-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid rgba(16, 185, 129, 0.2);
      
      .success-icon {
        font-size: 24px;
        color: #10B981;
      }
      
      .file-info-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-dark);
      }
    }
    
    .file-info-content {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .file-info-row {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 12px;
        background: rgba(255, 255, 255, 0.8);
        border-radius: var(--radius-sm);
        
        .el-icon {
          color: var(--primary-color);
          font-size: 18px;
        }
        
        .file-label {
          font-size: 14px;
          color: var(--text-gray);
          font-weight: 500;
          min-width: 80px;
        }
        
        .file-value {
          flex: 1;
          font-size: 14px;
          color: var(--text-dark);
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
    
    .file-info-actions {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
      padding-top: 12px;
      border-top: 1px solid rgba(16, 185, 129, 0.2);
    }
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 2px solid rgba(0, 0, 0, 0.08);
  
  .el-button {
    border-radius: var(--radius-md);
    padding: 12px 32px;
    font-weight: 500;
    transition: all 0.3s;
    
    &.el-button--primary {
      background: var(--primary-gradient);
      border: none;
      box-shadow: 0 4px 12px rgba(0, 195, 255, 0.3);
      
      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(0, 195, 255, 0.4);
      }
      
      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
    
    &:not(.el-button--primary) {
      &:hover {
        background: rgba(0, 0, 0, 0.05);
      }
    }
  }
}
</style>

