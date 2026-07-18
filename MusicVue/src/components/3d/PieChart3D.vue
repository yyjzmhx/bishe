<template>
  <div class="pie-chart-3d-wrapper" :style="{ width: width, height: height }">
    <!-- WebGL 不支持时显示降级方案 -->
    <div v-if="!webglSupported && showFallback" class="fallback-container">
      <slot name="fallback">
        <PieChart2D :data="chartData" />
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
    
    <!-- 3D 饼图 -->
    <div
      v-else
      ref="containerRef"
      class="pie-chart-3d-container"
    ></div>
    
    <!-- 控制面板 -->
    <div v-if="webglSupported && showControls" class="chart-controls">
      <el-button-group size="small">
        <el-button @click="resetCamera">
          <el-icon><Refresh /></el-icon>
          重置视角
        </el-button>
        <el-button @click="toggleAutoRotate">
          <el-icon><VideoPlay v-if="!autoRotateState" /><VideoPause v-else /></el-icon>
          {{ autoRotateState ? '暂停' : '自动旋转' }}
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
import PieChart2D from '@/components/charts/PieChart2D.vue'
import { Refresh, VideoPlay, VideoPause } from '@element-plus/icons-vue'

export interface PieChart3DData {
  name: string
  value: number
  color?: string
}

interface Props {
  data: PieChart3DData[]
  width?: string
  height?: string
  depth?: number // 3D深度
  innerRadius?: number // 内半径（环形图）
  outerRadius?: number // 外半径
  showLabels?: boolean
  showLegend?: boolean
  animation?: boolean
  autoRotate?: boolean
  rotateSpeed?: number
  showControls?: boolean
  showFallback?: boolean
  showFallbackNotice?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '500px',
  depth: 1,
  innerRadius: 0,
  outerRadius: 3,
  showLabels: true,
  showLegend: true,
  animation: true,
  autoRotate: false,
  rotateSpeed: 1,
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
let pieGroup: THREE.Group
let animationId: number

// 计算图表数据
const chartData = computed(() => {
  const total = props.data.reduce((sum, item) => sum + item.value, 0)
  return props.data.map(item => ({
    name: item.name,
    value: item.value,
    percentage: ((item.value / total) * 100).toFixed(1),
    color: item.color || getDefaultColor()
  }))
})

// 获取默认颜色
const colorPalette = [
  '#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe',
  '#43e97b', '#38f9d7', '#f5576c', '#f093fb', '#4facfe'
]
let colorIndex = 0
const getDefaultColor = () => {
  const color = colorPalette[colorIndex % colorPalette.length]
  colorIndex++
  return color
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
  camera.position.set(0, 0, 12)
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

  // 创建饼图
  createPie()

  // 添加轨道控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = props.autoRotate
  controls.autoRotateSpeed = props.rotateSpeed || 1
  controls.minDistance = 8
  controls.maxDistance = 25
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

// 创建3D饼图
const createPie = () => {
  if (pieGroup) {
    scene.remove(pieGroup)
  }

  pieGroup = new THREE.Group()
  const data = chartData.value
  const total = data.reduce((sum, item) => sum + item.value, 0)
  
  let currentAngle = 0

  data.forEach((item, index) => {
    const angle = (item.value / total) * Math.PI * 2
    const color = new THREE.Color(item.color || colorPalette[index % colorPalette.length])

    // 创建扇形几何体
    const shape = new THREE.Shape()
    shape.moveTo(0, 0)
    
    // 外圆弧
    for (let i = 0; i <= 32; i++) {
      const a = currentAngle + (angle * i) / 32
      const x = Math.cos(a) * props.outerRadius
      const y = Math.sin(a) * props.outerRadius
      shape.lineTo(x, y)
    }
    
    // 如果有内半径，添加内圆弧
    if (props.innerRadius > 0) {
      for (let i = 32; i >= 0; i--) {
        const a = currentAngle + (angle * i) / 32
        const x = Math.cos(a) * props.innerRadius
        const y = Math.sin(a) * props.innerRadius
        shape.lineTo(x, y)
      }
    }
    
    shape.lineTo(0, 0)

    // 拉伸成3D
    const geometry = new THREE.ExtrudeGeometry(shape, {
      depth: props.depth,
      bevelEnabled: false
    })

    // 创建材质
    const material = new THREE.MeshStandardMaterial({
      color: color,
      metalness: 0.3,
      roughness: 0.4,
      emissive: color,
      emissiveIntensity: 0.1
    })

    const segment = new THREE.Mesh(geometry, material)
    segment.castShadow = true
    segment.receiveShadow = true
    segment.userData = { name: item.name, value: item.value, percentage: item.percentage }

    // 添加点击事件
    segment.onClick = () => {
      console.log('Clicked:', item.name, item.value)
    }

    pieGroup.add(segment)
    currentAngle += angle
  })

  scene.add(pieGroup)

  // 添加动画
  if (props.animation) {
    pieGroup.children.forEach((segment, index) => {
      segment.scale.z = 0
      segment.position.z = -props.depth / 2
      
      setTimeout(() => {
        animateSegment(segment, props.depth, 500)
      }, index * 50)
    })
  }
}

// 扇形动画
const animateSegment = (segment: THREE.Mesh, targetDepth: number, duration: number) => {
  const startDepth = segment.scale.z
  const startTime = Date.now()

  const animate = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    const easeOutCubic = 1 - Math.pow(1 - progress, 3)
    const currentDepth = startDepth + (targetDepth - startDepth) * easeOutCubic
    
    segment.scale.z = currentDepth
    segment.position.z = (currentDepth - 1) * props.depth / 2

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

// 重置相机
const resetCamera = () => {
  if (camera && controls) {
    camera.position.set(0, 0, 12)
    camera.lookAt(0, 0, 0)
    controls.reset()
  }
}

// 切换自动旋转
const autoRotateState = ref(props.autoRotate)
const toggleAutoRotate = () => {
  autoRotateState.value = !autoRotateState.value
  if (controls) {
    controls.autoRotate = autoRotateState.value
  }
}

// 监听数据变化
watch(() => props.data, () => {
  colorIndex = 0 // 重置颜色索引
  if (scene && webglSupported.value) {
    createPie()
  }
}, { deep: true })

onMounted(() => {
  if (webglSupported.value) {
    initScene()
  }
})
</script>

<style scoped lang="scss">
.pie-chart-3d-wrapper {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.pie-chart-3d-container {
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
  .pie-chart-3d-wrapper {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  }
}
</style>

