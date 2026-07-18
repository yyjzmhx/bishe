<template>
  <div class="admin-login-page">
    <div class="login-background">
      <div class="grid-overlay"></div>
      <div class="geometric-shapes">
        <div v-for="i in 20" :key="i" class="shape" :style="getShapeStyle(i)"></div>
      </div>
    </div>
    
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <div class="logo-section">
            <div class="logo-icon">
              <el-icon :size="48"><Setting /></el-icon>
            </div>
            <h1 class="logo-text">管理员登录</h1>
            <p class="welcome-text">Admin Login</p>
          </div>
        </div>
        
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="管理员账号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <div class="form-actions">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" underline="never" @click="handleForgotPassword">忘记密码？</el-link>
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
          
          <div class="divider">
            <span>或</span>
          </div>
          
          <div class="switch-login">
            <span>普通用户？</span>
            <el-link type="primary" @click="$router.push('/login')">前往用户登录</el-link>
          </div>
        </el-form>
      </div>
      
      <div class="footer-info">
        <p>© 2024 MusicAI 管理系统</p>
        <p class="version">Version 1.0.0</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Setting } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import type { LoginRequest } from '@/types/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive<LoginRequest>({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const getShapeStyle = (index: number) => {
  const shapes = ['circle', 'square', 'triangle']
  const shape = shapes[index % 3]
  const size = Math.random() * 60 + 20
  const left = Math.random() * 100
  const top = Math.random() * 100
  const rotation = Math.random() * 360
  const opacity = Math.random() * 0.3 + 0.1
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    transform: `rotate(${rotation}deg)`,
    opacity: opacity,
    borderRadius: shape === 'circle' ? '50%' : shape === 'triangle' ? '0' : '4px',
    clipPath: shape === 'triangle' ? 'polygon(50% 0%, 0% 100%, 100% 100%)' : 'none'
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await login(loginForm)
        
        // 检查是否为管理员
        if (response.role !== 'ADMIN') {
          ElMessage.warning('此账号不是管理员账号，请使用管理员账号登录')
          return
        }
        
        userStore.setUserToken(response.token, response.role)
        userStore.setUserInfo({
          id: response.userId,
          username: response.username,
          role: response.role as 'USER' | 'ADMIN',
          nickname: response.nickname,
          avatar: response.avatar,
          status: 1
        })
        
        ElMessage.success('登录成功')
        router.push('/admin/dashboard')
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleForgotPassword = () => {
  ElMessage.info('请联系系统管理员重置密码')
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.admin-login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  @include flex-center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-background {
  position: absolute;
  inset: 0;
  z-index: 0;
  
  .grid-overlay {
    position: absolute;
    inset: 0;
    background-image: 
      linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
    background-size: 50px 50px;
    opacity: 0.5;
  }
  
  .geometric-shapes {
    position: absolute;
    inset: 0;
    
    .shape {
      position: absolute;
      background: rgba(255, 255, 255, 0.1);
      animation: float-shape 20s linear infinite;
      
      @keyframes float-shape {
        0% {
          transform: translateY(100vh) rotate(0deg);
        }
        100% {
          transform: translateY(-100vh) rotate(360deg);
        }
      }
    }
  }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 24px;
}

.login-card {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 32px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
  
  .card-header {
    text-align: center;
    margin-bottom: 28px;
    
    .logo-section {
      .logo-icon {
        width: 64px;
        height: 64px;
        margin: 0 auto 16px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 16px;
        @include flex-center;
        color: white;
        box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
      }
      
      .logo-text {
        font-size: 24px;
        font-weight: 700;
        color: #1a202c;
        margin-bottom: 6px;
        font-family: 'Roboto', sans-serif;
      }
      
      .welcome-text {
        font-size: 12px;
        color: #718096;
        letter-spacing: 1.5px;
        text-transform: uppercase;
      }
    }
  }
  
  .login-form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }
    
    .form-actions {
      display: flex;
      justify-content: space-between;
      width: 100%;
      margin-bottom: 4px;
    }
    
    .login-button {
      width: 100%;
      height: 48px;
      font-size: 16px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 10px;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 10px 20px rgba(102, 126, 234, 0.4);
      }
    }
    
    .divider {
      position: relative;
      text-align: center;
      margin: 18px 0 14px;
      
      &::before,
      &::after {
        content: '';
        position: absolute;
        top: 50%;
        width: 45%;
        height: 1px;
        background: #e2e8f0;
      }
      
      &::before {
        left: 0;
      }
      
      &::after {
        right: 0;
      }
      
      span {
        position: relative;
        padding: 0 16px;
        background: rgba(255, 255, 255, 0.98);
        color: #718096;
        font-size: 14px;
      }
    }
    
    .switch-login {
      text-align: center;
      color: #718096;
      font-size: 14px;
      padding-top: 8px;
      
      .el-link {
        margin-left: 8px;
        font-weight: 600;
      }
    }
  }
}

.footer-info {
  text-align: center;
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  
  .version {
    margin-top: 4px;
    opacity: 0.6;
  }
}

[data-theme="dark"] {
  .login-card {
    background: rgba(30, 41, 59, 0.98);
    border-color: rgba(255, 255, 255, 0.1);
    
    .card-header .logo-section {
      .logo-text {
        color: #f1f5f9;
      }
      
      .welcome-text {
        color: #94a3b8;
      }
    }
  }
}
</style>

