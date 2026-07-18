<template>
  <div class="scene-3d-container" :style="{ width: width, height: height }">
    <TresCanvas
      :shadows="shadows"
      :alpha="alpha"
      :background="backgroundColor"
      @created="onCreated"
    >
      <!-- 相机 -->
      <TresPerspectiveCamera
        :position="cameraPosition"
        :fov="fov"
        :near="near"
        :far="far"
        ref="cameraRef"
      />
      
      <!-- 光照系统 -->
      <TresAmbientLight :intensity="ambientIntensity" />
      <TresDirectionalLight
        :position="[10, 10, 5]"
        :intensity="directionalIntensity"
        :cast-shadow="shadows"
      />
      <TresPointLight
        :position="[-10, -10, -5]"
        :intensity="pointIntensity"
        color="#667eea"
      />
      
      <!-- 轨道控制器 -->
      <OrbitControls
        :enable-damping="true"
        :damping-factor="0.05"
        :auto-rotate="autoRotate"
        :auto-rotate-speed="autoRotateSpeed"
        :enable-zoom="enableZoom"
        :enable-pan="enablePan"
        :min-distance="minDistance"
        :max-distance="maxDistance"
      />
      
      <!-- 插槽：子组件可以在这里添加3D对象 -->
      <slot />
    </TresCanvas>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { TresCanvas } from '@tresjs/core'
import { OrbitControls } from '@tresjs/cientos'

interface Props {
  width?: string
  height?: string
  shadows?: boolean
  alpha?: boolean
  backgroundColor?: string
  cameraPosition?: [number, number, number]
  fov?: number
  near?: number
  far?: number
  ambientIntensity?: number
  directionalIntensity?: number
  pointIntensity?: number
  autoRotate?: boolean
  autoRotateSpeed?: number
  enableZoom?: boolean
  enablePan?: boolean
  minDistance?: number
  maxDistance?: number
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '500px',
  shadows: true,
  alpha: true,
  backgroundColor: 'transparent',
  cameraPosition: () => [0, 0, 5],
  fov: 75,
  near: 0.1,
  far: 1000,
  ambientIntensity: 0.5,
  directionalIntensity: 1,
  pointIntensity: 0.5,
  autoRotate: false,
  autoRotateSpeed: 2,
  enableZoom: true,
  enablePan: true,
  minDistance: 2,
  maxDistance: 20
})

const cameraRef = ref()
const sceneRef = ref()

const onCreated = ({ scene, camera, renderer }: any) => {
  sceneRef.value = scene
  // 可以在这里添加全局场景设置
  if (renderer) {
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.shadowMap.enabled = props.shadows
  }
}

defineExpose({
  camera: cameraRef,
  scene: sceneRef
})
</script>

<style scoped lang="scss">
.scene-3d-container {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: transparent;
  
  :deep(canvas) {
    display: block;
    outline: none;
  }
}
</style>

