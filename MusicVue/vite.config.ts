import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  css: {
    preprocessorOptions: {
      scss: {
        // 抑制 Sass 弃用警告
        // 注意：这些警告不影响功能，只是提示未来版本可能会移除这些特性
        // 如果需要完全消除警告，可以考虑迁移到 @use 语法
        silenceDeprecations: ['import', 'legacy-js-api'],
        quietDeps: true
      }
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'echarts': ['echarts', 'vue-echarts'],
          'gsap': ['gsap']
        }
      }
    }
  }
})

