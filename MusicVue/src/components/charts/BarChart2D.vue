<template>
  <div class="bar-chart-2d">
    <v-chart class="chart" :option="chartOption" autoresize />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

interface Props {
  data: Array<{ name: string; value: number; color?: string }>
  categories?: string[]
}

const props = defineProps<Props>()

const chartOption = computed(() => ({
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
    data: props.data.map(item => item.name),
    axisLabel: {
      rotate: props.data.length > 6 ? 45 : 0
    }
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '数值',
      type: 'bar',
      data: props.data.map(item => ({
        value: item.value,
        itemStyle: {
          color: item.color || '#667eea'
        }
      })),
      barWidth: '60%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))
</script>

<style scoped lang="scss">
.bar-chart-2d {
  width: 100%;
  height: 100%;
  min-height: 400px;
  
  .chart {
    width: 100%;
    height: 100%;
  }
}
</style>

