<template>
  <div class="galaxy-container">
    <!-- 背景星空层 -->
    <div class="stars-bg"></div>
    <div class="stars-bg-2"></div>
    <div class="nebula-bg"></div>

    <!-- 中心恒星：头像 -->
    <div class="sun-avatar" @click="$emit('enterEdit')">
      <div class="avatar-wrapper">
        <img :src="userStore.userInfo?.avatar || defaultAvatar" class="avatar-img" />
        <div class="avatar-glow"></div>
        <div class="avatar-ring"></div>
      </div>
      <div class="edit-hint">
        <el-icon><Edit /></el-icon>
        <span>编辑资料</span>
      </div>
    </div>

    <!-- 环绕行星：标签 -->
    <div class="orbits-container">
      <!-- 轨道线 -->
      <div class="orbit orbit-1"></div>
      <div class="orbit orbit-2"></div>
      <div class="orbit orbit-3"></div>
      
      <!-- 标签行星 -->
      <div 
        v-for="(tag, index) in tags" 
        :key="tag.id" 
        class="planet-tag"
        :style="getPlanetStyle(index, tags.length)"
      >
        <div class="planet-body" :style="{ '--tag-color': tag.color }">
          <div class="planet-core"></div>
          <div class="planet-label">
            {{ tag.name }}
          </div>
        </div>
      </div>
      
      <!-- 添加按钮作为特殊的行星 -->
      <div 
        class="planet-tag add-planet-wrapper"
        :style="getPlanetStyle(tags.length, tags.length + 1)"
      >
        <div class="add-planet" @click="showTagSelector = true">
          <div class="planet-core add-core">
            <el-icon><Plus /></el-icon>
          </div>
          <span class="planet-label">添加</span>
        </div>
      </div>
    </div>
    
    <!-- 标签选择器 (保持与 PersonalTagEditor.vue 一致的最新样式) -->
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
            <span class="count" :class="{ full: selectedTags.length >= 10 }">
              {{ selectedTags.length }}/10
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
import { ref, onMounted, computed, watch } from 'vue'
import { useUserStore } from '@/store/user'
import { Edit, Plus, Check } from '@element-plus/icons-vue'
import { getUserPersonalTags, createPersonalTag, deletePersonalTag } from '@/api/userTag'
import type { UserPersonalTag } from '@/api/userTag'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['enterEdit'])
const userStore = useUserStore()
const defaultAvatar = 'https://via.placeholder.com/150'

const tags = ref<UserPersonalTag[]>([])
const showTagSelector = ref(false)
const saving = ref(false)
const customTagName = ref('')

// 预设标签库
const presetTags = [
  { category: '听歌偏好', list: ['单曲循环', '随机播放', '歌词控', '前奏控', '抖腿', '睡眠BGM'] },
  { category: '音乐风格', list: ['流行', '摇滚', '民谣', '电子', '古典', '爵士', '嘻哈', 'R&B', '古风'] },
  { category: '心情状态', list: ['快乐', 'emo', '治愈', '热血', '平静', '孤独', '恋爱ing'] },
  { category: '身份标签', list: ['夜猫子', '考研党', '工作狂', '发烧友', '乐器达人', 'K歌之王'] }
]

// 临时选中的标签名称列表
const selectedTagNames = ref<string[]>([])

const loadTags = async () => {
  try {
    tags.value = await getUserPersonalTags()
    selectedTagNames.value = tags.value.map(t => t.name)
  } catch (error) {
    console.error('加载标签失败', error)
  }
}

// 计算行星位置
const getPlanetStyle = (index: number, total: number) => {
  // 使用三个轨道分布
  const orbitIndex = index % 3
  let orbitRadius = 180
  let duration = 25
  
  if (orbitIndex === 0) { // 内圈
    orbitRadius = 160
    duration = 20
  } else if (orbitIndex === 1) { // 中圈
    orbitRadius = 260
    duration = 30
  } else { // 外圈
    orbitRadius = 360
    duration = 40
  }
  
  const angle = (360 / total) * index
  const delay = -(duration / total) * index 
  
  return {
    '--orbit-radius': `${orbitRadius}px`,
    '--start-angle': `${angle}deg`,
    '--duration': `${duration}s`,
    '--delay': `${delay}s`
  } as any
}

// 标签选择逻辑
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
    ElMessage.success('星系更新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const selectedTags = computed(() => selectedTagNames.value)

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

<style lang="scss" scoped>
@import '@/assets/styles/mixins.scss';

// 生成随机星星的函数
@function generate-stars($n) {
  $value: '#{random(2000)}px #{random(2000)}px #FFF';
  @for $i from 2 through $n {
    $value: '#{$value} , #{random(2000)}px #{random(2000)}px #FFF';
  }
  @return unquote($value);
}

.galaxy-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 100px);
  min-height: 600px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  
  // 背景：白天模式
  background: radial-gradient(circle at center, #e0f2fe 0%, #bae6fd 40%, #7dd3fc 100%);
  
  // 背景：黑夜模式 - 深邃星空
  [data-theme="dark"] & {
    background: radial-gradient(circle at bottom center, #1b2735 0%, #090a0f 100%);
  }
}

// 星星背景层
.stars-bg {
  width: 1px;
  height: 1px;
  background: transparent;
  box-shadow: generate-stars(100);
  animation: animStar 50s linear infinite;
  opacity: 0; // 白天默认隐藏
  
  &::after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 1px;
    height: 1px;
    background: transparent;
    box-shadow: generate-stars(100);
  }
  
  [data-theme="dark"] & {
    opacity: 0.8;
  }
}

.stars-bg-2 {
  width: 2px;
  height: 2px;
  background: transparent;
  box-shadow: generate-stars(50);
  animation: animStar 100s linear infinite;
  opacity: 0;
  
  &::after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 2px;
    height: 2px;
    background: transparent;
    box-shadow: generate-stars(50);
  }
  
  [data-theme="dark"] & {
    opacity: 0.6;
  }
}

@keyframes animStar {
  from { transform: translateY(0px); }
  to { transform: translateY(-2000px); }
}

.sun-avatar {
  position: relative;
  z-index: 10;
  cursor: pointer;
  
  .avatar-wrapper {
    position: relative;
    width: 140px;
    height: 140px;
    border-radius: 50%;
    padding: 6px;
    background: rgba(255, 255, 255, 0.8);
    box-shadow: 0 0 40px rgba(64, 158, 255, 0.2);
    transition: transform 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    
    [data-theme="dark"] & {
      background: rgba(255, 255, 255, 0.05);
      box-shadow: 0 0 60px rgba(64, 158, 255, 0.4);
    }
    
    .avatar-img {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      object-fit: cover;
      position: relative;
      z-index: 2;
    }
    
    .avatar-glow {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(64, 158, 255, 0.4) 0%, transparent 70%);
      animation: pulse-glow 3s infinite;
      z-index: 1;
    }
    
    .avatar-ring {
      position: absolute;
      top: -10px;
      left: -10px;
      right: -10px;
      bottom: -10px;
      border: 2px solid rgba(64, 158, 255, 0.3);
      border-radius: 50%;
      border-top-color: transparent;
      border-bottom-color: transparent;
      animation: spin 10s linear infinite;
    }
  }
  
  .edit-hint {
    position: absolute;
    bottom: -50px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 6px;
    background: rgba(255, 255, 255, 0.9);
    padding: 8px 18px;
    border-radius: 24px;
    font-size: 14px;
    color: var(--primary-color);
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    opacity: 0;
    transition: all 0.3s;
    white-space: nowrap;
    
    [data-theme="dark"] & {
      background: rgba(15, 23, 42, 0.8);
      color: var(--primary-color);
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }
  }
  
  &:hover {
    .avatar-wrapper {
      transform: scale(1.1);
    }
    .edit-hint {
      opacity: 1;
      transform: translateX(-50%) translateY(-10px);
    }
  }
}

.orbits-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  pointer-events: none;
  
  .orbit {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 50%;
    border: 1px solid rgba(0, 0, 0, 0.05);
    transition: all 0.5s;
    
    [data-theme="dark"] & {
      border-color: rgba(255, 255, 255, 0.05);
      box-shadow: 0 0 10px rgba(255, 255, 255, 0.02);
    }
    
    &.orbit-1 { width: 320px; height: 320px; }
    &.orbit-2 { width: 520px; height: 520px; }
    &.orbit-3 { width: 720px; height: 720px; }
  }
}

.planet-tag, .add-planet-wrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  pointer-events: auto;
  animation: orbit-rotate var(--duration) linear infinite;
  animation-delay: var(--delay);
  
  .planet-body, .add-planet {
    position: absolute;
    transform: translate(-50%, -50%);
    margin-left: var(--orbit-radius);
    animation: counter-rotate var(--duration) linear infinite;
    animation-delay: var(--delay);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    transition: all 0.3s;
    
    .planet-core {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--tag-color);
      box-shadow: 0 0 15px var(--tag-color);
      transition: all 0.3s;
      
      [data-theme="dark"] & {
        box-shadow: 0 0 20px var(--tag-color), inset 0 0 6px rgba(255, 255, 255, 0.5);
      }
      
      &.add-core {
        background: var(--primary-color);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-size: 14px;
      }
    }
    
    .planet-label {
      background: rgba(255, 255, 255, 0.9);
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-dark);
      white-space: nowrap;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: all 0.3s;
      
      [data-theme="dark"] & {
        background: rgba(0, 0, 0, 0.6);
        color: var(--text-light);
        border: 1px solid rgba(255, 255, 255, 0.1);
      }
    }
    
    &:hover {
      z-index: 100;
      
      .planet-core {
        transform: scale(1.5);
        box-shadow: 0 0 30px var(--tag-color);
      }
      
      .planet-label {
        transform: translateY(4px);
        background: white;
        color: var(--primary-color);
        
        [data-theme="dark"] & {
          background: rgba(255, 255, 255, 0.1);
          color: white;
        }
      }
    }
  }
}

@keyframes orbit-rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes counter-rotate {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(-360deg); }
}

@keyframes pulse-glow {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 0.5; }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.2; }
  100% { transform: translate(-50%, -50%) scale(1); opacity: 0.5; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// 复用 PersonalTagEditor 的样式并优化
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
