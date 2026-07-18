<template>
  <div class="comment-section">
    <div class="comment-header">
      <h3 class="comment-title">评论 {{ commentCount }}</h3>
      <el-radio-group v-model="sortType" size="small" @change="loadComments">
        <el-radio-button label="time">最新</el-radio-button>
        <el-radio-button label="hot">最热</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 评论列表 -->
    <div v-if="loading" class="comment-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="comments.length > 0" class="comment-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @delete="handleDeleteComment"
        @like-change="handleLikeChange"
        @reply-submitted="loadComments"
      />
    </div>

    <el-empty v-else description="暂无评论，快来发表第一条评论吧！" />

    <!-- 分页 -->
    <div v-if="total > 0" class="comment-pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="prev, pager, next, sizes"
        small
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 评论输入 -->
    <div class="comment-input-area">
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="3"
        placeholder="期待你的神评论..."
        maxlength="500"
        show-word-limit
        @keyup.ctrl.enter="handleSubmitComment"
      />
      <div class="input-actions">
        <span class="input-tip">Ctrl + Enter 快速发布</span>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmitComment"
        >
          发布
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getCommentsByMusicId, submitComment, deleteComment } from '@/api/feedback'
import type { CommentVO } from '@/api/feedback'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import CommentItem from './CommentItem.vue'

const props = defineProps<{
  musicId: number
}>()

const emit = defineEmits<{
  'comment-count-change': [count: number]
}>()

const userStore = useUserStore()
const comments = ref<CommentVO[]>([])
const loading = ref(false)
const submitting = ref(false)
const commentCount = ref(0)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const sortType = ref<'time' | 'hot'>('time')
const commentContent = ref('')

// 加载评论列表
const loadComments = async () => {
  if (!props.musicId) return

  loading.value = true
  try {
    const result = await getCommentsByMusicId(props.musicId, {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortType: sortType.value
    })
    
    comments.value = result.list || []
    total.value = result.total || 0
    commentCount.value = total.value
    
    // 触发评论数量变化事件
    emit('comment-count-change', commentCount.value)
  } catch (error: any) {
    console.error('加载评论失败', error)
    ElMessage.error('加载评论失败')
  } finally {
    loading.value = false
  }
}

// 提交评论
const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  if (!props.musicId || props.musicId === 0) {
    console.error('❌ musicId无效:', props.musicId)
    ElMessage.error('歌曲ID无效，无法提交评论')
    return
  }

  submitting.value = true
  try {
    console.log('📝 提交评论，musicId:', props.musicId, 'content:', commentContent.value.trim())
    console.log('📝 请求URL:', `/api/user/music/${props.musicId}/comment`)
    const result = await submitComment(props.musicId, commentContent.value.trim())
    console.log('✅ 评论提交成功:', result)
    commentContent.value = ''
    ElMessage.success('评论发布成功')
    // 重新加载评论列表
    pageNum.value = 1
    await loadComments()
  } catch (error: any) {
    console.error('❌ 提交评论失败', error)
    console.error('错误状态码:', error.response?.status)
    console.error('错误URL:', error.config?.url)
    console.error('错误详情:', error.response?.data || error.message)
    const errorMsg = error?.response?.data?.message || error?.message || '提交评论失败'
    ElMessage.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

// 删除评论
const handleDeleteComment = async (commentId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteComment(commentId)
    ElMessage.success('删除成功')
    await loadComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评论失败', error)
      ElMessage.error('删除评论失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  loadComments()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  loadComments()
}

// 处理评论点赞变化
const handleLikeChange = (commentId: number, isLiked: boolean, likeCount: number) => {
  const comment = comments.value.find(c => c.id === commentId)
  if (comment) {
    comment.isLiked = isLiked
    comment.likeCount = likeCount
  }
}

watch(() => props.musicId, () => {
  if (props.musicId) {
    pageNum.value = 1
    loadComments()
  }
}, { immediate: true })

onMounted(() => {
  if (props.musicId) {
    loadComments()
  }
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.comment-section {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border: none !important;
  margin: 0;
  
  // 暗色模式
  [data-theme="dark"] & {
    background: rgba(15, 23, 42, 0.98) !important;
    border: none !important;
    margin: 0;
  }
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(0, 195, 255, 0.1);

  .comment-title {
    font-size: 20px;
    font-weight: 700;
    margin: 0;
    background: var(--primary-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    position: relative;
    
    &::after {
      content: '';
      position: absolute;
      bottom: -16px;
      left: 0;
      width: 50px;
      height: 3px;
      background: var(--primary-gradient);
      border-radius: 2px;
    }
  }
  
  :deep(.el-radio-group) {
    .el-radio-button {
      .el-radio-button__inner {
        border-radius: 8px;
        padding: 8px 16px;
        font-weight: 600;
        transition: all 0.3s;
        border: 1px solid rgba(0, 195, 255, 0.2);
        
        &:hover {
          border-color: rgba(0, 195, 255, 0.4);
          color: var(--primary-color);
        }
      }
      
      &.is-active {
        .el-radio-button__inner {
          background: var(--primary-gradient);
          border-color: transparent;
          color: #fff;
          box-shadow: 0 2px 8px rgba(0, 195, 255, 0.3);
        }
      }
    }
  }

  // 暗色模式
  [data-theme="dark"] & {
    border-bottom-color: rgba(255, 255, 255, 0.1);
    
    :deep(.el-radio-group) {
      .el-radio-button {
        .el-radio-button__inner {
          background: rgba(30, 41, 59, 0.6);
          border-color: rgba(255, 255, 255, 0.15);
          color: rgba(255, 255, 255, 0.8);
          
          &:hover {
            border-color: rgba(0, 195, 255, 0.5);
            color: var(--primary-color);
            background: rgba(0, 195, 255, 0.1);
          }
        }
        
        &.is-active {
          .el-radio-button__inner {
            background: var(--primary-gradient);
            border-color: transparent;
            color: #fff;
          }
        }
      }
    }
  }
}

.comment-loading {
  padding: 40px 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  
  :deep(.el-empty) {
    .el-empty__description {
      color: var(--text-gray);
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    :deep(.el-empty) {
      .el-empty__description {
        color: rgba(255, 255, 255, 0.6);
      }
      
      .el-empty__image {
        opacity: 0.6;
      }
    }
  }
}

.comment-list {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
  max-height: calc(100vh - 400px);
  padding-right: 12px;
  margin-right: -12px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.03);
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(
      180deg,
      rgba(0, 195, 255, 0.3) 0%,
      rgba(0, 136, 255, 0.4) 100%
    );
    border-radius: 10px;
    transition: all 0.3s;
    
    &:hover {
      background: linear-gradient(
        180deg,
        rgba(0, 195, 255, 0.5) 0%,
        rgba(0, 136, 255, 0.6) 100%
      );
    }
  }
  
  // 暗色模式滚动条
  [data-theme="dark"] & {
    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.05);
    }
  }
}

.comment-pagination {
  padding: 20px 0;
  display: flex;
  justify-content: center;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  margin-top: 20px;
  
  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 6px;
      transition: all 0.3s;
      
      &.is-active {
        background: var(--primary-gradient);
        color: #fff;
      }
      
      &:hover {
        color: var(--primary-color);
      }
    }
    
    .btn-prev,
    .btn-next {
      border-radius: 6px;
      transition: all 0.3s;
      
      &:hover {
        color: var(--primary-color);
      }
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    border-top-color: rgba(255, 255, 255, 0.1);

    :deep(.el-pagination) {
      .el-pager li {
        color: rgba(255, 255, 255, 0.7);
        background: rgba(30, 41, 59, 0.6);
        border: 1px solid rgba(255, 255, 255, 0.05);
        
        &.is-active {
          background: var(--primary-gradient);
          color: #fff;
          border: none;
        }
        
        &:hover:not(.is-active) {
          color: var(--primary-color);
          background: rgba(0, 195, 255, 0.1);
          border-color: rgba(0, 195, 255, 0.3);
        }
      }
      
      .btn-prev,
      .btn-next {
        color: rgba(255, 255, 255, 0.7);
        background: rgba(30, 41, 59, 0.6) !important;
        border: 1px solid rgba(255, 255, 255, 0.05);
        
        &:hover {
          color: var(--primary-color);
          background: rgba(0, 195, 255, 0.1) !important;
          border-color: rgba(0, 195, 255, 0.3);
        }
        
        &:disabled {
          color: rgba(255, 255, 255, 0.3);
          background: rgba(255, 255, 255, 0.05) !important;
        }
      }
      
      .el-pagination__total {
        color: rgba(255, 255, 255, 0.7);
      }

      // 修复分页器下拉框背景
      .el-select {
        .el-input__wrapper {
          background-color: rgba(30, 41, 59, 0.6) !important;
          box-shadow: none !important;
          border: 1px solid rgba(255, 255, 255, 0.15);
          
          &:hover {
            border-color: var(--primary-color);
          }

          &.is-focus {
            border-color: var(--primary-color);
            box-shadow: 0 0 0 1px var(--primary-color) inset !important;
          }
        }

        .el-input__inner {
          color: rgba(255, 255, 255, 0.9) !important;
        }
      }
    }
  }
}

.comment-input-area {
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  margin-top: 20px;

  :deep(.el-textarea) {
    .el-textarea__inner {
      border-radius: 12px;
      border: 2px solid rgba(0, 195, 255, 0.2);
      padding: 12px 16px;
      font-size: 14px;
      transition: all 0.3s;
      background: rgba(255, 255, 255, 0.95);
      
      &:focus {
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(0, 195, 255, 0.1);
      }
    }
  }

  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 14px;

    .input-tip {
      font-size: 12px;
      color: var(--text-gray);
      font-weight: 500;
    }
    
    :deep(.el-button--primary) {
      background: var(--primary-gradient);
      border: none;
      border-radius: 8px;
      padding: 10px 24px;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(0, 195, 255, 0.3);
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 195, 255, 0.4);
      }
      
      &:active {
        transform: translateY(0);
      }
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    border-top-color: rgba(255, 255, 255, 0.1);
    
    :deep(.el-textarea) {
      .el-textarea__inner {
        background: rgba(30, 41, 59, 0.8) !important;
        border-color: rgba(255, 255, 255, 0.2) !important;
        color: rgba(255, 255, 255, 0.9) !important;
        
        &:focus {
          border-color: var(--primary-color) !important;
          background: rgba(30, 41, 59, 0.95) !important;
        }
      }
      
      // 修复字数统计背景
      .el-input__count {
        background: transparent !important;
        color: rgba(255, 255, 255, 0.5) !important;
        bottom: 8px;
        right: 12px;
      }
    }

    .input-tip {
      color: rgba(255, 255, 255, 0.5) !important;
    }
  }
}
</style>

<!-- 全局样式覆盖 - 解决分页器下拉框背景顽固问题 -->
<style lang="scss">
[data-theme="dark"] {
  .comment-section {
    .el-pagination {
      .el-select {
        .el-input__wrapper {
          background-color: rgba(30, 41, 59, 0.6) !important;
          box-shadow: none !important;
          border: 1px solid rgba(255, 255, 255, 0.15) !important;
        }

        .el-input__inner {
          color: rgba(255, 255, 255, 0.9) !important;
        }
      }
    }
  }
}
</style>

