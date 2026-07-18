<template>
  <div class="feedback-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <el-icon class="header-icon"><ChatDotRound /></el-icon>
        <h1 class="page-title">对系统进行意见反馈</h1>
      </div>
    </div>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" class="feedback-tabs">
      <el-tab-pane label="歌曲互动" name="feedback">
        <div class="tab-content">
          <!-- 反馈列表 -->
          <div v-if="feedbackList.length > 0" class="feedback-list">
            <div
              v-for="item in feedbackList"
              :key="item.id"
              class="feedback-item"
            >
              <div class="feedback-header">
                <div class="feedback-type">
                  <el-tag :type="getFeedbackTypeTag(item.type)">
                    {{ getFeedbackTypeText(item.type) }}
                  </el-tag>
                </div>
                <div class="feedback-time">
                  {{ formatTime(item.createTime) }}
                </div>
              </div>
              <div v-if="item.content" class="feedback-content">
                {{ item.content }}
              </div>
              <div v-if="item.musicId" class="feedback-music">
                <el-link type="primary" @click="goToMusicDetail(item.musicId)">
                  查看相关音乐
                </el-link>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无反馈记录" />
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="我的建议" name="suggestion">
        <div class="tab-content">
          <!-- 提交建议表单 -->
          <div class="suggestion-form-card">
            <h3 class="form-title">提交系统建议</h3>
            <el-form :model="suggestionForm" :rules="suggestionRules" ref="suggestionFormRef">
              <el-form-item prop="content">
                <el-input
                  v-model="suggestionForm.content"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入您的建议或意见，我们会认真考虑您的反馈..."
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item>
                <el-button 
                  type="primary" 
                  :loading="submitting"
                  @click="handleSubmitSuggestion"
                >
                  提交建议
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 建议列表 -->
          <div v-if="suggestionList.length > 0" class="suggestion-list">
            <h3 class="list-title">我的建议记录</h3>
            <div
              v-for="item in suggestionList"
              :key="item.id"
              class="suggestion-item"
            >
              <div class="suggestion-header">
                <div class="suggestion-time">
                  {{ formatTime(item.createTime) }}
                </div>
              </div>
              <div class="suggestion-content">
                {{ item.content }}
              </div>
            </div>
          </div>
          <div v-else-if="!loading" class="empty-suggestions">
            <el-empty description="暂无建议记录，快来提交第一条建议吧！" />
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="通知" name="notification">
        <div class="tab-content">
          <el-empty description="通知功能开发中..." />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getFeedbackList, submitFeedback } from '@/api/feedback'
import type { Feedback } from '@/api/feedback'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const activeTab = ref('feedback')
const feedbackList = ref<Feedback[]>([])
const suggestionList = ref<Feedback[]>([])
const loading = ref(false)
const submitting = ref(false)
const suggestionFormRef = ref<FormInstance>()

const suggestionForm = ref({
  content: ''
})

const suggestionRules: FormRules = {
  content: [
    { required: true, message: '请输入建议内容', trigger: 'blur' },
    { min: 10, message: '建议内容至少10个字符', trigger: 'blur' },
    { max: 500, message: '建议内容不能超过500个字符', trigger: 'blur' }
  ]
}

// 监听Tab切换，加载对应的数据
watch(activeTab, (newTab) => {
  if (newTab === 'feedback') {
    loadFeedbackList()
  } else if (newTab === 'suggestion') {
    loadSuggestionList()
  }
})

onMounted(() => {
  loadFeedbackList()
})

const loadFeedbackList = async () => {
  loading.value = true
  try {
    const result = await getFeedbackList({
      type: 'LIKE,DISLIKE,COMMENT', // 排除建议类型
      pageNum: 1,
      pageSize: 50
    })
    feedbackList.value = result.list || []
  } catch (error: any) {
    console.error('加载反馈列表失败', error)
    ElMessage.error('加载反馈列表失败')
  } finally {
    loading.value = false
  }
}

const loadSuggestionList = async () => {
  loading.value = true
  try {
    const result = await getFeedbackList({
      type: 'SUGGESTION',
      pageNum: 1,
      pageSize: 50
    })
    suggestionList.value = result.list || []
  } catch (error: any) {
    console.error('加载建议列表失败', error)
    ElMessage.error('加载建议列表失败')
  } finally {
    loading.value = false
  }
}

const handleSubmitSuggestion = async () => {
  if (!suggestionFormRef.value) return
  
  await suggestionFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      await submitFeedback({
        type: 'SUGGESTION',
        content: suggestionForm.value.content.trim()
      })
      ElMessage.success('建议提交成功，感谢您的反馈！')
      suggestionForm.value.content = ''
      suggestionFormRef.value.resetFields()
      // 重新加载建议列表
      await loadSuggestionList()
    } catch (error: any) {
      console.error('提交建议失败', error)
      ElMessage.error(error?.response?.data?.message || '提交建议失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

const getFeedbackTypeText = (type: string) => {
  const map: Record<string, string> = {
    LIKE: '点赞',
    DISLIKE: '差评',
    COMMENT: '评论',
    SUGGESTION: '建议'
  }
  return map[type] || type
}

const getFeedbackTypeTag = (type: string) => {
  const map: Record<string, string> = {
    LIKE: 'success',
    DISLIKE: 'danger',
    COMMENT: 'primary',
    SUGGESTION: 'warning'
  }
  return map[type] || 'info'
}

const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const goToMusicDetail = (musicId?: number) => {
  if (musicId) {
    router.push(`/music/${musicId}`)
  }
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.feedback-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 32px;
  
  .header-content {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .header-icon {
      font-size: 32px;
      color: var(--primary-color);
    }
    
    .page-title {
      font-size: 28px;
      font-weight: 600;
      color: var(--text-dark);
      margin: 0;
    }
  }
}

.feedback-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }
  
  :deep(.el-tabs__item) {
    font-size: 16px;
    font-weight: 500;
    color: var(--text-dark);
    
    &:not(.is-active) {
      color: var(--text-dark);
    }
  }
  
  // 夜间模式：未选择的Tab标签为白色
  [data-theme="dark"] & {
    :deep(.el-tabs__item) {
      color: rgba(255, 255, 255, 0.8) !important;
      
      &:not(.is-active) {
        color: rgba(255, 255, 255, 0.8) !important;
      }
      
      &.is-active {
        color: var(--primary-color) !important;
      }
      
      &:hover {
        color: var(--primary-color) !important;
      }
    }
    
    :deep(.el-tabs__active-bar) {
      background-color: var(--primary-color) !important;
    }
  }
}

.tab-content {
  min-height: 400px;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-item {
  padding: 20px;
  background: var(--bg-white);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
  
  .feedback-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    
    .feedback-type {
      flex: 1;
    }
    
    .feedback-time {
      font-size: 14px;
      color: var(--text-gray);
    }
  }
  
  .feedback-content {
    margin: 12px 0;
    padding: 12px;
    background: var(--bg-gray);
    border-radius: var(--radius-sm);
    color: var(--text-dark);
    line-height: 1.6;
  }
  
  .feedback-music {
    margin-top: 12px;
  }
}

// 建议相关样式
.suggestion-form-card {
  padding: 24px;
  background: var(--bg-white);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
  
  .form-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-dark);
    margin: 0 0 20px 0;
  }
  
  [data-theme="dark"] & {
    background: var(--bg-dark-secondary);
    
    .form-title {
      color: var(--text-light);
    }
  }
}

.suggestion-list {
  .list-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-dark);
    margin: 0 0 16px 0;
    
    [data-theme="dark"] & {
      color: var(--text-light);
    }
  }
}

.suggestion-item {
  padding: 20px;
  background: var(--bg-white);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
  
  .suggestion-header {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;
    
    .suggestion-time {
      font-size: 14px;
      color: var(--text-gray);
    }
  }
  
  .suggestion-content {
    padding: 12px;
    background: var(--bg-gray);
    border-radius: var(--radius-sm);
    color: var(--text-dark);
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }
  
  [data-theme="dark"] & {
    background: var(--bg-dark-secondary);
    
    .suggestion-content {
      background: rgba(255, 255, 255, 0.05);
      color: var(--text-light);
    }
  }
}

.empty-suggestions {
  margin-top: 40px;
}
</style>

