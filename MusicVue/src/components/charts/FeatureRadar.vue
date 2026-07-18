<template>
  <div class="feature-radar">
    <v-chart
      class="chart"
      :option="chartOption"
      autoresize
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  RadarComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  RadarComponent
])

const props = defineProps<{
  features: number[]
  labels?: string[]
}>()

const defaultLabels = [
  '节奏',
  '旋律',
  '和声',
  '音色',
  '动态',
  '情感',
  '风格',
  '能量'
]

const chartOption = computed(() => {
  const labels = props.labels || defaultLabels
  const features = props.features || Array(labels.length).fill(0.5)
  
  // 归一化特征值到0-100
  const normalizedFeatures = features.map((val: number) => Math.min(val * 100, 100))
  
  return {
    title: {
      text: '音频特征分析',
      left: 'center',
      textStyle: {
        fontSize: 18,
        fontWeight: 600,
        color: '#1F2937'
      }
    },
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: labels.map((label, index) => ({
        name: label,
        max: 100
      })),
      center: ['50%', '55%'],
      radius: '70%',
      nameGap: 10,
      splitNumber: 4,
      shape: 'polygon',
      splitArea: {
        areaStyle: {
          color: [
            'rgba(59, 130, 246, 0.1)',
            'rgba(59, 130, 246, 0.2)',
            'rgba(59, 130, 246, 0.3)',
            'rgba(59, 130, 246, 0.4)'
          ]
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(59, 130, 246, 0.3)'
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(59, 130, 246, 0.3)'
        }
      },
      name: {
        textStyle: {
          color: '#6B7280',
          fontSize: 14
        }
      }
    },
    series: [
      {
        name: '音频特征',
        type: 'radar',
        data: [
          {
            value: normalizedFeatures,
            name: '特征值',
            areaStyle: {
              color: 'rgba(59, 130, 246, 0.3)'
            },
            lineStyle: {
              color: '#3B82F6',
              width: 2
            },
            itemStyle: {
              color: '#3B82F6'
            }
          }
        ]
      }
    ]
  }
})
</script>

<style scoped lang="scss">
.feature-radar {
  width: 100%;
  height: 400px;
  
  .chart {
    width: 100%;
    height: 100%;
  }
}
</style>

