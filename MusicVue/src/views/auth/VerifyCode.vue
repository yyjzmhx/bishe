<template>
  <div class="verify-code-page">
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
    
    <div class="verify-code-container">
      <div class="verify-code-card glass-card">
        <div class="card-header">
          <div class="icon-wrapper success">
            <el-icon :size="48">
              <Key />
            </el-icon>
          </div>
          <h1 class="title text-gradient">输入验证码</h1>
          <p class="subtitle">
            验证码已发送至 <span class="email-highlight">{{ email }}</span>
          </p>
        </div>
        
        <VerificationCodeInput
          v-model="code"
          :length="6"
          :loading="verifying"
          @complete="handleVerify"
        />
        
        <div class="resend-section">
          <el-link
            v-if="countdown === 0"
            type="primary"
            :underline="false"
            @click="handleResend"
          >
            <el-icon><Refresh /></el-icon>
            <span>重新发送验证码</span>
          </el-link>
          <span v-else class="countdown-text">
            {{ countdown }}秒后可重新发送
          </span>
        </div>
        
        <div class="steps-indicator">
          <div class="step completed">
            <div class="step-dot"></div>
            <span class="step-label">输入邮箱</span>
          </div>
          <div class="step-line active"></div>
          <div class="step active">
            <div class="step-dot"></div>
            <span class="step-label">验证码</span>
          </div>
          <div class="step-line"></div>
          <div class="step">
            <div class="step-dot"></div>
            <span class="step-label">重置密码</span>
          </div>
        </div>
        
        <div class="back-to-login">
          <el-link type="primary" :underline="false" @click="$router.push('/login')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回登录</span>
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Refresh, ArrowLeft } from '@element-plus/icons-vue'
import VerificationCodeInput from '@/components/auth/VerificationCodeInput.vue'
import { verifyCode, forgotPassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const email = ref((route.query.email as string) || '')
const code = ref('')
const verifying = ref(false)
const countdown = ref(60)

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

const handleVerify = async (verificationCode: string) => {
  if (!email.value) {
    ElMessage.error('邮箱地址缺失')
    router.push('/forgot-password')
    return
  }
  
  verifying.value = true
  try {
    await verifyCode({
      email: email.value,
      code: verificationCode
    })
    
    ElMessage.success('验证码验证成功')
    
    // 跳转到重置密码页面
    router.push({
      path: '/reset-password',
      query: {
        email: email.value,
        code: verificationCode
      }
    })
  } catch (error: any) {
    ElMessage.error(error.message || '验证码错误或已过期')
    code.value = ''
  } finally {
    verifying.value = false
  }
}

const handleResend = async () => {
  if (!email.value) {
    ElMessage.error('邮箱地址缺失')
    return
  }
  
  try {
    await forgotPassword({ email: email.value })
    ElMessage.success('验证码已重新发送')
    
    // 重新开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    
    code.value = ''
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  }
}

onMounted(() => {
  if (!email.value) {
    router.push('/forgot-password')
    return
  }
  
  // 开始倒计时
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.verify-code-page {
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

.verify-code-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
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

.verify-code-card {
  backdrop-filter: blur(30px) saturate(180%);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border-radius: 20px;
  padding: 48px 40px;
  
  .card-header {
    text-align: center;
    margin-bottom: 32px;
    
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
      
      .email-highlight {
        color: #667eea;
        font-weight: 600;
      }
    }
  }
  
  .resend-section {
    text-align: center;
    margin-bottom: 32px;
    
    .el-link {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 500;
      
      &:hover {
        transform: translateX(2px);
      }
    }
    
    .countdown-text {
      font-size: 14px;
      color: #a0aec0;
    }
  }
  
  .steps-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 32px 0 24px;
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
  
  .back-to-login {
    text-align: center;
    margin-top: 24px;
    
    .el-link {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s ease;
      
      &:hover {
        transform: translateX(-2px);
      }
    }
  }
}

[data-theme="dark"] {
  .verify-code-card {
    background: rgba(26, 32, 44, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
    
    .card-header {
      .subtitle {
        color: #a0aec0;
      }
    }
  }
}
</style>

