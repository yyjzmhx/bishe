<template>
  <div class="bar-chart-3d-wrapper" :style="{ width: width, height: height }">
    <!-- WebGL 不支持时显示降级方案 -->
    <div v-if="!webglSupported && showFallback" class="fallback-container">
      <slot name="fallback">
        <BarChart2D :data="chartData" :categories="categories" />
        <el-alert
          v-if="showFallbackNotice"
          type="info"
          :closable="false"
          class="fallback-notice"
        >
          您的浏览器不支持 WebGL，已切换到 2D 视图
        </el-alert>
      </slot>
    </div>
    
    <!-- 3D 柱状图 -->
    <div
      v-else
      ref="containerRef"
      class="bar-chart-3d-container"
      @mousedown="handleMouseDown"
      @mousemove="handleMouseMove"
      @mouseup="handleMouseUp"
      @wheel="handleWheel"
    ></div>
    
    <!-- 控制面板 -->
    <div v-if="webglSupported && showControls" class="chart-controls">
      <el-button-group size="small">
        <el-button @click="resetCamera">
          <el-icon><Refresh /></el-icon>
          重置视角
        </el-button>
        <el-button @click="toggleAutoRotate">
          <el-icon><VideoPlay v-if="!autoRotate" /><VideoPause v-else /></el-icon>
          {{ autoRotate ? '暂停' : '自动旋转' }}
        </el-button>
      </el-button-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { useWebGL } from '@/composables/useWebGL'
import BarChart2D from '@/components/charts/BarChart2D.vue'
import { Refresh, VideoPlay, VideoPause } from '@element-plus/icons-vue'

export interface BarChart3DData {
  name: string
  value: number
  color?: string
}

interface Props {
  data: BarChart3DData[]
  categories?: string[] // X轴分类（用于多系列）
  series?: string[] // 系列名称
  width?: string
  height?: string
  autoRotate?: boolean
  rotateSpeed?: number
  showGrid?: boolean
  showLabels?: boolean
  showAxes?: boolean
  animation?: boolean
  showControls?: boolean
  showFallback?: boolean
  showFallbackNotice?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  categories: undefined,
  series: undefined,
  width: '100%',
  height: '500px',
  autoRotate: false,
  rotateSpeed: 1,
  showGrid: true,
  showLabels: true,
  showAxes: true,
  animation: true,
  showControls: true,
  showFallback: true,
  showFallbackNotice: true
})

const { isSupported: webglSupported } = useWebGL()
const containerRef = ref<HTMLElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let bars: THREE.Group
let animationId: number
let isMouseDown = false
let mouseX = 0
let mouseY = 0

// 计算图表数据
const chartData = computed(() => {
  return props.data.map(item => ({
    name: item.name,
    value: item.value,
    color: item.color || getDefaultColor(item.value)
  }))
})

// 获取默认颜色（根据值的大小）
const getDefaultColor = (value: number) => {
  const maxValue = Math.max(...props.data.map(d => d.value))
  const ratio = value / maxValue
  const hue = 200 + ratio * 160 // 从蓝色到紫色
  return `hsl(${hue}, 70%, 60%)`
}

// 初始化3D场景
const initScene = () => {
  if (!containerRef.value || !webglSupported.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  // 创建场景
  scene = new THREE.Scene()
  scene.background = null

  // 创建相机
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000)
  camera.position.set(8, 6, 8)
  camera.lookAt(0, 0, 0)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  containerRef.value.appendChild(renderer.domElement)

  // 添加光照
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(10, 10, 5)
  directionalLight.castShadow = true
  scene.add(directionalLight)

  const pointLight = new THREE.PointLight(0x667eea, 0.5)
  pointLight.position.set(-10, -10, -5)
  scene.add(pointLight)

  // 添加网格
  if (props.showGrid) {
    const gridHelper = new THREE.GridHelper(10, 10, 0x888888, 0xcccccc)
    scene.add(gridHelper)
  }

  // 添加坐标轴
  if (props.showAxes) {
    const axesHelper = new THREE.AxesHelper(5)
    scene.add(axesHelper)
  }

  // 创建柱状图
  createBars()

  // 添加轨道控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = props.autoRotate
  controls.autoRotateSpeed = props.rotateSpeed
  controls.minDistance = 5
  controls.maxDistance = 30
  controls.enableZoom = true
  controls.enablePan = true

  // 动画循环
  animate()

  // 窗口大小调整
  const handleResize = () => {
    if (!containerRef.value) return
    const w = containerRef.value.clientWidth
    const h = containerRef.value.clientHeight

    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
  }

  window.addEventListener('resize', handleResize)

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (animationId) {
      cancelAnimationFrame(animationId)
    }
    if (containerRef.value && renderer) {
      containerRef.value.removeChild(renderer.domElement)
    }
    renderer?.dispose()
  })
}

// 创建柱体
const createBars = () => {
  if (bars) {
    scene.remove(bars)
  }

  bars = new THREE.Group()
  const data = chartData.value
  const maxValue = Math.max(...data.map(d => d.value))
  const barWidth = 0.6
  const barSpacing = 1.2
  const baseHeight = 0.1

  data.forEach((item, index) => {
    const height = (item.value / maxValue) * 5 + baseHeight
    const geometry = new THREE.BoxGeometry(barWidth, height, barWidth)
    
    // 创建渐变材质
    const color = new THREE.Color(item.color || '#667eea')
    const material = new THREE.MeshStandardMaterial({
      color: color,
      metalness: 0.3,
      roughness: 0.4,
      emissive: color,
      emissiveIntensity: 0.1
    })

    const bar = new THREE.Mesh(geometry, material)
    bar.position.set(
      (index - data.length / 2 + 0.5) * barSpacing,
      height / 2,
      0
    )
    bar.castShadow = true
    bar.receiveShadow = true
    bar.userData = { name: item.name, value: item.value }

    // 添加标签（使用简单的文本精灵）
    if (props.showLabels) {
      // 这里可以添加3D文本标签，简化版本先跳过
    }

    bars.add(bar)
  })

  scene.add(bars)

  // 添加动画
  if (props.animation) {
    bars.children.forEach((bar, index) => {
      bar.scale.y = 0
      bar.position.y = baseHeight / 2
      
      setTimeout(() => {
        const targetHeight = (data[index].value / maxValue) * 5 + baseHeight
        animateBar(bar, targetHeight, 1000)
      }, index * 100)
    })
  }
}

// 柱体动画
const animateBar = (bar: THREE.Mesh, targetHeight: number, duration: number) => {
  const startHeight = bar.scale.y
  const startTime = Date.now()

  const animate = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    // 使用缓动函数
    const easeOutCubic = 1 - Math.pow(1 - progress, 3)
    const currentHeight = startHeight + (targetHeight - startHeight) * easeOutCubic
    
    bar.scale.y = currentHeight
    bar.position.y = (currentHeight * 0.5) + 0.05

    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }

  animate()
}

// 动画循环
const animate = () => {
  animationId = requestAnimationFrame(animate)
  controls.update()
  renderer.render(scene, camera)
}

// 鼠标事件处理
const handleMouseDown = (event: MouseEvent) => {
  isMouseDown = true
  mouseX = event.clientX
  mouseY = event.clientY
}

const handleMouseMove = (event: MouseEvent) => {
  if (!isMouseDown) return
  // OrbitControls 会自动处理
}

const handleMouseUp = () => {
  isMouseDown = false
}

const handleWheel = (event: WheelEvent) => {
  event.preventDefault()
  // OrbitControls 会自动处理
}

// 重置相机
const resetCamera = () => {
  if (camera && controls) {
    camera.position.set(8, 6, 8)
    camera.lookAt(0, 0, 0)
    controls.reset()
  }
}

// 切换自动旋转
const autoRotate = ref(props.autoRotate)
const toggleAutoRotate = () => {
  autoRotate.value = !autoRotate.value
  if (controls) {
    controls.autoRotate = autoRotate.value
  }
}

// 监听数据变化
watch(() => props.data, () => {
  if (scene && webglSupported.value) {
    createBars()
  }
}, { deep: true })

onMounted(() => {
  if (webglSupported.value) {
    initScene()
  }
})
</script>

<style scoped lang="scss">
.bar-chart-3d-wrapper {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.bar-chart-3d-container {
  width: 100%;
  height: 100%;
  
  canvas {
    display: block;
    outline: none;
    cursor: grab;
    
    &:active {
      cursor: grabbing;
    }
  }
}

.fallback-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.fallback-notice {
  margin-top: 16px;
}

.chart-controls {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
}

[data-theme="dark"] {
  .bar-chart-3d-wrapper {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  }
}
</style>

