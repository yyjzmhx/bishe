<template>
  <div class="login-page">
    <div class="login-background">
      <div class="gradient-overlay"></div>
      <div class="animated-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
      <div class="particles">
        <div v-for="i in 50" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>
    </div>
    
    <div class="login-container">
      <div class="login-card glass-card">
        <div class="card-header">
          <div class="logo-wrapper">
            <div class="logo-icon">
              <el-icon :size="40"><VideoPlay /></el-icon>
            </div>
            <h1 class="logo-text text-gradient">MusicAI</h1>
          </div>
          <p class="welcome-text">欢迎回来</p>
          <p class="subtitle-text">登录您的账户以继续</p>
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
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
              class="custom-input"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
              class="custom-input"
            />
          </el-form-item>
          
          <el-form-item>
            <div class="form-actions">
              <el-checkbox v-model="rememberMe" class="remember-checkbox">
                记住我
              </el-checkbox>
              <el-link type="primary" :underline="false" class="forgot-link" @click="$router.push('/forgot-password')">
                忘记密码？
              </el-link>
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
              <span v-if="!loading">登录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
          
          <div class="divider">
            <span class="divider-text">或</span>
          </div>
          
          <div class="social-login">
            <el-button circle size="large" class="social-button wechat" title="微信登录">
              <el-icon><ChatDotRound /></el-icon>
            </el-button>
            <el-button circle size="large" class="social-button qq" title="QQ登录">
              <el-icon><Message /></el-icon>
            </el-button>
          </div>
          
          <div class="register-link">
            <span class="register-text">还没有账号？</span>
            <el-link type="primary" :underline="false" @click="$router.push('/register')" class="register-btn">
              立即注册
            </el-link>
          </div>
          
          <div class="admin-link">
            <el-link type="info" :underline="false" @click="$router.push('/admin/login')" class="admin-btn">
              <el-icon><Setting /></el-icon>
              <span>管理员登录</span>
            </el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, ChatDotRound, Message, Setting, VideoPlay } from '@element-plus/icons-vue'
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
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const getParticleStyle = (index: number) => {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const animationDelay = Math.random() * 5
  const animationDuration = Math.random() * 3 + 2
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${animationDelay}s`,
    animationDuration: `${animationDuration}s`
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await login(loginForm)
        
        // 检查是否为管理员，如果是则提示使用管理员登录页
        if (response.role === 'ADMIN') {
          ElMessage.warning('管理员账号请使用管理员登录页面')
          router.push('/admin/login')
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
        router.push('/')
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  @include flex-center;
}

.login-background {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #4facfe 75%, #00f2fe 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
  z-index: 0;
  
  .gradient-overlay {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 20% 50%, rgba(124, 58, 237, 0.4), transparent 50%),
                radial-gradient(circle at 80% 80%, rgba(59, 130, 246, 0.4), transparent 50%),
                radial-gradient(circle at 50% 20%, rgba(236, 72, 153, 0.3), transparent 50%);
  }
  
  .animated-shapes {
    position: absolute;
    inset: 0;
    overflow: hidden;
    
    .shape {
      position: absolute;
      border-radius: 50%;
      filter: blur(60px);
      opacity: 0.3;
      animation: floatShape 20s ease-in-out infinite;
      
      &.shape-1 {
        width: 300px;
        height: 300px;
        background: rgba(124, 58, 237, 0.5);
        top: 10%;
        left: 10%;
        animation-delay: 0s;
      }
      
      &.shape-2 {
        width: 250px;
        height: 250px;
        background: rgba(59, 130, 246, 0.5);
        top: 60%;
        right: 10%;
        animation-delay: 5s;
      }
      
      &.shape-3 {
        width: 200px;
        height: 200px;
        background: rgba(236, 72, 153, 0.5);
        bottom: 10%;
        left: 50%;
        animation-delay: 10s;
      }
    }
  }
  
  .particles {
    position: absolute;
    inset: 0;
    
    .particle {
      position: absolute;
      background: rgba(255, 255, 255, 0.8);
      border-radius: 50%;
      animation: float linear infinite;
      box-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
    }
  }
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes floatShape {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

@keyframes float {
  0% {
    transform: translateY(100vh) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) rotate(360deg);
    opacity: 0;
  }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
  padding: 24px;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card {
  backdrop-filter: blur(30px) saturate(180%);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border-radius: 20px;
  padding: 32px 36px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15),
                0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  }
  
  .card-header {
    text-align: center;
    margin-bottom: 28px;
    
    .logo-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      margin-bottom: 12px;
      
      .logo-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        @include flex-center;
        color: white;
        box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
        animation: pulse 2s ease-in-out infinite;
      }
      
      .logo-text {
        font-size: 36px;
        font-weight: 800;
        font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
        letter-spacing: -1px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
    }
    
    .welcome-text {
      font-size: 20px;
      font-weight: 600;
      color: #1a202c;
      margin-bottom: 4px;
    }
    
    .subtitle-text {
      font-size: 13px;
      color: #718096;
      font-weight: 400;
    }
  }
  
  .login-form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }
    
    .custom-input {
      :deep(.el-input__wrapper) {
        border-radius: 12px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        transition: all 0.3s ease;
        padding: 12px 16px;
        
        &:hover {
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
        }
        
        &.is-focus {
          box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1),
                      0 4px 12px rgba(102, 126, 234, 0.3);
        }
      }
      
      :deep(.el-input__inner) {
        font-size: 15px;
      }
    }
    
    .form-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
      margin-bottom: 8px;
      
      .remember-checkbox {
        :deep(.el-checkbox__label) {
          font-size: 14px;
          color: #4a5568;
        }
      }
      
      .forgot-link {
        font-size: 14px;
        font-weight: 500;
        transition: all 0.2s ease;
        
        &:hover {
          transform: translateX(2px);
        }
      }
    }
    
    .login-button {
      width: 100%;
      height: 52px;
      font-size: 16px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 12px;
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
      
      &::before {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        width: 0;
        height: 0;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.3);
        transform: translate(-50%, -50%);
        transition: width 0.6s, height 0.6s;
      }
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
        
        &::before {
          width: 300px;
          height: 300px;
        }
      }
      
      &:active {
        transform: translateY(0);
      }
    }
    
    .divider {
      position: relative;
      text-align: center;
      margin: 20px 0 16px;
      
      &::before,
      &::after {
        content: '';
        position: absolute;
        top: 50%;
        width: 42%;
        height: 1px;
        background: linear-gradient(to right, transparent, #e2e8f0, transparent);
      }
      
      &::before {
        left: 0;
      }
      
      &::after {
        right: 0;
      }
      
      .divider-text {
        position: relative;
        padding: 0 16px;
        background: rgba(255, 255, 255, 0.98);
        color: #a0aec0;
        font-size: 13px;
        font-weight: 500;
      }
    }
    
    .social-login {
      display: flex;
      justify-content: center;
      gap: 16px;
      margin-bottom: 20px;
      
      .social-button {
        width: 52px;
        height: 52px;
        border: 2px solid #e2e8f0;
        background: white;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        color: #4a5568;
        
        &:hover {
          transform: translateY(-4px) scale(1.05);
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
        }
        
        &.wechat {
          &:hover {
            background: #07C160;
            border-color: #07C160;
            color: white;
          }
        }
        
        &.qq {
          &:hover {
            background: #12B7F5;
            border-color: #12B7F5;
            color: white;
          }
        }
      }
    }
    
    .register-link {
      text-align: center;
      padding: 12px 0;
      
      .register-text {
        color: #718096;
        font-size: 14px;
        margin-right: 8px;
      }
      
      .register-btn {
        font-size: 14px;
        font-weight: 600;
        transition: all 0.2s ease;
        
        &:hover {
          transform: translateX(2px);
        }
      }
    }
    
    .admin-link {
      text-align: center;
      padding-top: 12px;
      border-top: 1px solid #e2e8f0;
      margin-top: 8px;
      
      .admin-btn {
        font-size: 13px;
        color: #718096;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-weight: 500;
        transition: all 0.2s ease;
        
        &:hover {
          color: #667eea;
          transform: translateY(-1px);
        }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 6px 24px rgba(102, 126, 234, 0.6);
  }
}

[data-theme="dark"] {
  .login-card {
    background: rgba(26, 32, 44, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3),
                0 0 0 1px rgba(255, 255, 255, 0.1) inset;
    
    .card-header {
      .welcome-text {
        color: #f7fafc;
      }
      
      .subtitle-text {
        color: #a0aec0;
      }
    }
    
    .login-form {
      .custom-input {
        :deep(.el-input__wrapper) {
          background: rgba(45, 55, 72, 0.8);
          border-color: rgba(255, 255, 255, 0.1);
        }
      }
      
      .form-actions {
        .remember-checkbox {
          :deep(.el-checkbox__label) {
            color: #cbd5e0;
          }
        }
      }
      
      .divider {
        &::before,
        &::after {
          background: linear-gradient(to right, transparent, rgba(255, 255, 255, 0.1), transparent);
        }
        
        .divider-text {
          background: rgba(26, 32, 44, 0.95);
          color: #718096;
        }
      }
      
      .social-login {
        .social-button {
          background: rgba(45, 55, 72, 0.8);
          border-color: rgba(255, 255, 255, 0.1);
          color: #cbd5e0;
        }
      }
      
      .register-link {
        .register-text {
          color: #a0aec0;
        }
      }
      
      .admin-link {
        border-top-color: rgba(255, 255, 255, 0.1);
      }
    }
  }
}

// 响应式设计
@media (max-width: 640px) {
  .login-container {
    max-width: 100%;
    padding: 16px;
  }
  
  .login-card {
    padding: 32px 24px;
    border-radius: 20px;
    
    .card-header {
      margin-bottom: 32px;
      
      .logo-wrapper {
        .logo-text {
          font-size: 36px;
        }
      }
      
      .welcome-text {
        font-size: 20px;
      }
    }
  }
}
</style>


