/**
 * WebGL 支持检测组合式函数
 * 在组件中使用，提供响应式的 WebGL 支持状态
 */

import { ref, onMounted } from 'vue'
import { detectWebGL, isWebGLSupported, type WebGLInfo } from '@/utils/webgl-detector'

export function useWebGL() {
  const webglInfo = ref<WebGLInfo>({
    supported: false,
    version: null
  })
  
  const isSupported = ref(false)
  const isLoading = ref(true)
  const performanceHint = ref('')

  const checkWebGL = () => {
    isLoading.value = true
    try {
      webglInfo.value = detectWebGL()
      isSupported.value = isWebGLSupported()
      
      // 生成性能提示
      if (!isSupported.value) {
        performanceHint.value = '您的浏览器不支持 WebGL，3D 可视化功能将使用 2D 降级方案。'
      } else if (webglInfo.value.version === 'webgl1') {
        performanceHint.value = '您的浏览器支持 WebGL 1.0，建议升级到支持 WebGL 2.0 的浏览器以获得更好的性能。'
      } else {
        performanceHint.value = 'WebGL 支持正常，可以正常使用 3D 可视化功能。'
      }
    } catch (error) {
      console.error('WebGL 检测失败:', error)
      isSupported.value = false
      performanceHint.value = 'WebGL 检测失败，将使用 2D 降级方案。'
    } finally {
      isLoading.value = false
    }
  }

  onMounted(() => {
    checkWebGL()
  })

  return {
    webglInfo,
    isSupported,
    isLoading,
    performanceHint,
    checkWebGL
  }
}

