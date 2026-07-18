<template>
  <div class="dashboard-page">
    <h2 class="page-title text-gradient">数据监测</h2>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <NextCard
        v-for="stat in stats"
        :key="stat.key"
        class="stat-card fade-in"
        variant="shadow"
        hover
        three-d
        :style="{ animationDelay: `${stat.index * 0.1}s` }"
      >
        <!-- 背景装饰大图标 -->
        <div class="bg-icon">
          <el-icon :size="120">
            <component :is="stat.icon" />
          </el-icon>
        </div>
        
        <div class="stat-content">
          <div class="stat-icon-wrapper">
            <div class="stat-icon" :style="{ background: stat.gradient }">
              <el-icon :size="32">
                <component :is="stat.icon" />
              </el-icon>
            </div>
          </div>
          <div class="stat-info">
            <p class="stat-label">{{ stat.label }}</p>
            <p class="stat-value">{{ stat.value }}</p>
            <p class="stat-trend" :class="stat.trend > 0 ? 'positive' : 'negative'">
              <el-icon><ArrowUp v-if="stat.trend > 0" /><ArrowDown v-else /></el-icon>
              {{ Math.abs(stat.trend) }}%
            </p>
          </div>
        </div>
      </NextCard>
    </div>
    
    <!-- 图表区域 -->
    <div class="charts-grid">
      <NextCard class="chart-card" variant="shadow" hover>
        <template #header>
          <div class="chart-header">
            <h3 class="card-title text-gradient">用户增长趋势</h3>
            <el-radio-group v-model="userGrowthViewType" size="small">
              <el-radio-button label="2d">2D</el-radio-button>
              <el-radio-button label="3d">3D</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="chart-wrapper">
          <BarChart3D
            v-if="userGrowthViewType === '3d'"
            :data="userGrowth3DData"
            height="400px"
            :auto-rotate="false"
            :show-controls="true"
          />
          <v-chart v-else class="chart" :option="userGrowthOption" autoresize />
        </div>
      </NextCard>
      
      <NextCard class="chart-card" variant="shadow" hover>
        <template #header>
          <div class="chart-header">
            <h3 class="card-title text-gradient">音乐类型分布</h3>
            <el-radio-group v-model="genreDistributionViewType" size="small">
              <el-radio-button label="2d">2D</el-radio-button>
              <el-radio-button label="3d">3D</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="chart-wrapper">
          <PieChart3D
            v-if="genreDistributionViewType === '3d'"
            :data="genreDistribution3DData"
            height="400px"
            :auto-rotate="true"
            :show-controls="true"
            :depth="0.5"
            :inner-radius="1"
            :outer-radius="3"
          />
          <v-chart v-else class="chart" :option="genreDistributionOption" autoresize />
        </div>
      </NextCard>
    </div>
    
    <!-- 最近活动 -->
    <el-card class="activity-card">
      <template #header>
        <h3 class="card-title">最近活动</h3>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="activity in activities"
          :key="activity.id"
          :timestamp="activity.time"
          placement="top"
        >
          <el-card>
            <h4>{{ activity.title }}</h4>
            <p>{{ activity.description }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
  User,
  VideoCamera,
  Upload,
  Star,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'
import NextCard from '@/components/ui/NextCard.vue'
import BarChart3D from '@/components/3d/BarChart3D.vue'
import PieChart3D from '@/components/3d/PieChart3D.vue'
import request from '@/api/request'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const stats = ref([
  {
    key: 'users',
    label: '总用户数',
    value: 0,
    trend: 12.5,
    icon: User,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    index: 0
  },
  {
    key: 'music',
    label: '音乐总数',
    value: 0,
    trend: 8.3,
    icon: VideoCamera,
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    index: 1
  },
  {
    key: 'uploads',
    label: '上传总数',
    value: 0,
    trend: 15.2,
    icon: Upload,
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    index: 2
  },
  {
    key: 'feedback',
    label: '反馈总数',
    value: 0,
    trend: -2.1,
    icon: Star,
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    index: 3
  }
])

const activities = ref([
  {
    id: 1,
    title: '新用户注册',
    description: '用户 "musiclover123" 注册了账号',
    time: '2小时前'
  },
  {
    id: 2,
    title: '音乐上传',
    description: '用户上传了新的音频文件',
    time: '3小时前'
  },
  {
    id: 3,
    title: '推荐生成',
    description: 'AI完成了音频分析并生成了推荐',
    time: '5小时前'
  }
])

// 图表视图类型
const userGrowthViewType = ref<'2d' | '3d'>('2d')
const genreDistributionViewType = ref<'2d' | '3d'>('2d')

// 3D图表数据
const userGrowth3DData = computed(() => [
  { name: '1月', value: 120 },
  { name: '2月', value: 200 },
  { name: '3月', value: 150 },
  { name: '4月', value: 300 },
  { name: '5月', value: 280 },
  { name: '6月', value: 350 }
])

const genreDistribution3DData = computed(() => [
  { name: '流行', value: 335, color: '#3B82F6' },
  { name: '摇滚', value: 310, color: '#7C3AED' },
  { name: '电子', value: 234, color: '#10B981' },
  { name: '爵士', value: 135, color: '#F59E0B' },
  { name: '古典', value: 154, color: '#EF4444' }
])

const userGrowthOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: ['1月', '2月', '3月', '4月', '5月', '6月']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      data: [120, 200, 150, 300, 280, 350],
      type: 'line',
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.1)' }
          ]
        }
      },
      lineStyle: {
        color: '#3B82F6'
      },
      itemStyle: {
        color: '#3B82F6'
      }
    }
  ]
}))

const genreDistributionOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [
    {
      name: '音乐类型',
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
        { value: 335, name: '流行', itemStyle: { color: '#3B82F6' } },
        { value: 310, name: '摇滚', itemStyle: { color: '#7C3AED' } },
        { value: 234, name: '电子', itemStyle: { color: '#10B981' } },
        { value: 135, name: '爵士', itemStyle: { color: '#F59E0B' } },
        { value: 154, name: '古典', itemStyle: { color: '#EF4444' } }
      ]
    }
  ]
}))

const loadStats = async () => {
  try {
    const data = await request.get('/admin/dashboard/stats')
    stats.value[0].value = data.totalUsers || 0
    stats.value[1].value = data.totalMusic || 0
    stats.value[2].value = data.totalUploads || 0
    stats.value[3].value = data.totalFeedback || 0
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.dashboard-page {
  max-width: 1600px;
  margin: 0 auto;
  
  .page-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 32px;
    font-family: 'Roboto', sans-serif;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
  
  .stat-card {
    position: relative;
    overflow: hidden;
    height: 140px;
    
    // 背景装饰大图标
    .bg-icon {
      position: absolute;
      right: -20px;
      top: -20px;
      opacity: 0.05;
      transform: rotate(15deg);
      color: var(--text-dark);
      pointer-events: none;
    }
    
    .stat-content {
      display: flex;
      align-items: center;
      gap: 20px;
      position: relative;
      z-index: 1;
      
      .stat-icon-wrapper {
        flex-shrink: 0;
      }
      
      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: var(--radius-md);
        @include flex-center;
        color: white;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
      
      .stat-info {
        flex: 1;
        min-width: 0;
        
        .stat-label {
          font-size: 14px;
          color: var(--text-gray);
          margin-bottom: 8px;
        }
        
        .stat-value {
          font-size: 36px;
          font-weight: 800;
          // 数字渐变色（使用播放器同款青蓝色）
          background: linear-gradient(135deg, var(--text-dark), var(--primary-color));
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          margin-bottom: 4px;
        }
        
        .stat-trend {
          font-size: 13px;
          display: flex;
          align-items: center;
          gap: 4px;
          font-weight: 500;
          
          &.positive {
            color: var(--accent-color);
          }
          
          &.negative {
            color: var(--danger-color);
          }
        }
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
    
    :deep(.el-card__header) {
      background: transparent;
      border-bottom: 1px solid var(--glass-border);
    }
    
    .card-title {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      color: var(--text-dark);
    }
    
    .chart-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
    }
    
    .chart-wrapper {
      padding: 8px 0;
      min-height: 400px;
    }
    
    .chart {
      width: 100%;
      height: 400px;
    }
  }
}

.activity-card {
  @include glass-card;
  
  :deep(.el-card__header) {
    background: transparent;
    border-bottom: 1px solid var(--glass-border);
  }
  
  .card-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: var(--text-dark);
  }
}
</style>

