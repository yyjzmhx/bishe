<template>
  <div class="analysis-page">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
    
    <div v-else-if="analysisData" class="analysis-content">
      <!-- 分析报告 -->
      <div class="analysis-report fade-in">
        <!-- 报告头部 -->
        <div class="report-header">
          <div class="header-content">
            <div class="header-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="header-text">
              <h2 class="report-title">AI 智能分析报告</h2>
              <p class="report-subtitle">基于深度学习的音频特征分析</p>
            </div>
          </div>
          <el-button
            :icon="isExpanded ? ArrowUp : ArrowDown"
            circle
            class="toggle-btn"
            @click="isExpanded = !isExpanded"
          />
        </div>
        
        <el-collapse-transition>
          <div v-show="isExpanded" class="report-content">
            <!-- 推荐理由卡片 -->
            <div v-if="analysisData.analysisResult" class="reason-card">
              <div class="card-icon reason-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">推荐理由</h3>
                <p class="reason-text">{{ analysisData.analysisResult }}</p>
              </div>
            </div>
            
            <!-- 音频特征分析 -->
            <div v-if="parsedFeatures" class="features-section">
              <div class="section-header">
                <div class="section-icon">
                  <el-icon><DataAnalysis /></el-icon>
                </div>
                <h3 class="section-title">音频特征分析</h3>
              </div>
              
              <div class="features-grid">
                <div v-if="parsedFeatures.style" class="feature-card style-card">
                  <div class="feature-icon-wrapper">
                    <el-icon class="feature-icon"><Headset /></el-icon>
                  </div>
                  <div class="feature-content">
                    <div class="feature-label">音乐风格</div>
                    <div class="feature-value">{{ parsedFeatures.style }}</div>
                  </div>
                </div>
                
                <div v-if="parsedFeatures.emotion" class="feature-card emotion-card">
                  <div class="feature-icon-wrapper">
                    <el-icon class="feature-icon"><Sunny /></el-icon>
                  </div>
                  <div class="feature-content">
                    <div class="feature-label">情感色彩</div>
                    <div class="feature-value">{{ parsedFeatures.emotion }}</div>
                  </div>
                </div>
                
                <div v-if="parsedFeatures.rhythm" class="feature-card rhythm-card">
                  <div class="feature-icon-wrapper">
                    <el-icon class="feature-icon"><Timer /></el-icon>
                  </div>
                  <div class="feature-content">
                    <div class="feature-label">节奏特点</div>
                    <div class="feature-value">{{ parsedFeatures.rhythm }}</div>
                  </div>
                </div>
                
                <div v-if="parsedFeatures.atmosphere" class="feature-card atmosphere-card">
                  <div class="feature-icon-wrapper">
                    <el-icon class="feature-icon"><Moon /></el-icon>
                  </div>
                  <div class="feature-content">
                    <div class="feature-label">整体氛围</div>
                    <div class="feature-value">{{ parsedFeatures.atmosphere }}</div>
                  </div>
                </div>
              </div>
              
              <!-- 乐器类型 -->
              <div v-if="parsedFeatures.instruments && parsedFeatures.instruments.length > 0" class="instruments-card">
                <div class="card-icon instruments-icon">
                  <el-icon><Microphone /></el-icon>
                </div>
                <div class="card-content">
                  <h3 class="card-title">乐器类型</h3>
                  <div class="instruments-list">
                    <div
                      v-for="(instrument, idx) in parsedFeatures.instruments"
                      :key="idx"
                      class="instrument-badge"
                    >
                      {{ instrument }}
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 整体描述 -->
              <div v-if="parsedFeatures.description" class="description-card">
                <div class="card-icon description-icon">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="card-content">
                  <h3 class="card-title">整体描述</h3>
                  <p class="description-text">{{ parsedFeatures.description }}</p>
                </div>
              </div>
            </div>
            
            <!-- 特征向量展示 -->
            <div v-if="features.length > 0" class="vector-section">
              <div class="section-header">
                <div class="section-icon">
                  <el-icon><TrendCharts /></el-icon>
                </div>
                <h3 class="section-title">特征向量</h3>
              </div>
              
              <div class="vector-grid">
                <div
                  v-for="(value, index) in features"
                  :key="index"
                  class="vector-card"
                >
                  <div class="vector-header">
                    <span class="vector-label">{{ featureLabels[index] || `维度${index + 1}` }}</span>
                    <span class="vector-percentage">{{ Math.round(value * 100) }}%</span>
                  </div>
                  <div class="vector-progress-wrapper">
                    <div
                      class="vector-progress"
                      :style="{
                        width: `${value * 100}%`,
                        background: getProgressGradient(value)
                      }"
                    />
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 可视化图表 -->
            <div v-if="features.length > 0" class="visualization-section">
              <div class="section-header">
                <div class="section-icon">
                  <el-icon><TrendCharts /></el-icon>
                </div>
                <h3 class="section-title">可视化分析</h3>
              </div>
              
              <div class="visualization-tabs">
                <el-radio-group v-model="visualizationType" size="large">
                  <el-radio-button label="2d">
                    <el-icon><TrendCharts /></el-icon>
                    <span>2D雷达图</span>
                  </el-radio-button>
                  <el-radio-button label="3d">
                    <el-icon><DataAnalysis /></el-icon>
                    <span>3D特征球</span>
                  </el-radio-button>
                </el-radio-group>
              </div>
              
              <div v-if="visualizationType === '2d'" class="radar-chart-container">
                <FeatureRadar :features="features" />
              </div>
              
              <div v-else class="feature-3d-container">
                <AudioFeature3D 
                  :features="features" 
                  :labels="featureLabels"
                  height="500px"
                  :auto-rotate="true"
                  :show-points="true"
                  :show-lines="true"
                  :show-labels="true"
                />
              </div>
            </div>
          </div>
        </el-collapse-transition>
      </div>
      
      <!-- 推荐列表 -->
      <div class="recommendations-section">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><StarFilled /></el-icon>
            推荐音乐 ({{ recommendations.length }})
          </h3>
          <el-select v-model="sortBy" placeholder="排序方式" style="width: 150px">
            <el-option label="相似度" value="similarity" />
            <el-option label="播放量" value="playCount" />
            <el-option label="点赞数" value="likeCount" />
          </el-select>
        </div>
        
        <div class="recommendations-grid">
          <div
            v-for="(rec, index) in sortedRecommendations"
            :key="rec.id"
            class="recommendation-card fade-in"
            :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <div class="rank-badge">{{ rec.rank }}</div>
            <MusicCard
              v-if="rec.music"
              :music="rec.music"
              :show-similarity="true"
              :similarity="rec.similarity"
              :preview-mode="true"
            />
          </div>
        </div>
        
        <div v-if="recommendations.length === 0" class="empty-state">
          <el-empty description="暂无推荐结果" />
        </div>
      </div>
    </div>
    
    <div v-else class="error-state">
      <el-result
        icon="error"
        title="加载失败"
        sub-title="无法获取分析结果，请稍后重试"
      >
        <template #extra>
          <el-button type="primary" @click="loadAnalysis">重新加载</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, ArrowUp, ArrowDown, StarFilled, ChatDotRound, DataAnalysis, Headset, Sunny, Timer, Moon, Microphone, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { analyzeAudio, getRecommendations } from '@/api/recommend'
import { getUploadRecord } from '@/api/upload'
import FeatureRadar from '@/components/charts/FeatureRadar.vue'
import AudioFeature3D from '@/components/3d/AudioFeature3D.vue'
import MusicCard from '@/components/common/MusicCard.vue'
import { formatSimilarity } from '@/utils/format'
import { useMusicStore } from '@/store/music'
import type { RecommendRecord, UploadRecord } from '@/types/music'

const route = useRoute()
const router = useRouter()
const musicStore = useMusicStore()
const uploadId = Number(route.params.uploadId)

const loading = ref(true)
const isExpanded = ref(true)
const analysisData = ref<UploadRecord | null>(null)
const recommendations = ref<RecommendRecord[]>([])
const sortBy = ref('similarity')
const features = ref<number[]>([])
const parsedFeatures = ref<any>(null)
const visualizationType = ref<'2d' | '3d'>('2d')
const featureLabels = ref<string[]>(['能量强度', '节奏活跃度', '音色明亮度', '动态起伏', '纹理复杂度'])

// 轮询定时器
let pollTimer: NodeJS.Timeout | null = null
const POLL_INTERVAL = 2000 // 2秒轮询一次
const MAX_POLL_COUNT = 60 // 最多轮询60次（2分钟）

let pollCount = 0

const sortedRecommendations = computed(() => {
  const sorted = [...recommendations.value]
  
  if (sortBy.value === 'similarity') {
    return sorted.sort((a, b) => b.similarity - a.similarity)
  } else if (sortBy.value === 'playCount') {
    return sorted.sort((a, b) => (b.music?.playCount || 0) - (a.music?.playCount || 0))
  } else {
    return sorted.sort((a, b) => (b.music?.likeCount || 0) - (a.music?.likeCount || 0))
  }
})

const loadAnalysis = async () => {
  loading.value = true
  try {
    // 获取上传记录
    const uploadRecord = await getUploadRecord(uploadId)
    analysisData.value = uploadRecord
    
    // 解析特征
    parseFeatures(uploadRecord.features)
    
    // 如果状态是UPLOADING或ANALYZING，触发分析并开始轮询
    if (uploadRecord.status === 'UPLOADING' || uploadRecord.status === 'ANALYZING') {
      // 触发分析（如果还没有开始）
      if (uploadRecord.status === 'UPLOADING') {
        try {
          await analyzeAudio(uploadId)
        } catch (error: any) {
          ElMessage.error('启动分析失败：' + (error.message || '未知错误'))
          loading.value = false
          return
        }
      }
      
      // 开始轮询检查分析状态
      startPolling()
    } else if (uploadRecord.status === 'COMPLETED') {
      // 直接加载推荐
      await loadRecommendations()
      loading.value = false
    } else {
      // FAILED状态
      ElMessage.error('分析失败：' + (uploadRecord.errorMessage || '未知错误'))
      loading.value = false
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
    loading.value = false
  }
}

// 解析特征数据
const parseFeatures = (featuresStr?: string) => {
  features.value = []
  parsedFeatures.value = null
  featureLabels.value = ['能量强度', '节奏活跃度', '音色明亮度', '动态起伏', '纹理复杂度']
  if (!featuresStr) return
  
  try {
    const parsed = JSON.parse(featuresStr)
    if (Array.isArray(parsed)) {
      // 如果是数组，说明是特征向量
      features.value = parsed
    } else if (typeof parsed === 'object') {
      // 如果是对象，说明是详细的音频描述
      parsedFeatures.value = parsed
      // 尝试提取特征向量（如果有）
      if (parsed.vector && Array.isArray(parsed.vector)) {
        features.value = parsed.vector
      }
      if (parsed.vectorLabels && Array.isArray(parsed.vectorLabels) && parsed.vectorLabels.length > 0) {
        featureLabels.value = parsed.vectorLabels
      }
    }
  } catch (e) {
    console.error('解析特征失败', e)
    // 尝试直接解析为数组
    try {
      const arrayMatch = featuresStr.match(/\[[\d.,\s]+\]/)
      if (arrayMatch) {
        features.value = JSON.parse(arrayMatch[0])
      }
    } catch (e2) {
      console.error('解析特征向量失败', e2)
    }
  }
}

// 开始轮询检查分析状态
const startPolling = () => {
  pollCount = 0
  
  pollTimer = setInterval(async () => {
    pollCount++
    
    try {
      const uploadRecord = await getUploadRecord(uploadId)
      
      // 更新分析数据
      analysisData.value = uploadRecord
      
      // 如果分析完成
      if (uploadRecord.status === 'COMPLETED') {
        stopPolling()
        
        // 重新解析特征
        parseFeatures(uploadRecord.features)
        
        // 加载推荐
        await loadRecommendations()
        loading.value = false
        ElMessage.success('分析完成')
      } else if (uploadRecord.status === 'FAILED') {
        stopPolling()
        ElMessage.error('分析失败：' + (uploadRecord.errorMessage || '未知错误'))
        loading.value = false
      } else if (pollCount >= MAX_POLL_COUNT) {
        // 超时
        stopPolling()
        ElMessage.warning('分析超时，请稍后刷新页面查看结果')
        loading.value = false
      }
    } catch (error: any) {
      console.error('轮询检查失败', error)
      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        loading.value = false
      }
    }
  }, POLL_INTERVAL)
}

// 停止轮询
const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const loadRecommendations = async () => {
  try {
    const result = await getRecommendations(uploadId)
    recommendations.value = result.recommendations || []
  } catch (error: any) {
    console.error('加载推荐失败', error)
  }
}

// 根据特征值获取进度条颜色
const getProgressColor = (value: number) => {
  if (value >= 0.8) return '#67c23a'
  if (value >= 0.6) return '#e6a23c'
  if (value >= 0.4) return '#409eff'
  return '#909399'
}

// 根据特征值获取进度条渐变
const getProgressGradient = (value: number) => {
  if (value >= 0.8) {
    return 'linear-gradient(90deg, #10b981 0%, #059669 100%)'
  }
  if (value >= 0.6) {
    return 'linear-gradient(90deg, #f59e0b 0%, #d97706 100%)'
  }
  if (value >= 0.4) {
    return 'linear-gradient(90deg, #3b82f6 0%, #2563eb 100%)'
  }
  return 'linear-gradient(90deg, #6b7280 0%, #4b5563 100%)'
}

onMounted(() => {
  loadAnalysis()
})

// 监听路由变化，当离开分析页面时，如果播放列表有歌曲，确保显示播放条
watch(() => route.name, (newRouteName, oldRouteName) => {
  // 如果从分析页面离开
  if (oldRouteName === 'Analysis' && newRouteName !== 'Analysis') {
    // 如果播放列表有歌曲但没有当前播放的音乐，设置第一首为当前音乐
    if (musicStore.playlist.length > 0 && !musicStore.currentMusic) {
      musicStore.setCurrentMusicAndIndex(musicStore.playlist[0])
    }
  }
})

onUnmounted(() => {
  stopPolling()
  // 组件卸载时，如果播放列表有歌曲但没有当前播放的音乐，设置第一首为当前音乐
  if (musicStore.playlist.length > 0 && !musicStore.currentMusic) {
    musicStore.setCurrentMusicAndIndex(musicStore.playlist[0])
  }
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.analysis-page {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.loading-container {
  padding: 48px;
  @include flex-center;
}

  .analysis-content {
  .analysis-report {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
    backdrop-filter: blur(20px);
    border-radius: var(--radius-xl);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.5);
    overflow: hidden;
    margin-bottom: 32px;
    transition: all var(--transition-base);
    
    [data-theme="dark"] & {
      background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.9) 100%);
      border-color: rgba(255, 255, 255, 0.1);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    }
    
    &:hover {
      box-shadow: 0 12px 48px rgba(0, 0, 0, 0.12);
      
      [data-theme="dark"] & {
        box-shadow: 0 12px 48px rgba(0, 0, 0, 0.5);
      }
    }
    
    // 报告头部
    .report-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 32px 40px;
      background: linear-gradient(135deg, rgba(0, 195, 255, 0.1) 0%, rgba(0, 136, 255, 0.1) 100%);
      border-bottom: 1px solid rgba(0, 195, 255, 0.2);
      
      [data-theme="dark"] & {
        background: linear-gradient(135deg, rgba(0, 195, 255, 0.15) 0%, rgba(0, 136, 255, 0.15) 100%);
        border-bottom-color: rgba(0, 195, 255, 0.3);
      }
      
      .header-content {
        display: flex;
        align-items: center;
        gap: 20px;
        
        .header-icon {
          width: 64px;
          height: 64px;
          border-radius: 16px;
          background: var(--primary-gradient);
          @include flex-center;
          box-shadow: 0 4px 16px rgba(0, 195, 255, 0.3);
          
          .el-icon {
            font-size: 32px;
            color: white;
          }
        }
        
        .header-text {
          .report-title {
            font-size: 28px;
            font-weight: 700;
            margin: 0 0 8px 0;
            @include gradient-text;
          }
          
          .report-subtitle {
            font-size: 14px;
            color: var(--text-gray);
            margin: 0;
            
            [data-theme="dark"] & {
              color: var(--text-light-gray);
            }
          }
        }
      }
      
      .toggle-btn {
        width: 40px;
        height: 40px;
        border: 1px solid rgba(0, 195, 255, 0.3);
        background: rgba(255, 255, 255, 0.8);
        transition: all var(--transition-base);
        
        [data-theme="dark"] & {
          background: rgba(30, 41, 59, 0.8);
          border-color: rgba(0, 195, 255, 0.5);
        }
        
        &:hover {
          background: var(--primary-gradient);
          border-color: transparent;
          color: white;
          transform: scale(1.1);
        }
      }
    }
    
    // 报告内容
    .report-content {
      padding: 40px;
      
      // 推荐理由卡片
      .reason-card {
        display: flex;
        gap: 24px;
        padding: 32px;
        background: linear-gradient(135deg, rgba(0, 195, 255, 0.05) 0%, rgba(124, 58, 237, 0.05) 100%);
        border-radius: var(--radius-lg);
        border-left: 4px solid;
        border-image: var(--primary-gradient) 1;
        margin-bottom: 32px;
        transition: all var(--transition-base);
        
        [data-theme="dark"] & {
          background: linear-gradient(135deg, rgba(0, 195, 255, 0.1) 0%, rgba(124, 58, 237, 0.1) 100%);
        }
        
        &:hover {
          transform: translateX(4px);
          box-shadow: 0 8px 24px rgba(0, 195, 255, 0.15);
          
          [data-theme="dark"] & {
            box-shadow: 0 8px 24px rgba(0, 195, 255, 0.25);
          }
        }
        
        .card-icon {
          width: 56px;
          height: 56px;
          border-radius: 12px;
          @include flex-center;
          flex-shrink: 0;
          
          &.reason-icon {
            background: linear-gradient(135deg, #00c3ff 0%, #0088ff 100%);
            
            .el-icon {
              font-size: 28px;
              color: white;
            }
          }
        }
        
        .card-content {
          flex: 1;
          
          .card-title {
            font-size: 18px;
            font-weight: 600;
            color: var(--text-dark);
            margin: 0 0 12px 0;
            
            [data-theme="dark"] & {
              color: var(--text-light);
            }
          }
          
          .reason-text {
            font-size: 16px;
            line-height: 1.8;
            color: var(--text-gray);
            margin: 0;
            
            [data-theme="dark"] & {
              color: var(--text-light-gray);
            }
          }
        }
      }
      
      // 特征分析区域
      .features-section {
        margin-bottom: 32px;
        
        .section-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 24px;
          
          .section-icon {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            background: var(--primary-gradient);
            @include flex-center;
            
            .el-icon {
              font-size: 20px;
              color: white;
            }
          }
          
          .section-title {
            font-size: 22px;
            font-weight: 600;
            color: var(--text-dark);
            margin: 0;
            
            [data-theme="dark"] & {
              color: var(--text-light);
            }
          }
        }
        
        .features-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
          gap: 20px;
          margin-bottom: 24px;
          
          .feature-card {
            display: flex;
            align-items: center;
            gap: 16px;
            padding: 24px;
            background: white;
            border-radius: var(--radius-md);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
            transition: all var(--transition-base);
            border: 1px solid rgba(0, 0, 0, 0.05);
            
            [data-theme="dark"] & {
              background: rgba(30, 41, 59, 0.8);
              border-color: rgba(255, 255, 255, 0.1);
            }
            
            &:hover {
              transform: translateY(-4px);
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
              
              [data-theme="dark"] & {
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
              }
            }
            
            .feature-icon-wrapper {
              width: 48px;
              height: 48px;
              border-radius: 12px;
              @include flex-center;
              flex-shrink: 0;
              
              .feature-icon {
                font-size: 24px;
              }
            }
            
            &.style-card .feature-icon-wrapper {
              background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
              color: white;
            }
            
            &.emotion-card .feature-icon-wrapper {
              background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
              color: white;
            }
            
            &.rhythm-card .feature-icon-wrapper {
              background: linear-gradient(135deg, #10b981 0%, #059669 100%);
              color: white;
            }
            
            &.atmosphere-card .feature-icon-wrapper {
              background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
              color: white;
            }
            
            .feature-content {
              flex: 1;
              
              .feature-label {
                font-size: 13px;
                color: var(--text-gray);
                margin-bottom: 6px;
                
                [data-theme="dark"] & {
                  color: var(--text-light-gray);
                }
              }
              
              .feature-value {
                font-size: 18px;
                font-weight: 600;
                color: var(--text-dark);
                
                [data-theme="dark"] & {
                  color: var(--text-light);
                }
              }
            }
          }
        }
        
        // 乐器卡片
        .instruments-card,
        .description-card {
          display: flex;
          gap: 24px;
          padding: 32px;
          background: white;
          border-radius: var(--radius-lg);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
          margin-bottom: 24px;
          transition: all var(--transition-base);
          
          [data-theme="dark"] & {
            background: rgba(30, 41, 59, 0.8);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
          }
          
          &:hover {
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
            transform: translateY(-2px);
            
            [data-theme="dark"] & {
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
            }
          }
          
          .card-icon {
            width: 56px;
            height: 56px;
            border-radius: 12px;
            @include flex-center;
            flex-shrink: 0;
            
            &.instruments-icon {
              background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
            }
            
            &.description-icon {
              background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
            }
            
            .el-icon {
              font-size: 28px;
              color: white;
            }
          }
          
          .card-content {
            flex: 1;
            
            .card-title {
              font-size: 18px;
              font-weight: 600;
              color: var(--text-dark);
              margin: 0 0 16px 0;
              
              [data-theme="dark"] & {
                color: var(--text-light);
              }
            }
            
            .instruments-list {
              display: flex;
              flex-wrap: wrap;
              gap: 12px;
              
              .instrument-badge {
                padding: 8px 16px;
                background: linear-gradient(135deg, rgba(236, 72, 153, 0.1) 0%, rgba(219, 39, 119, 0.1) 100%);
                border: 1px solid rgba(236, 72, 153, 0.2);
                border-radius: 20px;
                font-size: 14px;
                font-weight: 500;
                color: #db2777;
                transition: all var(--transition-base);
                
                [data-theme="dark"] & {
                  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2) 0%, rgba(219, 39, 119, 0.2) 100%);
                  border-color: rgba(236, 72, 153, 0.4);
                  color: #ec4899;
                }
                
                &:hover {
                  background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
                  color: white;
                  transform: scale(1.05);
                }
              }
            }
            
            .description-text {
              font-size: 16px;
              line-height: 1.8;
              color: var(--text-gray);
              margin: 0;
              
              [data-theme="dark"] & {
                color: var(--text-light-gray);
              }
            }
          }
        }
      }
      
      // 特征向量区域
      .vector-section {
        margin-bottom: 32px;
        
        .section-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 24px;
          
          .section-icon {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            background: var(--primary-gradient);
            @include flex-center;
            
            .el-icon {
              font-size: 20px;
              color: white;
            }
          }
          
          .section-title {
            font-size: 22px;
            font-weight: 600;
            color: var(--text-dark);
            margin: 0;
            
            [data-theme="dark"] & {
              color: var(--text-light);
            }
          }
        }
        
        .vector-grid {
          display: grid;
          gap: 20px;
          
          .vector-card {
            padding: 24px 32px;
            background: white;
            border-radius: var(--radius-lg);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
            transition: all var(--transition-base);
            border: 1px solid rgba(0,0,0,0.02);
            
            [data-theme="dark"] & {
              background: rgba(255, 255, 255, 0.05);
              border-color: rgba(255, 255, 255, 0.05);
            }
            
            &:hover {
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
              transform: translateY(-2px);
              
              [data-theme="dark"] & {
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
              }
            }
            
            .vector-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 16px;
              
              .vector-label {
                font-size: 16px;
                font-weight: 600;
                color: var(--text-dark);
                display: flex;
                align-items: center;
                gap: 8px;
                
                [data-theme="dark"] & {
                  color: var(--text-light);
                }
                
                &::before {
                  content: '';
                  width: 4px;
                  height: 16px;
                  background: var(--primary-color);
                  border-radius: 2px;
                }
              }
              
              .vector-percentage {
                font-size: 18px;
                font-weight: 700;
                color: var(--primary-color);
                font-family: 'Roboto', monospace;
              }
            }
            
            .vector-progress-wrapper {
              height: 16px;
              background: rgba(0, 0, 0, 0.05);
              border-radius: 10px;
              overflow: hidden;
              position: relative;
              
              [data-theme="dark"] & {
                background: rgba(255, 255, 255, 0.1);
              }
              
              .vector-progress {
                height: 100%;
                border-radius: 10px;
                transition: width 1.2s cubic-bezier(0.22, 1, 0.36, 1);
                box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                position: relative;
                overflow: hidden;
                
                // 增加流光效果
                &::after {
                  content: '';
                  position: absolute;
                  top: 0;
                  left: 0;
                  bottom: 0;
                  right: 0;
                  background: linear-gradient(
                    45deg,
                    rgba(255, 255, 255, 0) 25%,
                    rgba(255, 255, 255, 0.3) 50%,
                    rgba(255, 255, 255, 0) 75%
                  );
                  background-size: 200% 100%;
                  animation: shimmer 2s infinite linear;
                }
              }
            }
          }
        }
      }
      
      // 可视化区域
      .visualization-section {
        .section-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 24px;
          
          .section-icon {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            background: var(--primary-gradient);
            @include flex-center;
            
            .el-icon {
              font-size: 20px;
              color: white;
            }
          }
          
          .section-title {
            font-size: 22px;
            font-weight: 600;
            color: var(--text-dark);
            margin: 0;
            
            [data-theme="dark"] & {
              color: var(--text-light);
            }
          }
        }
        
        .visualization-tabs {
          display: flex;
          justify-content: center;
          margin-bottom: 32px;
          
          :deep(.el-radio-group) {
            .el-radio-button {
              .el-radio-button__inner {
                padding: 12px 24px;
                border-radius: var(--radius-md);
                font-weight: 500;
                display: flex;
                align-items: center;
                gap: 8px;
                background: white;
                color: var(--text-dark);
                border-color: rgba(0, 0, 0, 0.1);
                
                [data-theme="dark"] & {
                  background: rgba(30, 41, 59, 0.8);
                  color: var(--text-light);
                  border-color: rgba(255, 255, 255, 0.1);
                }
                
                .el-icon {
                  font-size: 18px;
                }
              }
              
              &.is-active .el-radio-button__inner {
                background: var(--primary-gradient);
                border-color: transparent;
                color: white;
              }
            }
          }
        }
        
        .radar-chart-container {
          width: 100%;
          height: 500px;
          background: white;
          border-radius: var(--radius-lg);
          padding: 24px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
          
          [data-theme="dark"] & {
            background: rgba(30, 41, 59, 0.8);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
          }
        }
        
        .feature-3d-container {
          width: 100%;
          min-height: 500px;
          border-radius: var(--radius-lg);
          overflow: hidden;
          background: linear-gradient(135deg, rgba(0, 195, 255, 0.05) 0%, rgba(124, 58, 237, 0.05) 100%);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
          
          [data-theme="dark"] & {
            background: linear-gradient(135deg, rgba(0, 195, 255, 0.1) 0%, rgba(124, 58, 237, 0.1) 100%);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
          }
        }
      }
    }
  }
  
  .recommendations-section {
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      
      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 24px;
        font-weight: 600;
        margin: 0;
        color: var(--text-dark);
        
        [data-theme="dark"] & {
          color: var(--text-light);
        }
        
        .el-icon {
          color: var(--secondary-color);
        }
      }
    }
    
    .recommendations-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 24px;
      
      .recommendation-card {
        position: relative;
        
        .rank-badge {
          position: absolute;
          top: -12px;
          left: -12px;
          width: 32px;
          height: 32px;
          background: var(--primary-gradient);
          color: white;
          border-radius: 50%;
          @include flex-center;
          font-weight: 600;
          font-size: 14px;
          z-index: 10;
          box-shadow: var(--shadow-md);
        }
      }
    }
    
    .empty-state {
      padding: 48px 0;
      text-align: center;
    }
  }
}

.error-state {
  padding: 48px 0;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
