<template>
  <div class="pie-chart-2d">
    <v-chart class="chart" :option="chartOption" autoresize />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent
])

interface Props {
  data: Array<{ name: string; value: number; color?: string; percentage?: string }>
}

const props = defineProps<Props>()

const chartOption = computed(() => ({
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
      name: '数据',
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
      data: props.data.map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: {
          color: item.color || '#667eea'
        }
      }))
    }
  ]
}))
</script>

<style scoped lang="scss">
.pie-chart-2d {
  width: 100%;
  height: 100%;
  min-height: 400px;
  
  .chart {
    width: 100%;
    height: 100%;
  }
}
</style>

