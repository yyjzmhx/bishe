import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import '@/assets/styles/main.scss'

// 在应用启动前初始化主题
const initTheme = () => {
  const savedTheme = localStorage.getItem('theme')
  // 如果没有保存的主题，默认使用浅色模式
  const theme = savedTheme || 'light'
  document.documentElement.setAttribute('data-theme', theme)
  document.documentElement.style.colorScheme = theme
  console.log('🎨 主题初始化:', theme)
}

// 立即初始化主题（在任何组件渲染之前）
initTheme()

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.use(router)
app.use(pinia)

app.mount('#app')

