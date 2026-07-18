<template>
  <div class="personal-tag-editor">
    <div class="tag-editor-header">
      <div class="section-title">
        <div class="icon-wrapper">
          <el-icon><PriceTag /></el-icon>
        </div>
        <span>个性化标签</span>
        <span class="tag-count" v-if="tags.length > 0">({{ tags.length }}/10)</span>
      </div>
      <el-button
        v-if="tags.length < 10"
        type="primary"
        size="small"
        :icon="Plus"
        class="add-tag-btn"
        @click="showTagSelector = true"
      >
        添加
      </el-button>
    </div>
    
    <!-- 标签列表 -->
    <div v-if="tags.length > 0" class="tag-list">
      <div
        v-for="tag in tags"
        :key="tag.id"
        class="tag-item"
      >
        <el-tag
          :style="{
            backgroundColor: tag.color + '15',
            borderColor: tag.color + '40',
            color: tag.color
          }"
          class="custom-tag"
          closable
          @close="handleDeleteTag(tag.id)"
        >
          {{ tag.name }}
        </el-tag>
      </div>
    </div>
    
    <div v-else class="empty-tags" @click="showTagSelector = true">
      <div class="empty-icon">
        <el-icon><PriceTag /></el-icon>
      </div>
      <p class="empty-text">添加标签，展现独特的你</p>
      <el-button link type="primary">点击添加</el-button>
    </div>
    
    <!-- 标签选择器 -->
    <el-dialog
      v-model="showTagSelector"
      title="选择你的个性标签"
      width="520px"
      class="tag-selector-dialog"
      append-to-body
      destroy-on-close
    >
      <div class="preset-categories">
        <div v-for="cat in presetTags" :key="cat.category" class="category-group">
          <h4 class="category-title">{{ cat.category }}</h4>
          <div class="tag-cloud">
            <div 
              v-for="tag in cat.list" 
              :key="tag"
              class="preset-tag"
              :class="{ active: isSelected(tag) }"
              @click="toggleTag(tag)"
            >
              {{ tag }}
              <el-icon v-if="isSelected(tag)" class="check-icon"><Check /></el-icon>
            </div>
          </div>
        </div>
        
        <!-- 自定义标签输入 -->
        <div class="custom-tag-section">
          <div class="section-divider">
            <span>或自定义标签</span>
          </div>
          <div class="custom-input-wrapper">
            <el-input
              v-model="customTagName"
              placeholder="输入标签名称 (最多10字)"
              maxlength="10"
              show-word-limit
              @keyup.enter="addCustomTag"
            >
              <template #append>
                <el-button @click="addCustomTag" :disabled="!customTagName.trim()">添加</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <div class="selected-info">
            <span class="label">已选：</span>
            <span class="count" :class="{ full: selectedTagNames.length >= 10 }">
              {{ selectedTagNames.length }}/10
            </span>
          </div>
          <div class="footer-btns">
            <el-button @click="showTagSelector = false">取消</el-button>
            <el-button type="primary" @click="saveSelection" :loading="saving">保存修改</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { PriceTag, Plus, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserPersonalTags,
  createPersonalTag,
  deletePersonalTag
} from '@/api/userTag'
import type { UserPersonalTag } from '@/api/userTag'

const tags = ref<UserPersonalTag[]>([])
const showTagSelector = ref(false)
const saving = ref(false)
const customTagName = ref('')
const selectedTagNames = ref<string[]>([])

// 预设标签库
const presetTags = [
  { category: '听歌偏好', list: ['单曲循环', '随机播放', '歌词控', '前奏控', '抖腿', '睡眠BGM'] },
  { category: '音乐风格', list: ['流行', '摇滚', '民谣', '电子', '古典', '爵士', '嘻哈', 'R&B', '古风'] },
  { category: '心情状态', list: ['快乐', 'emo', '治愈', '热血', '平静', '孤独', '恋爱ing'] },
  { category: '身份标签', list: ['夜猫子', '考研党', '工作狂', '发烧友', '乐器达人', 'K歌之王'] }
]

const loadTags = async () => {
  try {
    tags.value = await getUserPersonalTags()
    selectedTagNames.value = tags.value.map(t => t.name)
  } catch (error: any) {
    ElMessage.error(error.message || '加载标签失败')
  }
}

const handleDeleteTag = async (tagId: number) => {
  try {
    await deletePersonalTag(tagId)
    // 更新本地数据
    tags.value = tags.value.filter(t => t.id !== tagId)
    selectedTagNames.value = tags.value.map(t => t.name)
    ElMessage.success('删除成功')
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const isSelected = (tagName: string) => selectedTagNames.value.includes(tagName)

const toggleTag = (tagName: string) => {
  if (isSelected(tagName)) {
    selectedTagNames.value = selectedTagNames.value.filter(t => t !== tagName)
  } else {
    if (selectedTagNames.value.length >= 10) {
      ElMessage.warning('最多只能添加10个标签')
      return
    }
    selectedTagNames.value.push(tagName)
  }
}

const addCustomTag = () => {
  const name = customTagName.value.trim()
  if (!name) return
  
  if (selectedTagNames.value.includes(name)) {
    ElMessage.warning('标签已存在')
    return
  }
  
  if (selectedTagNames.value.length >= 10) {
    ElMessage.warning('最多只能添加10个标签')
    return
  }
  
  selectedTagNames.value.push(name)
  customTagName.value = ''
}

const saveSelection = async () => {
  saving.value = true
  try {
    // 找出需要删除的（原tags中有，但selected中没有）
    const toDelete = tags.value.filter(t => !selectedTagNames.value.includes(t.name))
    // 找出需要新增的（selected中有，但原tags中没有）
    const toAdd = selectedTagNames.value.filter(name => !tags.value.some(t => t.name === name))
    
    // 执行删除
    for (const tag of toDelete) {
      await deletePersonalTag(tag.id)
    }
    
    // 执行新增
    // 为不同类型的标签分配不同颜色
    const getColor = (name: string) => {
      // 简单的哈希颜色生成
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8e44ad', '#ec4899', '#1abc9c']
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    }
    
    for (const name of toAdd) {
      await createPersonalTag(name, getColor(name))
    }
    
    await loadTags()
    showTagSelector.value = false
    ElMessage.success('标签更新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 当打开选择器时，重置选中状态
watch(showTagSelector, (val) => {
  if (val) {
    selectedTagNames.value = tags.value.map(t => t.name)
    customTagName.value = ''
  }
})

onMounted(() => {
  loadTags()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.personal-tag-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .tag-editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    .section-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 700;
      color: var(--text-dark);
      
      .icon-wrapper {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        background: rgba(236, 72, 153, 0.1);
        @include flex-center;
        color: #ec4899;
      }
      
      .tag-count {
        font-size: 13px;
        color: var(--text-gray);
        font-weight: 400;
      }
    }
    
    .add-tag-btn {
      border-radius: 20px;
      padding: 8px 16px;
    }
  }
  
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-content: flex-start;
    
    .tag-item {
      .custom-tag {
        border-radius: 16px;
        padding: 6px 14px;
        font-size: 13px;
        border-width: 1px;
        height: 32px;
        transition: all 0.2s;
        
        :deep(.el-tag__close) {
          color: currentColor;
          opacity: 0.6;
          &:hover {
            opacity: 1;
            background: transparent;
          }
        }
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        }
      }
    }
  }
  
  .empty-tags {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 160px;
    background: rgba(0, 0, 0, 0.02);
    border-radius: 12px;
    border: 1px dashed rgba(0, 0, 0, 0.1);
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: rgba(0, 0, 0, 0.04);
      border-color: var(--primary-color);
      
      .empty-icon {
        transform: scale(1.1);
        color: var(--primary-color);
      }
    }
    
    .empty-icon {
      font-size: 32px;
      color: var(--text-light-gray);
      margin-bottom: 12px;
      transition: all 0.3s;
    }
    
    .empty-text {
      color: var(--text-gray);
      font-size: 14px;
      margin-bottom: 8px;
    }
  }
}

[data-theme="dark"] {
  .personal-tag-editor {
    .tag-editor-header {
      .section-title {
        color: var(--text-light);
        
        .icon-wrapper {
          background: rgba(236, 72, 153, 0.2);
        }
      }
    }
    
    .empty-tags {
      background: rgba(255, 255, 255, 0.02);
      border-color: rgba(255, 255, 255, 0.1);
      
      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }
    }
  }
}

// 标签选择器样式
.tag-selector-dialog {
  .preset-categories {
    max-height: 400px;
    overflow-y: auto;
    padding-right: 4px;
    
    .category-group {
      margin-bottom: 24px;
      
      .category-title {
        font-size: 15px;
        font-weight: 600;
        margin-bottom: 12px;
        color: var(--text-dark);
        display: flex;
        align-items: center;
        
        &::before {
          content: '';
          display: block;
          width: 4px;
          height: 14px;
          background: var(--primary-color);
          border-radius: 2px;
          margin-right: 8px;
        }
        
        [data-theme="dark"] & {
          color: var(--text-light);
        }
      }
      
      .tag-cloud {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        
        .preset-tag {
          padding: 6px 14px;
          border-radius: 20px;
          background: var(--bg-light-secondary);
          color: var(--text-gray);
          cursor: pointer;
          font-size: 13px;
          transition: all 0.2s;
          display: flex;
          align-items: center;
          gap: 6px;
          border: 1px solid transparent;
          
          [data-theme="dark"] & {
            background: rgba(255, 255, 255, 0.08);
            color: var(--text-light-secondary);
          }
          
          &:hover {
            color: var(--primary-color);
            background: rgba(var(--primary-color-rgb), 0.1);
          }
          
          &.active {
            background: var(--primary-gradient);
            color: white;
            box-shadow: 0 4px 10px rgba(var(--primary-color-rgb), 0.3);
            border-color: transparent;
            
            .check-icon {
              font-size: 12px;
            }
          }
        }
      }
    }
    
    .custom-tag-section {
      margin-top: 24px;
      
      .section-divider {
        display: flex;
        align-items: center;
        margin-bottom: 16px;
        color: var(--text-light-gray);
        font-size: 12px;
        
        &::before, &::after {
          content: '';
          flex: 1;
          height: 1px;
          background: var(--border-color);
        }
        
        span {
          padding: 0 12px;
        }
      }
      
      .custom-input-wrapper {
        :deep(.el-input__wrapper) {
          border-radius: 20px 0 0 20px;
        }
        :deep(.el-input-group__append) {
          border-radius: 0 20px 20px 0;
          background: var(--primary-color);
          color: white;
          border-color: var(--primary-color);
          
          .el-button {
            color: white;
            &:hover {
              background: transparent;
            }
          }
        }
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    
    .selected-info {
      font-size: 14px;
      color: var(--text-gray);
      
      .count {
        font-weight: 600;
        color: var(--primary-color);
        
        &.full {
          color: var(--danger-color);
        }
      }
    }
  }
}
</style>
