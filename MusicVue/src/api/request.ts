import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error('未登录或登录已过期')
      removeToken()
      router.push('/login')
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足')
    } else if (error.response?.status === 404) {
      console.error('404错误 - 请求路径:', error.config?.url)
      console.error('404错误 - 完整URL:', error.config?.baseURL + error.config?.url)
      ElMessage.error('请求的资源不存在，请检查接口路径')
    } else {
      const errorMsg = error.response?.data?.message || error.message || '网络错误'
      ElMessage.error(errorMsg)
    }
    return Promise.reject(error)
  }
)

export default request

