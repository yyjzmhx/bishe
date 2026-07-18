<template>
  <div class="ai-monitor-tab">
    <div class="stats-grid">
      <div class="stat-card" :style="{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }">
        <div class="stat-icon">
          <el-icon :size="40"><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">推荐准确率</p>
          <p class="stat-value">{{ accuracy.toFixed(1) }}%</p>
        </div>
      </div>

      <div class="stat-card" :style="{ background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' }">
        <div class="stat-icon">
          <el-icon :size="40"><Connection /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">平均相似度</p>
          <p class="stat-value">{{ (avgSimilarity * 100).toFixed(1) }}%</p>
        </div>
      </div>

      <div class="stat-card" :style="{ background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' }">
        <div class="stat-icon">
          <el-icon :size="40"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">推荐总数</p>
          <p class="stat-value">{{ totalRecommends }}</p>
        </div>
      </div>

      <div class="stat-card" :style="{ background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' }">
        <div class="stat-icon">
          <el-icon :size="40"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">AI 服务状态</p>
          <p class="stat-value">
            <el-tag :type="aiStatus.online ? 'success' : 'danger'" size="large">
              {{ aiStatus.online ? '在线' : '离线' }}
            </el-tag>
          </p>
          <p class="stat-extra">
            {{ aiStatus.provider || '未启用配置' }}
            <span v-if="aiStatus.model">· {{ aiStatus.model }}</span>
            <span v-if="aiStatus.analyzingCount">· 分析中 {{ aiStatus.analyzingCount }}</span>
          </p>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card">
        <h3 class="card-title">反馈类型分布</h3>
        <v-chart class="chart" :option="feedbackTypeOption" autoresize />
      </div>

      <div class="chart-card">
        <h3 class="card-title">曲风分布</h3>
        <v-chart class="chart" :option="featureDistributionOption" autoresize />
      </div>
    </div>

    <div class="summary-card">
      <div class="summary-item">
        <span class="summary-label">积极反馈</span>
        <span class="summary-value">{{ positiveFeedback }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">负向反馈</span>
        <span class="summary-value">{{ negativeFeedback }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">收藏总数</span>
        <span class="summary-value">{{ feedbackAggregation.favoriteCount }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">活跃互动用户</span>
        <span class="summary-value">{{ feedbackAggregation.activeUsers }}</span>
      </div>
    </div>

    <div class="refresh-section">
      <el-button type="primary" :icon="Refresh" @click="loadAllData" :loading="loading">
        刷新数据
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { CircleCheck, Connection, DataAnalysis, Document, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

use([
  CanvasRenderer,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const loading = ref(false)
const accuracy = ref(0)
const avgSimilarity = ref(0)
const totalRecommends = ref(0)
const positiveFeedback = ref(0)
const negativeFeedback = ref(0)
const aiStatus = ref<any>({ online: false, analyzingCount: 0 })
const feedbackAggregation = ref({
  likeCount: 0,
  dislikeCount: 0,
  commentCount: 0,
  favoriteCount: 0,
  activeUsers: 0,
  totalFeedback: 0
})
const featureDistribution = ref<Record<string, number>>({})

const feedbackTypeOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    top: 'middle'
  },
  series: [
    {
      type: 'pie',
      radius: ['42%', '72%'],
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      data: [
        { value: feedbackAggregation.value.likeCount, name: '点赞', itemStyle: { color: '#10B981' } },
        { value: feedbackAggregation.value.dislikeCount, name: '差评', itemStyle: { color: '#EF4444' } },
        { value: feedbackAggregation.value.commentCount, name: '评论', itemStyle: { color: '#3B82F6' } },
        { value: feedbackAggregation.value.favoriteCount, name: '收藏', itemStyle: { color: '#F59E0B' } }
      ]
    }
  ]
}))

const featureDistributionOption = computed(() => {
  const entries = Object.entries(featureDistribution.value)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: entries.map(([name]) => name)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        type: 'bar',
        data: entries.map(([, value]) => value),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#00c3ff' },
              { offset: 1, color: '#0088ff' }
            ]
          }
        },
        barWidth: '56%'
      }
    ]
  }
})

const loadAllData = async () => {
  loading.value = true
  try {
    const accuracyResult: any = await request.get('/admin/ai/accuracy')
    accuracy.value = accuracyResult.accuracy || 0
    avgSimilarity.value = accuracyResult.avgSimilarity || 0
    totalRecommends.value = accuracyResult.totalRecommends || 0
    positiveFeedback.value = accuracyResult.positiveFeedback || 0
    negativeFeedback.value = accuracyResult.negativeFeedback || 0

    const feedbackResult: any = await request.get('/admin/ai/feedback/aggregation')
    feedbackAggregation.value = {
      ...feedbackAggregation.value,
      ...feedbackResult
    }

    const featureResult: any = await request.get('/admin/ai/features/distribution')
    featureDistribution.value = featureResult.distribution || {}

    const statusResult: any = await request.get('/admin/ai/status')
    aiStatus.value = statusResult || {}
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.ai-monitor-tab {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 24px;
    margin-bottom: 32px;
  }

  .stat-card {
    @include glass-card;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 20px;
    color: #fff;
    border: none;
  }

  .stat-icon {
    flex-shrink: 0;
    width: 64px;
    height: 64px;
    border-radius: var(--radius-md);
    @include flex-center;
    background: rgba(255, 255, 255, 0.2);
  }

  .stat-label {
    font-size: 14px;
    opacity: 0.9;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 32px;
    font-weight: 800;
    margin: 0;
  }

  .stat-extra {
    margin-top: 8px;
    font-size: 13px;
    opacity: 0.9;
  }

  .charts-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
    gap: 24px;
    margin-bottom: 24px;
  }

  .chart-card,
  .summary-card {
    @include glass-card;
    padding: 24px;
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 20px;
    color: var(--text-dark);
  }

  .chart {
    width: 100%;
    height: 320px;
  }

  .summary-card {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }

  .summary-item {
    padding: 18px;
    border-radius: var(--radius-lg);
    background: rgba(0, 195, 255, 0.06);
  }

  .summary-label {
    display: block;
    color: var(--text-gray);
    font-size: 13px;
    margin-bottom: 8px;
  }

  .summary-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-dark);
  }

  .refresh-section {
    display: flex;
    justify-content: center;

    .el-button {
      background: var(--primary-gradient);
      border: none;
      border-radius: 8px;
      padding: 12px 32px;
      color: #fff;
      font-weight: 500;
    }
  }
}
</style>
