/**
 * WebGL 支持检测工具
 * 用于检测浏览器是否支持 WebGL，并提供降级方案
 */

export interface WebGLInfo {
  supported: boolean
  version: 'webgl1' | 'webgl2' | null
  renderer?: string
  vendor?: string
  maxTextureSize?: number
  maxVertexAttribs?: number
  maxViewportDims?: [number, number]
  shaderPrecision?: {
    vertex: { float: number; int: number }
    fragment: { float: number; int: number }
  }
}

/**
 * 检测 WebGL 支持情况
 */
export function detectWebGL(): WebGLInfo {
  const info: WebGLInfo = {
    supported: false,
    version: null
  }

  // 检测 WebGL2
  const canvas = document.createElement('canvas')
  let gl: WebGL2RenderingContext | WebGLRenderingContext | null = null

  try {
    gl = canvas.getContext('webgl2') as WebGL2RenderingContext
    if (gl) {
      info.supported = true
      info.version = 'webgl2'
    }
  } catch (e) {
    // WebGL2 不支持，尝试 WebGL1
  }

  // 如果 WebGL2 不支持，检测 WebGL1
  if (!gl) {
    try {
      gl = canvas.getContext('webgl') as WebGLRenderingContext || 
           canvas.getContext('experimental-webgl') as WebGLRenderingContext
      if (gl) {
        info.supported = true
        info.version = 'webgl1'
      }
    } catch (e) {
      // WebGL 不支持
    }
  }

  // 获取详细信息
  if (gl && info.supported) {
    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info')
    if (debugInfo) {
      info.renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || undefined
      info.vendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) || undefined
    }

    info.maxTextureSize = gl.getParameter(gl.MAX_TEXTURE_SIZE)
    info.maxVertexAttribs = gl.getParameter(gl.MAX_VERTEX_ATTRIBS)
    info.maxViewportDims = gl.getParameter(gl.MAX_VIEWPORT_DIMS)

    // 获取着色器精度
    const vertexShader = gl.createShader(gl.VERTEX_SHADER)
    const fragmentShader = gl.createShader(gl.FRAGMENT_SHADER)
    
    if (vertexShader && fragmentShader) {
      const vertexPrecision = gl.getShaderPrecisionFormat(gl.VERTEX_SHADER, gl.HIGH_FLOAT)
      const fragmentPrecision = gl.getShaderPrecisionFormat(gl.FRAGMENT_SHADER, gl.HIGH_FLOAT)
      
      if (vertexPrecision && fragmentPrecision) {
        info.shaderPrecision = {
          vertex: {
            float: vertexPrecision.precision,
            int: gl.getShaderPrecisionFormat(gl.VERTEX_SHADER, gl.HIGH_INT)?.precision || 0
          },
          fragment: {
            float: fragmentPrecision.precision,
            int: gl.getShaderPrecisionFormat(gl.FRAGMENT_SHADER, gl.HIGH_INT)?.precision || 0
          }
        }
      }
    }
  }

  return info
}

/**
 * 检查是否支持 WebGL
 */
export function isWebGLSupported(): boolean {
  return detectWebGL().supported
}

/**
 * 检查是否支持 WebGL2
 */
export function isWebGL2Supported(): boolean {
  const info = detectWebGL()
  return info.supported && info.version === 'webgl2'
}

/**
 * 获取 WebGL 上下文（如果支持）
 */
export function getWebGLContext(
  canvas: HTMLCanvasElement,
  options?: WebGLContextAttributes
): WebGL2RenderingContext | WebGLRenderingContext | null {
  // 优先尝试 WebGL2
  let gl = canvas.getContext('webgl2', options) as WebGL2RenderingContext | null
  
  if (!gl) {
    // 降级到 WebGL1
    gl = (canvas.getContext('webgl', options) || 
          canvas.getContext('experimental-webgl', options)) as WebGLRenderingContext | null
  }
  
  return gl
}

/**
 * 检查 WebGL 上下文是否有效
 */
export function isValidWebGLContext(gl: WebGLRenderingContext | WebGL2RenderingContext | null): boolean {
  if (!gl) return false
  
  try {
    // 尝试获取一个简单的参数来验证上下文是否有效
    const test = gl.getParameter(gl.VERSION)
    return !!test
  } catch (e) {
    return false
  }
}

/**
 * 获取性能提示信息
 */
export function getPerformanceHint(): string {
  const info = detectWebGL()
  
  if (!info.supported) {
    return '您的浏览器不支持 WebGL，3D 可视化功能将使用 2D 降级方案。'
  }
  
  if (info.version === 'webgl1') {
    return '您的浏览器支持 WebGL 1.0，建议升级到支持 WebGL 2.0 的浏览器以获得更好的性能。'
  }
  
  if (info.maxTextureSize && info.maxTextureSize < 4096) {
    return '您的设备纹理大小限制较低，可能影响 3D 渲染性能。'
  }
  
  return 'WebGL 支持正常，可以正常使用 3D 可视化功能。'
}

