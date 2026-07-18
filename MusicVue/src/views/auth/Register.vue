<template>
  <div class="register-page">
    <div class="register-background">
      <div class="gradient-overlay"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>
    
    <div class="register-container">
      <div class="register-card">
        <div class="card-header">
          <div class="logo-wrapper">
            <h1 class="logo-text">MusicAI</h1>
            <div class="logo-subtitle">个性化音乐推荐系统</div>
          </div>
          <p class="welcome-text">创建新账号</p>
        </div>
        
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          @keyup.enter="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="用户名（3-20个字符）"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="密码（至少6位）"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="nickname">
            <el-input
              v-model="registerForm.nickname"
              placeholder="昵称（可选）"
              size="large"
              :prefix-icon="UserFilled"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="邮箱（必填，用于验证）"
              size="large"
              :prefix-icon="Message"
              clearable
              @blur="handleEmailBlur"
            />
          </el-form-item>
          
          <el-form-item prop="emailCode">
            <div class="code-input-wrapper">
              <el-input
                v-model="registerForm.emailCode"
                placeholder="请输入邮箱验证码"
                size="large"
                :prefix-icon="Key"
                clearable
                maxlength="6"
              />
              <el-button
                type="primary"
                :disabled="!canSendCode || countdown > 0"
                :loading="sendingCode"
                @click="handleSendCode"
                class="send-code-btn"
              >
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-button"
              :loading="loading"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>
          
          <div class="login-link">
            <span>已有账号？</span>
            <el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, UserFilled, Message, Key } from '@element-plus/icons-vue'
import { register, sendRegisterCode } from '@/api/auth'
import type { RegisterRequest } from '@/types/user'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

const registerForm = reactive<RegisterRequest & { confirmPassword: string; emailCode: string }>({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  emailCode: ''
})

const canSendCode = computed(() => {
  return registerForm.email && 
         registerForm.email.trim() !== '' && 
         /^[A-Za-z0-9+_.-]+@(.+)$/.test(registerForm.email)
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateEmailCode = (rule: any, value: string, callback: any) => {
  if (!value || value.trim() === '') {
    callback(new Error('请输入邮箱验证码'))
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('验证码为6位数字'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  emailCode: [
    { validator: validateEmailCode, trigger: 'blur' }
  ]
}

const handleEmailBlur = () => {
  // 邮箱失焦时，如果验证码输入框为空，清除验证码
  if (registerForm.emailCode && !canSendCode.value) {
    registerForm.emailCode = ''
  }
}

const handleSendCode = async () => {
  if (!canSendCode.value) {
    ElMessage.warning('请先输入正确的邮箱地址')
    return
  }
  
  sendingCode.value = true
  try {
    await sendRegisterCode({ email: registerForm.email })
    ElMessage.success('验证码已发送，请查收邮箱')
    
    // 开始倒计时
    countdown.value = 60
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
    countdownTimer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (countdownTimer) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register({
          username: registerForm.username,
          password: registerForm.password,
          nickname: registerForm.nickname || undefined,
          email: registerForm.email,
          emailCode: registerForm.emailCode
        })
        
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.register-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: auto;
  @include flex-center;
  padding: 48px 24px;
  min-height: 600px;
}

.register-background {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
  
  .gradient-overlay {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 20% 30%, rgba(124, 58, 237, 0.3), transparent 50%),
                radial-gradient(circle at 80% 70%, rgba(59, 130, 246, 0.3), transparent 50%);
  }
  
  .floating-shapes {
    position: absolute;
    inset: 0;
    overflow: hidden;
    
    .shape {
      position: absolute;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      animation: float 20s infinite ease-in-out;
      
      &.shape-1 {
        width: 300px;
        height: 300px;
        top: 10%;
        left: 10%;
        animation-delay: 0s;
      }
      
      &.shape-2 {
        width: 200px;
        height: 200px;
        top: 60%;
        right: 10%;
        animation-delay: 5s;
      }
      
      &.shape-3 {
        width: 150px;
        height: 150px;
        bottom: 20%;
        left: 50%;
        animation-delay: 10s;
      }
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

.register-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
}

.register-card {
  backdrop-filter: blur(20px) saturate(180%);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  padding: 48px 40px;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 
      0 24px 80px rgba(0, 0, 0, 0.35),
      0 0 0 1px rgba(255, 255, 255, 0.15) inset;
  }
  
  .card-header {
    text-align: center;
    margin-bottom: 40px;
    
    .logo-wrapper {
      margin-bottom: 12px;
      
      .logo-text {
        font-size: 52px;
        font-weight: 800;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        font-family: 'Roboto', 'Microsoft YaHei', sans-serif;
        letter-spacing: -1px;
        margin: 0;
        line-height: 1.2;
      }
      
      .logo-subtitle {
        font-size: 12px;
        color: rgba(102, 126, 234, 0.7);
        font-weight: 500;
        margin-top: 4px;
        letter-spacing: 2px;
      }
    }
    
    .welcome-text {
      font-size: 18px;
      color: var(--text-gray);
      font-weight: 500;
      margin: 0;
    }
  }
  
  .register-form {
    :deep(.el-form-item) {
      margin-bottom: 24px;
      
      .el-input__wrapper {
        border-radius: 12px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        transition: all 0.3s ease;
        
        &:hover {
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
        }
        
        &.is-focus {
          box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
        }
      }
    }
    
    .code-input-wrapper {
      display: flex;
      gap: 12px;
      
      :deep(.el-input) {
        flex: 1;
      }
      
      .send-code-btn {
        flex-shrink: 0;
        white-space: nowrap;
        min-width: 120px;
        border-radius: 12px;
        font-weight: 600;
      }
    }
    
    .register-button {
      width: 100%;
      height: 52px;
      font-size: 17px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 12px;
      margin-top: 8px;
      transition: all 0.3s ease;
      
      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
      }
      
      &:active:not(:disabled) {
        transform: translateY(0);
      }
    }
    
    .login-link {
      text-align: center;
      margin-top: 24px;
      color: var(--text-gray);
      font-size: 14px;
      
      .el-link {
        margin-left: 8px;
        font-weight: 600;
        font-size: 14px;
      }
    }
  }
}

[data-theme="dark"] {
  .register-card {
    background: rgba(30, 41, 59, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 
      0 20px 60px rgba(0, 0, 0, 0.5),
      0 0 0 1px rgba(255, 255, 255, 0.05) inset;
    
    &:hover {
      box-shadow: 
        0 24px 80px rgba(0, 0, 0, 0.6),
        0 0 0 1px rgba(255, 255, 255, 0.08) inset;
    }
    
    .card-header {
      .welcome-text {
        color: rgba(255, 255, 255, 0.7);
      }
    }
    
    .register-form {
      .login-link {
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }
}
</style>
