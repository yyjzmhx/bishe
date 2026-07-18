<template>
  <div class="ai-monitor-page">
    <h2 class="page-title text-gradient">AI监控</h2>
    
    <!-- 统计卡片 -->
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
          <p class="stat-label">AI服务状态</p>
          <p class="stat-value">
            <el-tag :type="aiStatus.online ? 'success' : 'danger'" size="large">
              {{ aiStatus.online ? '在线' : '离线' }}
            </el-tag>
          </p>
        </div>
      </div>
    </div>
    
    <!-- 图表区域 -->
    <div class="charts-grid">
      <div class="chart-card">
        <h3 class="card-title">反馈类型分布</h3>
        <v-chart class="chart" :option="feedbackTypeOption" autoresize />
      </div>
      
      <div class="chart-card">
        <h3 class="card-title">特征分布</h3>
        <v-chart class="chart" :option="featureDistributionOption" autoresize />
      </div>
    </div>
    
    <!-- 刷新按钮 -->
    <div class="refresh-section">
      <el-button type="primary" :icon="Refresh" @click="loadAllData" :loading="loading">
        刷新数据
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { DataAnalysis, Connection, Document, CircleCheck, Refresh } from '@element-plus/icons-vue'
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
const aiStatus = ref({ online: true, analyzingCount: 0 })
const feedbackAggregation = ref({
  likeCount: 0,
  dislikeCount: 0,
  commentCount: 0,
  totalFeedback: 0
})
const featureDistribution = ref<Record<string, number>>({})

const feedbackTypeOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    top: 'middle'
  },
  series: [
    {
      name: '反馈类型',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      data: [
        { 
          value: feedbackAggregation.value.likeCount, 
          name: '点赞',
          itemStyle: { color: '#10B981' }
        },
        { 
          value: feedbackAggregation.value.dislikeCount, 
          name: '差评',
          itemStyle: { color: '#EF4444' }
        },
        { 
          value: feedbackAggregation.value.commentCount, 
          name: '评论',
          itemStyle: { color: '#3B82F6' }
        }
      ]
    }
  ]
}))

const featureDistributionOption = computed(() => {
  const data = Object.entries(featureDistribution.value).map(([name, value]) => ({
    name,
    value
  }))
  
  if (data.length === 0) {
    return {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: []
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '数量',
          type: 'bar',
          data: []
        }
      ]
    }
  }
  
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.name),
      axisLabel: {
        rotate: 0
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '数量',
        type: 'bar',
        data: data.map(item => item.value),
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
        barWidth: '60%'
      }
    ]
  }
})

const loadAllData = async () => {
  loading.value = true
  try {
    // 加载准确率统计
    const accuracyResult: any = await request.get('/admin/ai/accuracy')
    accuracy.value = accuracyResult.accuracy || 0
    avgSimilarity.value = accuracyResult.avgSimilarity || 0
    totalRecommends.value = accuracyResult.totalRecommends || 0
    
    // 加载反馈聚合
    const feedbackResult: any = await request.get('/admin/ai/feedback/aggregation')
    feedbackAggregation.value = feedbackResult || {}
    
    // 加载特征分布
    const featureResult: any = await request.get('/admin/ai/features/distribution')
    featureDistribution.value = featureResult.distribution || {}
    
    // 加载AI服务状态
    const statusResult: any = await request.get('/admin/ai/status')
    aiStatus.value = statusResult || {}
    
    ElMessage.success('数据刷新成功')
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

.ai-monitor-page {
  max-width: 1600px;
  margin: 0 auto;
  
  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 24px;
    font-family: 'Roboto', sans-serif;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
  
  .stat-card {
    @include glass-card;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 20px;
    color: #fff;
    border: none;
    
    .stat-icon {
      flex-shrink: 0;
      width: 64px;
      height: 64px;
      border-radius: var(--radius-md);
      @include flex-center;
      background: rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(10px);
    }
    
    .stat-info {
      flex: 1;
      
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
    }
  }
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
  
  .chart-card {
    @include glass-card;
    padding: 24px;
    
    .card-title {
      font-size: 18px;
      font-weight: 600;
      margin: 0 0 20px 0;
      color: var(--text-dark);
    }
    
    .chart {
      width: 100%;
      height: 300px;
    }
  }
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
    transition: all 0.2s;
    box-shadow: 0 2px 6px rgba(0, 195, 255, 0.25);
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 195, 255, 0.35);
    }
  }
}
</style>

