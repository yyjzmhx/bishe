<template>
  <div class="feature-3d-wrapper" :style="{ width: width, height: height }">
    <div ref="containerRef" class="feature-3d-container"></div>
    <div v-if="showLabels" class="feature-labels">
      <div
        v-for="(label, index) in labels"
        :key="index"
        class="feature-label"
        :style="getLabelStyle(index)"
      >
        <span class="label-name">{{ label }}</span>
        <span class="label-value">{{ (features[index] * 100).toFixed(0) }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

interface Props {
  features: number[] // 特征值数组，范围0-1
  labels?: string[] // 特征标签
  width?: string
  height?: string
  autoRotate?: boolean
  rotateSpeed?: number
  showPoints?: boolean
  showLines?: boolean
  sphereColor?: string
  pointColor?: string
  lineColor?: string
  pointSize?: number
  showLabels?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  labels: () => ['节奏', '旋律', '和声', '音色', '动态', '情感', '风格', '能量'],
  width: '100%',
  height: '500px',
  autoRotate: true,
  rotateSpeed: 1,
  showPoints: true,
  showLines: true,
  sphereColor: '#667eea',
  pointColor: '#f093fb',
  lineColor: '#4facfe',
  pointSize: 0.15,
  showLabels: true
})

const containerRef = ref<HTMLElement>()
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let sphereMesh: THREE.Mesh
let innerMesh: THREE.Mesh
let outerMesh: THREE.Mesh
let pointsMesh: THREE.Points
let linesMesh: THREE.LineSegments
let backgroundParticles: THREE.Points
let animationId: number
let lights: any = {}

// 计算特征点在球面上的位置
const getPointPositions = (featureCount: number) => {
  const positions: number[] = []
  
  for (let i = 0; i < featureCount; i++) {
    // 使用黄金角度螺旋分布
    const theta = Math.acos(1 - (2 * i) / featureCount)
    const phi = Math.PI * (1 + Math.sqrt(5)) * i
    
    // 根据特征值调整半径
    // 根据特征值调整半径，让点悬浮在线框球表面上方
    const radius = 2.4 + (props.features[i] || 0.5) * 0.6
    
    const x = radius * Math.sin(theta) * Math.cos(phi)
    const y = radius * Math.sin(theta) * Math.sin(phi)
    const z = radius * Math.cos(theta)
    
    positions.push(x, y, z)
  }
  
  return new Float32Array(positions)
}

// 初始化3D场景
const initScene = () => {
  if (!containerRef.value) return
  
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  
  // 创建场景
  scene = new THREE.Scene()
  scene.background = null // 透明背景
  
  // 创建相机
  camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000)
  camera.position.set(0, 0, 9) // 稍微拉远一点，容纳更大的标签范围
  
  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ 
    antialias: true,
    alpha: true 
  })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  containerRef.value.appendChild(renderer.domElement)
  
  // 添加更丰富的光照系统
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.4)
  scene.add(ambientLight)
  
  // 主光源
  const directionalLight1 = new THREE.DirectionalLight(0xffffff, 1.2)
  directionalLight1.position.set(10, 10, 5)
  scene.add(directionalLight1)
  
  // 辅助光源
  const directionalLight2 = new THREE.DirectionalLight(0x667eea, 0.6)
  directionalLight2.position.set(-10, -10, -5)
  scene.add(directionalLight2)
  
  // 彩色点光源 - 动态变化
  const pointLight1 = new THREE.PointLight(0x667eea, 1.5, 20)
  pointLight1.position.set(5, 5, 5)
  scene.add(pointLight1)
  
  const pointLight2 = new THREE.PointLight(0xf093fb, 1.5, 20)
  pointLight2.position.set(-5, -5, -5)
  scene.add(pointLight2)
  
  const pointLight3 = new THREE.PointLight(0x4facfe, 1.0, 20)
  pointLight3.position.set(0, 10, 0)
  scene.add(pointLight3)
  
  // 保存光源引用用于动画
  lights = { pointLight1, pointLight2, pointLight3 }
  
  // 创建核心球体 - 发光能量球
  const coreGeometry = new THREE.IcosahedronGeometry(1.5, 1)
  const coreMaterial = new THREE.MeshBasicMaterial({
    color: 0x764ba2,
    wireframe: true,
    transparent: true,
    opacity: 0.3
  })
  const coreMesh = new THREE.Mesh(coreGeometry, coreMaterial)
  scene.add(coreMesh)
  
  // 创建外层线框球 - 科技感网格
  const wireframeGeometry = new THREE.WireframeGeometry(new THREE.IcosahedronGeometry(2.2, 2))
  const wireframeMaterial = new THREE.LineBasicMaterial({
    color: 0x667eea,
    transparent: true,
    opacity: 0.15
  })
  const wireframeMesh = new THREE.LineSegments(wireframeGeometry, wireframeMaterial)
  scene.add(wireframeMesh)
  
  // 保存引用
  sphereMesh = coreMesh // 借用变量名用于动画
  outerMesh = wireframeMesh as any
  
  // 创建特征点 - 使用更美观的粒子系统
  if (props.showPoints) {
    const featureCount = props.features.length || props.labels.length
    const positions = getPointPositions(featureCount)
    const colors: number[] = []
    const sizes: number[] = []
    
    // 为每个点生成颜色和大小
    for (let i = 0; i < featureCount; i++) {
      const featureValue = props.features[i] || 0.5
      
      // 根据特征值生成渐变色
      const hue = (i / featureCount) * 360
      const color = new THREE.Color()
      color.setHSL(hue / 360, 0.8, 0.6)
      colors.push(color.r, color.g, color.b)
      
      // 根据特征值调整大小
      sizes.push(featureValue * 0.3 + 0.1)
    }
    
    const pointsGeometry = new THREE.BufferGeometry()
    pointsGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    pointsGeometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
    pointsGeometry.setAttribute('size', new THREE.Float32BufferAttribute(sizes, 1))
    
    // 创建自定义着色器材质，实现发光效果
    const vertexShader = `
      attribute float size;
      varying vec3 vColor;
      varying float vOpacity;
      
      void main() {
        vColor = color;
        vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
        gl_PointSize = size * (300.0 / -mvPosition.z);
        vOpacity = size * 2.0;
        gl_Position = projectionMatrix * mvPosition;
      }
    `
    
    const fragmentShader = `
      uniform float time;
      varying vec3 vColor;
      varying float vOpacity;
      
      void main() {
        float dist = distance(gl_PointCoord, vec2(0.5));
        float alpha = 1.0 - smoothstep(0.0, 0.5, dist);
        alpha *= vOpacity;
        
        // 添加光晕效果
        float glow = 1.0 - smoothstep(0.0, 0.3, dist);
        vec3 finalColor = vColor + glow * vec3(1.0, 1.0, 1.0) * 0.5;
        
        gl_FragColor = vec4(finalColor, alpha);
      }
    `
    
    const pointsMaterial = new THREE.ShaderMaterial({
      uniforms: {
        time: { value: 0 }
      },
      vertexShader,
      fragmentShader,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })
    
    pointsMesh = new THREE.Points(pointsGeometry, pointsMaterial)
    scene.add(pointsMesh)
  }
  
  // 创建连接线 - 使用渐变和发光效果
  if (props.showLines && props.showPoints) {
    const featureCount = props.features.length || props.labels.length
    const positions = getPointPositions(featureCount)
    const linePositions: number[] = []
    const lineColors: number[] = []
    
    // 连接相邻的点，并添加颜色渐变
    for (let i = 0; i < featureCount - 1; i++) {
      const startIdx = i * 3
      const endIdx = (i + 1) * 3
      
      linePositions.push(
        positions[startIdx],
        positions[startIdx + 1],
        positions[startIdx + 2],
        positions[endIdx],
        positions[endIdx + 1],
        positions[endIdx + 2]
      )
      
      // 为每条线添加渐变色
      const color1 = new THREE.Color()
      const color2 = new THREE.Color()
      color1.setHSL(i / featureCount, 0.8, 0.6)
      color2.setHSL((i + 1) / featureCount, 0.8, 0.6)
      
      lineColors.push(
        color1.r, color1.g, color1.b,
        color2.r, color2.g, color2.b
      )
    }
    
    const lineGeometry = new THREE.BufferGeometry()
    lineGeometry.setAttribute('position', new THREE.BufferAttribute(new Float32Array(linePositions), 3))
    lineGeometry.setAttribute('color', new THREE.Float32BufferAttribute(lineColors, 3))
    
    const lineMaterial = new THREE.LineBasicMaterial({
      vertexColors: true,
      transparent: true,
      opacity: 0.4,
      linewidth: 2
    })
    
    linesMesh = new THREE.LineSegments(lineGeometry, lineMaterial)
    scene.add(linesMesh)
  }
  
  // 添加背景粒子效果
  const createBackgroundParticles = () => {
    const particleCount = 200
    const particles = new THREE.BufferGeometry()
    const positions = new Float32Array(particleCount * 3)
    
    for (let i = 0; i < particleCount * 3; i += 3) {
      positions[i] = (Math.random() - 0.5) * 20
      positions[i + 1] = (Math.random() - 0.5) * 20
      positions[i + 2] = (Math.random() - 0.5) * 20
    }
    
    particles.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    
    const particleMaterial = new THREE.PointsMaterial({
      color: 0xffffff,
      size: 0.05,
      transparent: true,
      opacity: 0.3
    })
    
    const particleSystem = new THREE.Points(particles, particleMaterial)
    scene.add(particleSystem)
    return particleSystem
  }
  
  backgroundParticles = createBackgroundParticles()
  
  // 添加轨道控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = props.autoRotate
  controls.autoRotateSpeed = props.rotateSpeed
  controls.minDistance = 5
  controls.maxDistance = 15
  controls.enableZoom = true
  controls.enablePan = true
  
  // 动画循环
  const animate = () => {
    animationId = requestAnimationFrame(animate)
    const time = Date.now() * 0.001
    
    // 球体动画
    if (sphereMesh) {
      sphereMesh.rotation.y += 0.002
      sphereMesh.rotation.x += 0.001
    }
    
    // 外层线框反向旋转
    if (outerMesh) {
      outerMesh.rotation.y -= 0.001
      outerMesh.rotation.z += 0.001
    }
    
    // 点光源动态移动
    if (lights.pointLight1) {
      lights.pointLight1.position.x = Math.sin(time) * 5
      lights.pointLight1.position.y = Math.cos(time) * 5
      lights.pointLight1.intensity = 1.5 + Math.sin(time * 2) * 0.5
    }
    if (lights.pointLight2) {
      lights.pointLight2.position.x = Math.cos(time) * 5
      lights.pointLight2.position.y = Math.sin(time) * 5
      lights.pointLight2.intensity = 1.5 + Math.cos(time * 2) * 0.5
    }
    if (lights.pointLight3) {
      lights.pointLight3.position.z = Math.sin(time * 0.5) * 5
      lights.pointLight3.intensity = 1.0 + Math.sin(time) * 0.3
    }
    
    // 更新粒子着色器时间
    if (pointsMesh && pointsMesh.material instanceof THREE.ShaderMaterial) {
      pointsMesh.material.uniforms.time.value = time
    }
    
    // 背景粒子旋转
    if (backgroundParticles) {
      backgroundParticles.rotation.y += 0.0005
    }
    
    controls.update()
    renderer.render(scene, camera)
  }
  
  animate()
  
  // 窗口大小调整
  const handleResize = () => {
    if (!containerRef.value) return
    const width = containerRef.value.clientWidth
    const height = containerRef.value.clientHeight
    
    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setSize(width, height)
  }
  
  window.addEventListener('resize', handleResize)
  
  // 清理函数
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

// 标签位置计算（用于CSS定位）
const getLabelStyle = (index: number) => {
  const featureCount = props.features.length || props.labels.length
  const theta = Math.acos(1 - (2 * index) / featureCount)
  const phi = Math.PI * (1 + Math.sqrt(5)) * index
  
  // 转换为屏幕坐标（简化版本）
  const radius = 3.8 // 增加半径，让标签离球体更远
  const x = radius * Math.sin(theta) * Math.cos(phi)
  const y = radius * Math.sin(theta) * Math.sin(phi)
  
  // 转换为百分比位置（50%为中心）
  // 调整缩放系数，确保标签在可视区域内但尽量靠外
  const scale = 25 
  const left = 50 + (x / 5) * scale
  const top = 50 + (y / 5) * scale
  
  return {
    left: `${Math.max(5, Math.min(95, left))}%`,
    top: `${Math.max(5, Math.min(95, top))}%`
  }
}

onMounted(() => {
  initScene()
})

// 监听特征值变化，更新点位置
watch(() => props.features, () => {
  if (pointsMesh && props.showPoints) {
    const featureCount = props.features.length || props.labels.length
    const positions = getPointPositions(featureCount)
    const geometry = pointsMesh.geometry as THREE.BufferGeometry
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    geometry.attributes.position.needsUpdate = true
  }
}, { deep: true })
</script>

<style scoped lang="scss">
.feature-3d-wrapper {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: 
    radial-gradient(circle at 20% 50%, rgba(102, 126, 234, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(118, 75, 162, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 20%, rgba(240, 147, 251, 0.08) 0%, transparent 50%),
    linear-gradient(135deg, rgba(102, 126, 234, 0.03) 0%, rgba(118, 75, 162, 0.03) 100%);
  backdrop-filter: blur(10px);
}

.feature-3d-container {
  width: 100%;
  height: 100%;
  
  canvas {
    display: block;
    outline: none;
  }
}

.feature-labels {
  position: absolute;
  inset: 0;
  pointer-events: none;
  
    .feature-label {
    position: absolute;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 6px 10px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(4px);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 6px;
    font-size: 12px;
    color: #1a202c;
    white-space: nowrap;
    pointer-events: auto;
    transition: all 0.3s ease;
    z-index: 10;
    
    // 连接线效果
    &::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 0;
      height: 0;
      z-index: -1;
      // 这里只是占位，实际连接线很难纯用CSS做，主要靠位置偏移
    }
    
    &:hover {
      transform: translate(-50%, -50%) scale(1.1);
      background: rgba(255, 255, 255, 0.9);
      box-shadow: 0 0 15px rgba(102, 126, 234, 0.4);
      border-color: #667eea;
      z-index: 20;
    }
    
    .label-name {
      font-weight: 600;
    }
    
    .label-value {
      font-size: 11px;
      color: #667eea;
      font-weight: 700;
    }
  }
}

[data-theme="dark"] {
  .feature-labels .feature-label {
    background: rgba(30, 41, 59, 0.9);
    color: #f1f5f9;
    
    &:hover {
      background: rgba(30, 41, 59, 1);
    }
  }
}
</style>
