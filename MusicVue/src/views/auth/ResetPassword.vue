<template>
  <div class="reset-password-page">
    <div class="background-animation">
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
    
    <div class="reset-password-container">
      <div class="reset-password-card glass-card">
        <div class="card-header">
          <div class="icon-wrapper success">
            <el-icon :size="48">
              <Lock />
            </el-icon>
          </div>
          <h1 class="title text-gradient">设置新密码</h1>
          <p class="subtitle">请设置您的新密码，密码长度至少6位</p>
        </div>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="reset-password-form"
          @keyup.enter="handleReset"
        >
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入新密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
              class="custom-input"
              @input="checkPasswordStrength"
            />
            <div v-if="form.password" class="password-strength">
              <div class="strength-bar">
                <div
                  class="strength-fill"
                  :class="strengthLevel"
                  :style="{ width: strengthPercent + '%' }"
                ></div>
              </div>
              <span class="strength-text" :class="strengthLevel">
                {{ strengthText }}
              </span>
            </div>
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
              class="custom-input"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="reset-button"
              :loading="resetting"
              @click="handleReset"
            >
              {{ resetting ? '重置中...' : '重置密码' }}
            </el-button>
          </el-form-item>
          
          <div class="steps-indicator">
            <div class="step completed">
              <div class="step-dot"></div>
              <span class="step-label">输入邮箱</span>
            </div>
            <div class="step-line active"></div>
            <div class="step completed">
              <div class="step-dot"></div>
              <span class="step-label">验证码</span>
            </div>
            <div class="step-line active"></div>
            <div class="step active">
              <div class="step-dot"></div>
              <span class="step-label">重置密码</span>
            </div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { resetPassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const resetting = ref(false)

const form = reactive({
  password: '',
  confirmPassword: ''
})

const strengthLevel = ref<'weak' | 'medium' | 'strong'>('weak')
const strengthPercent = computed(() => {
  if (strengthLevel.value === 'weak') return 33
  if (strengthLevel.value === 'medium') return 66
  return 100
})

const strengthText = computed(() => {
  if (strengthLevel.value === 'weak') return '弱'
  if (strengthLevel.value === 'medium') return '中'
  return '强'
})

const checkPasswordStrength = () => {
  const password = form.password
  if (password.length < 6) {
    strengthLevel.value = 'weak'
    return
  }
  
  let strength = 0
  if (password.length >= 8) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/[0-9]/.test(password)) strength++
  if (/[^a-zA-Z0-9]/.test(password)) strength++
  
  if (strength <= 1) strengthLevel.value = 'weak'
  else if (strength <= 2) strengthLevel.value = 'medium'
  else strengthLevel.value = 'strong'
}

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
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

const handleReset = async () => {
  if (!formRef.value) return
  
  const email = route.query.email as string
  const code = route.query.code as string
  
  if (!email || !code) {
    ElMessage.error('验证信息缺失，请重新操作')
    router.push('/forgot-password')
    return
  }
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      resetting.value = true
      try {
        await resetPassword({
          email,
          code,
          password: form.password
        })
        ElMessage.success('密码重置成功，请使用新密码登录')
        router.push('/login')
      } catch (error: any) {
        ElMessage.error(error.message || '密码重置失败')
      } finally {
        resetting.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.reset-password-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  @include flex-center;
}

.background-animation {
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

.reset-password-container {
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

.reset-password-card {
  backdrop-filter: blur(30px) saturate(180%);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border-radius: 20px;
  padding: 48px 40px;
  
  .card-header {
    text-align: center;
    margin-bottom: 36px;
    
    .icon-wrapper {
      width: 80px;
      height: 80px;
      margin: 0 auto 20px;
      border-radius: 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      @include flex-center;
      box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
      
      &.success {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        box-shadow: 0 8px 24px rgba(16, 185, 129, 0.4);
      }
      
      .el-icon {
        color: white;
      }
    }
    
    .title {
      font-size: 32px;
      font-weight: 700;
      margin-bottom: 8px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    
    .subtitle {
      font-size: 14px;
      color: #718096;
      line-height: 1.6;
    }
  }
  
  .reset-password-form {
    :deep(.el-form-item) {
      margin-bottom: 20px;
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
    }
    
    .password-strength {
      margin-top: 8px;
      
      .strength-bar {
        height: 4px;
        background: #e2e8f0;
        border-radius: 2px;
        overflow: hidden;
        margin-bottom: 4px;
        
        .strength-fill {
          height: 100%;
          transition: all 0.3s ease;
          border-radius: 2px;
          
          &.weak {
            background: #ef4444;
          }
          
          &.medium {
            background: #f59e0b;
          }
          
          &.strong {
            background: #10b981;
          }
        }
      }
      
      .strength-text {
        font-size: 12px;
        font-weight: 500;
        
        &.weak {
          color: #ef4444;
        }
        
        &.medium {
          color: #f59e0b;
        }
        
        &.strong {
          color: #10b981;
        }
      }
    }
    
    .reset-button {
      width: 100%;
      height: 52px;
      font-size: 16px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 12px;
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
      transition: all 0.3s ease;
      
      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
      }
    }
    
    .steps-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 32px 0 0;
      padding: 0 20px;
      
      .step {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
        
        .step-dot {
          width: 12px;
          height: 12px;
          border-radius: 50%;
          background: #e2e8f0;
          transition: all 0.3s ease;
        }
        
        .step-label {
          font-size: 12px;
          color: #a0aec0;
          transition: all 0.3s ease;
        }
        
        &.active {
          .step-dot {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.2);
          }
          
          .step-label {
            color: #667eea;
            font-weight: 600;
          }
        }
        
        &.completed {
          .step-dot {
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
          }
          
          .step-label {
            color: #10b981;
          }
        }
      }
      
      .step-line {
        flex: 1;
        height: 2px;
        background: #e2e8f0;
        margin: 0 16px;
        margin-bottom: 20px;
        transition: all 0.3s ease;
        
        &.active {
          background: linear-gradient(to right, #10b981, #667eea);
        }
      }
    }
  }
}

[data-theme="dark"] {
  .reset-password-card {
    background: rgba(26, 32, 44, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
    
    .card-header {
      .subtitle {
        color: #a0aec0;
      }
    }
    
    .reset-password-form {
      .custom-input {
        :deep(.el-input__wrapper) {
          background: rgba(45, 55, 72, 0.8);
          border-color: rgba(255, 255, 255, 0.1);
        }
      }
    }
  }
}
</style>

