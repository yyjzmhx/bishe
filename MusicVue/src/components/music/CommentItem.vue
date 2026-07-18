<template>
  <div class="comment-item">
    <el-avatar :src="comment.avatar || defaultAvatar" :size="40" class="comment-avatar">
      <el-icon><User /></el-icon>
    </el-avatar>
    
    <div class="comment-content">
      <div class="comment-header">
        <span class="comment-author">{{ comment.nickname || comment.username }}</span>
        <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
      </div>
      
      <div class="comment-text">{{ comment.content }}</div>
      
      <div class="comment-actions">
        <el-button 
          text 
          size="small" 
          :class="{ 'liked': isLiked }"
          :loading="liking"
          :disabled="isOwnComment"
          @click="handleLike"
        >
          <el-icon>
            <StarFilled v-if="isLiked" />
            <Star v-else />
          </el-icon>
          <span>{{ likeCount > 0 ? likeCount : '' }}</span>
          <span class="action-text">{{ isLiked ? '已赞' : isOwnComment ? '不能给自己点赞' : '点赞' }}</span>
        </el-button>
        <el-button text size="small" @click="handleReply">
          <el-icon><ChatDotRound /></el-icon>
          <span class="action-text">{{ showReplies ? '收起回复' : '回复' }}</span>
          <span v-if="comment.replyCount && comment.replyCount > 0" class="reply-count">({{ comment.replyCount }})</span>
        </el-button>
        <el-button
          v-if="isOwnComment"
          text
          size="small"
          type="danger"
          @click="handleDelete"
        >
          <el-icon><Delete /></el-icon>
          <span class="action-text">删除</span>
        </el-button>
      </div>
      
      <!-- 回复列表 -->
      <div v-if="showReplies" class="replies-container">
        <div
          v-for="reply in replies"
          :key="reply.id"
          class="reply-item"
        >
          <el-avatar :src="reply.avatar || defaultAvatar" :size="32" class="reply-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="reply-content">
            <div class="reply-header">
              <span class="reply-author">{{ reply.nickname || reply.username }}</span>
              <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
            </div>
            <div class="reply-text">{{ reply.content }}</div>
          </div>
        </div>
      </div>
      
      <!-- 回复输入框 -->
      <div v-if="showReplyInput" class="reply-input-container">
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="2"
          placeholder="写下你的回复..."
          maxlength="500"
          show-word-limit
          @keyup.ctrl.enter="handleSubmitReply"
        />
        <div class="reply-input-actions">
          <el-button size="small" @click="cancelReply">取消</el-button>
          <el-button
            type="primary"
            size="small"
            :loading="replying"
            @click="handleSubmitReply"
          >
            回复
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { User, Star, StarFilled, ChatDotRound, Delete } from '@element-plus/icons-vue'
import type { CommentVO } from '@/api/feedback'
import { useUserStore } from '@/store/user'
import { toggleCommentLike, replyComment, getCommentReplies } from '@/api/feedback'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  comment: CommentVO
}>()

const emit = defineEmits<{
  delete: [commentId: number]
  'like-change': [commentId: number, isLiked: boolean, likeCount: number]
  reply: [commentId: number]
  'reply-submitted': []
}>()

const userStore = useUserStore()
const defaultAvatar = 'https://via.placeholder.com/40x40?text=User'

const isOwnComment = computed(() => {
  return userStore.userInfo?.id === props.comment.userId
})

const isLiked = ref(props.comment.isLiked || false)
const likeCount = ref(props.comment.likeCount || 0)
const liking = ref(false)
const showReplyInput = ref(false)
const showReplies = ref(false)
const replies = ref<CommentVO[]>([])
const replyContent = ref('')
const replying = ref(false)

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  }
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  // 不能给自己点赞
  if (isOwnComment.value) {
    ElMessage.warning('不能给自己点赞')
    return
  }
  
  if (liking.value) return
  
  liking.value = true
  try {
    const result = await toggleCommentLike(props.comment.id)
    isLiked.value = result.isLiked
    likeCount.value = result.isLiked ? (likeCount.value + 1) : Math.max(0, likeCount.value - 1)
    
    emit('like-change', props.comment.id, result.isLiked, likeCount.value)
  } catch (error: any) {
    console.error('点赞失败', error)
    const errorMsg = error?.response?.data?.message || error?.message || '操作失败'
    ElMessage.error(errorMsg)
  } finally {
    liking.value = false
  }
}

const handleReply = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  // 切换回复输入框显示状态
  showReplyInput.value = !showReplyInput.value
  
  // 如果显示回复输入框，且还没有加载回复列表，先加载
  if (showReplyInput.value && !showReplies.value) {
    if (props.comment.replyCount && props.comment.replyCount > 0) {
      await loadReplies()
    } else {
      // 即使没有回复，也显示回复列表容器（为空）
      showReplies.value = true
    }
  }
}

const loadReplies = async () => {
  try {
    const result = await getCommentReplies(props.comment.id)
    replies.value = result || []
    showReplies.value = true
  } catch (error: any) {
    console.error('加载回复失败', error)
  }
}

const handleSubmitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  replying.value = true
  try {
    await replyComment(props.comment.id, replyContent.value.trim())
    ElMessage.success('回复成功')
    replyContent.value = ''
    showReplyInput.value = false
    
    // 重新加载回复列表
    await loadReplies()
    
    // 通知父组件刷新评论列表
    emit('reply-submitted')
  } catch (error: any) {
    console.error('回复失败', error)
    ElMessage.error('回复失败')
  } finally {
    replying.value = false
  }
}

const cancelReply = () => {
  showReplyInput.value = false
  replyContent.value = ''
}

const handleDelete = () => {
  emit('delete', props.comment.id)
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.comment-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;

  &:last-child {
    border-bottom: none;
  }
  
  &:hover {
    background: rgba(0, 0, 0, 0.02);
    border-radius: 12px;
    padding-left: 8px;
    padding-right: 8px;
    margin: 0 -8px;
  }

  .comment-avatar {
    flex-shrink: 0;
    border: 2px solid rgba(0, 195, 255, 0.2);
    transition: all 0.3s;
    
    &:hover {
      border-color: rgba(0, 195, 255, 0.5);
      transform: scale(1.05);
    }
  }

  .comment-content {
    flex: 1;
    min-width: 0;

    .comment-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 10px;

      .comment-author {
        font-weight: 600;
        color: var(--text-dark);
        font-size: 15px;
        transition: color 0.3s;
        
        &:hover {
          color: var(--primary-color);
        }
      }

      .comment-time {
        font-size: 12px;
        color: var(--text-gray);
      }
    }

    .comment-text {
      color: var(--text-dark);
      font-size: 14px;
      line-height: 1.7;
      word-break: break-word;
      margin-bottom: 12px;
      white-space: pre-wrap;
    }

    .comment-actions {
      display: flex;
      gap: 20px;
      align-items: center;

      :deep(.el-button) {
        color: var(--text-gray);
        padding: 6px 12px;
        border-radius: 8px;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        
        .el-icon {
          font-size: 16px;
          transition: transform 0.3s;
        }
        
        .action-text {
          font-size: 13px;
        }
        
        .reply-count {
          font-size: 12px;
          color: var(--primary-color);
        }

        &:hover:not(:disabled) {
          color: var(--primary-color);
          background: rgba(0, 195, 255, 0.08);
          transform: translateY(-1px);
          
          .el-icon {
            transform: scale(1.1);
          }
        }
        
        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
        
        &.liked {
          color: var(--primary-color);
          
          .el-icon {
            color: var(--primary-color);
          }
        }
        
        &.el-button--danger {
          color: var(--danger-color);
          
          &:hover {
            background: rgba(239, 68, 68, 0.1);
            color: var(--danger-color);
          }
        }
      }
    }
    
    .replies-container {
      margin-top: 16px;
      padding-left: 16px;
      border-left: 2px solid rgba(0, 195, 255, 0.2);
      
      .reply-item {
        display: flex;
        gap: 10px;
        padding: 12px 0;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        
        &:last-child {
          border-bottom: none;
        }
        
        .reply-avatar {
          flex-shrink: 0;
          border: 1px solid rgba(0, 195, 255, 0.2);
        }
        
        .reply-content {
          flex: 1;
          
          .reply-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 6px;
            
            .reply-author {
              font-weight: 600;
              color: var(--text-dark);
              font-size: 13px;
            }
            
            .reply-time {
              font-size: 11px;
              color: var(--text-gray);
            }
          }
          
          .reply-text {
            color: var(--text-dark);
            font-size: 13px;
            line-height: 1.6;
            word-break: break-word;
          }
        }
      }
    }
    
    .reply-input-container {
      margin-top: 12px;
      padding: 12px;
      background: rgba(0, 195, 255, 0.05);
      border-radius: 8px;
      border: 1px solid rgba(0, 195, 255, 0.2);
      
      :deep(.el-textarea) {
        .el-textarea__inner {
          background: rgba(255, 255, 255, 0.95);
          border-color: rgba(0, 195, 255, 0.2);
          color: var(--text-dark);
          
          &:focus {
            border-color: var(--primary-color);
            background: rgba(255, 255, 255, 1);
          }
        }
        
        .el-input__count {
          background: transparent;
          color: var(--text-gray);
        }
      }
      
      .reply-input-actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        margin-top: 8px;
        
        :deep(.el-button) {
          border-radius: 6px;
          transition: all 0.3s;
        }
        
        :deep(.el-button--default) {
          background: rgba(255, 255, 255, 0.9);
          border-color: rgba(0, 0, 0, 0.1);
          color: var(--text-dark);
          
          &:hover {
            background: rgba(255, 255, 255, 1);
            border-color: rgba(0, 0, 0, 0.2);
          }
        }
      }
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    border-bottom-color: rgba(255, 255, 255, 0.1);
    
    &:hover {
      background: rgba(255, 255, 255, 0.05);
    }
    
    .comment-content {
      .comment-header {
        .comment-author {
          color: rgba(255, 255, 255, 0.9);
          
          &:hover {
            color: var(--primary-color);
          }
        }
        
        .comment-time {
          color: rgba(255, 255, 255, 0.5);
        }
      }
      
      .comment-text {
        color: rgba(255, 255, 255, 0.8);
      }
      
      .comment-actions {
        :deep(.el-button) {
          color: rgba(255, 255, 255, 0.6);
          
          &:hover:not(:disabled) {
            color: var(--primary-color);
            background: rgba(0, 195, 255, 0.15);
          }
        }
      }
      
      .replies-container {
        border-left-color: rgba(255, 255, 255, 0.2);
        
        .reply-item {
          border-bottom-color: rgba(255, 255, 255, 0.1);
          
          .reply-content {
            .reply-author {
              color: rgba(255, 255, 255, 0.9);
            }
            
            .reply-time {
              color: rgba(255, 255, 255, 0.5);
            }
            
            .reply-text {
              color: rgba(255, 255, 255, 0.8);
            }
          }
        }
      }
      
      .reply-input-container {
        background: rgba(30, 41, 59, 0.6);
        border-color: rgba(255, 255, 255, 0.2);
        
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
          
          .el-input__count {
            background: transparent !important;
            color: rgba(255, 255, 255, 0.5) !important;
          }
        }
        
        .reply-input-actions {
          :deep(.el-button--default) {
            background: rgba(30, 41, 59, 0.8) !important;
            border-color: rgba(255, 255, 255, 0.2) !important;
            color: rgba(255, 255, 255, 0.8) !important;
            
            &:hover {
              background: rgba(30, 41, 59, 0.95) !important;
              border-color: rgba(255, 255, 255, 0.3) !important;
              color: rgba(255, 255, 255, 0.9) !important;
            }
          }
        }
      }
    }
  }
}
</style>
