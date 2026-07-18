<template>
  <div class="profile-edit">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <button class="back-btn" @click="$emit('back')">
          <el-icon><Back /></el-icon>
        </button>
        <div class="header-text">
          <h1 class="page-title">编辑资料</h1>
          <p class="page-subtitle">完善你的个人信息，展现独特的自己</p>
        </div>
      </div>
    </div>
    
    <div class="profile-content">
      <!-- 左侧：基本信息 -->
      <div class="profile-card basic-info-card">
        <!-- 头像上传 -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
            >
              <div class="avatar-container">
                <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-image" />
                <div v-else class="avatar-placeholder">
                  <el-icon class="avatar-icon"><User /></el-icon>
                </div>
                <div class="avatar-overlay">
                  <el-icon class="upload-icon"><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </div>
              <div class="edit-badge">
                <el-icon><Edit /></el-icon>
              </div>
            </el-upload>
          </div>
          <p class="avatar-hint">支持 JPG、PNG，最大 2MB</p>
        </div>
        
        <!-- 基本信息表单 -->
        <el-form
          ref="formRef"
          :model="userInfo"
          :rules="rules"
          class="profile-form"
        >
          <div class="form-group">
            <div class="form-label">
              <el-icon><User /></el-icon>用户名
            </div>
            <el-form-item prop="username">
              <el-input v-model="userInfo.username" disabled class="custom-input" />
            </el-form-item>
          </div>
          
          <div class="form-group">
            <div class="form-label">
              <el-icon><Edit /></el-icon>昵称
            </div>
            <el-form-item prop="nickname">
              <el-input v-model="userInfo.nickname" placeholder="设置一个好听的昵称" class="custom-input" />
            </el-form-item>
          </div>
          
          <div class="form-group">
            <div class="form-label">
              <el-icon><Phone /></el-icon>手机号
            </div>
            <el-form-item prop="phone">
              <el-input v-model="userInfo.phone" placeholder="绑定手机号" class="custom-input" />
            </el-form-item>
          </div>
          
          <div class="form-group">
            <div class="form-label">
              <el-icon><Message /></el-icon>邮箱
            </div>
            <el-form-item prop="email">
              <el-input v-model="userInfo.email" placeholder="绑定邮箱" class="custom-input" />
            </el-form-item>
          </div>
          
          <div class="form-actions">
            <el-button class="cancel-btn" @click="handleCancel">重置</el-button>
            <el-button type="primary" class="save-btn" @click="handleSave">保存修改</el-button>
          </div>
        </el-form>
      </div>
      
      <!-- 右侧：标签与安全 -->
      <div class="right-column">
        <!-- 个性化标签 -->
        <div class="profile-card tags-card">
          <PersonalTagEditor />
        </div>
        
        <!-- 账号安全 -->
        <div class="password-card">
          <div class="card-header">
            <div class="icon-box">
              <el-icon><Lock /></el-icon>
            </div>
            <h3>账号安全</h3>
          </div>
          
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            class="password-form"
          >
            <div class="password-grid">
              <div class="form-group full-width">
                <div class="form-label">原密码<span class="required">*</span></div>
                <el-form-item prop="oldPassword">
                  <el-input 
                    v-model="passwordForm.oldPassword" 
                    type="password" 
                    show-password 
                    placeholder="请输入当前密码"
                    class="custom-input" 
                  />
                </el-form-item>
              </div>
              
              <div class="form-group">
                <div class="form-label">新密码<span class="required">*</span></div>
                <el-form-item prop="newPassword">
                  <el-input 
                    v-model="passwordForm.newPassword" 
                    type="password" 
                    show-password 
                    placeholder="至少6位"
                    class="custom-input" 
                  />
                </el-form-item>
              </div>
              
              <div class="form-group">
                <div class="form-label">确认密码<span class="required">*</span></div>
                <el-form-item prop="confirmPassword">
                  <el-input 
                    v-model="passwordForm.confirmPassword" 
                    type="password" 
                    show-password 
                    placeholder="再次输入"
                    class="custom-input" 
                  />
                </el-form-item>
              </div>
            </div>
            
            <div class="password-actions">
              <el-button type="warning" class="submit-btn" @click="handleChangePassword">
                修改密码
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { 
  User, Camera, Edit, Phone, Message, 
  Lock, Key, Back
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/api/request'
import PersonalTagEditor from '@/components/user/PersonalTagEditor.vue'

defineEmits(['back'])

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const userInfo = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  nickname: [
    { max: 20, message: '昵称长度不能超过20个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const beforeAvatarUpload = async (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  
  // 上传到服务器
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await request.post<{ avatarUrl: string }>('/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    // 使用服务器返回的URL
    userInfo.avatar = response.avatarUrl
    // 更新用户store中的头像
    if (userStore.userInfo) {
      userStore.userInfo.avatar = response.avatarUrl
    }
    ElMessage.success('头像上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '头像上传失败')
  }
  
  return false // 阻止默认上传行为
}

const loadUserInfo = async () => {
  try {
    const info = await request.get('/user/profile')
    Object.assign(userInfo, info)
  } catch (error) {
    console.error('加载用户信息失败', error)
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.put('/user/profile', userInfo)
        userStore.setUserInfo(userInfo as any)
        ElMessage.success('保存成功')
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      }
    }
  })
}

const handleCancel = () => {
  loadUserInfo()
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.put('/user/password', {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error: any) {
        ElMessage.error(error.message || '密码修改失败')
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.profile-edit {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 100vh;
}

// 页面头部
.page-header {
  margin-bottom: 32px;
  
  .header-content {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .back-btn {
      width: 40px;
      height: 40px;
      font-size: 20px;
      border: 1px solid rgba(0,0,0,0.1);
      background: white;
      transition: all 0.3s;
      border-radius: 12px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      
      &:hover {
        background: var(--primary-color);
        color: white;
        border-color: var(--primary-color);
        transform: translateX(-2px);
      }
      
      [data-theme="dark"] & {
        background: rgba(255, 255, 255, 0.05);
        border-color: rgba(255, 255, 255, 0.1);
        color: var(--text-light);
        
        &:hover {
          background: var(--primary-color);
          border-color: var(--primary-color);
        }
      }
    }
    
    .header-text {
      .page-title {
        font-size: 24px;
        font-weight: 700;
        margin: 0 0 4px 0;
        color: var(--text-dark);
      }
      
      .page-subtitle {
        font-size: 14px;
        color: var(--text-gray);
        margin: 0;
      }
    }
  }
}

.profile-content {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  align-items: start;
  
  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

// 卡片通用样式
.profile-card,
.password-card {
  background: white;
  border-radius: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.3s;
  
  [data-theme="dark"] & {
    background: rgba(30, 41, 59, 0.6);
    border-color: rgba(255, 255, 255, 0.08);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  }
  
  &:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }
}

// 左侧基本信息卡片
.basic-info-card {
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .avatar-section {
    margin-bottom: 32px;
    position: relative;
    
    .avatar-wrapper {
      position: relative;
      cursor: pointer;
      
      .avatar-container {
        width: 120px;
        height: 120px;
        border-radius: 50%;
        overflow: hidden;
        border: 4px solid white;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
        position: relative;
        
        [data-theme="dark"] & {
          border-color: rgba(255, 255, 255, 0.1);
        }
        
        .avatar-image,
        .avatar-placeholder {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        
        .avatar-placeholder {
          background: linear-gradient(135deg, #e0e7ff 0%, #f3e8ff 100%);
          @include flex-center;
          
          .avatar-icon {
            font-size: 48px;
            color: #818cf8;
          }
        }
        
        .avatar-overlay {
          position: absolute;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          @include flex-center;
          flex-direction: column;
          opacity: 0;
          transition: all 0.3s;
          color: white;
          
          .upload-icon { font-size: 24px; margin-bottom: 4px; }
          span { font-size: 12px; }
        }
      }
      
      .edit-badge {
        position: absolute;
        bottom: 4px;
        right: 4px;
        width: 32px;
        height: 32px;
        background: var(--primary-color);
        border-radius: 50%;
        @include flex-center;
        color: white;
        border: 3px solid white;
        box-shadow: 0 4px 10px rgba(0,0,0,0.1);
        
        [data-theme="dark"] & {
          border-color: #1e293b;
        }
      }
      
      &:hover .avatar-overlay { opacity: 1; }
    }
    
    .avatar-hint {
      margin-top: 12px;
      font-size: 12px;
      color: var(--text-light-gray);
      text-align: center;
    }
  }
  
  .profile-form {
    width: 100%;
    
    .form-group {
      margin-bottom: 20px;
      
      .form-label {
        font-size: 13px;
        font-weight: 600;
        color: var(--text-gray);
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        gap: 6px;
      }
      
      .custom-input {
        :deep(.el-input__wrapper) {
          background: var(--bg-light-secondary);
          box-shadow: none;
          border: 1px solid transparent;
          border-radius: 12px;
          padding: 8px 12px;
          transition: all 0.3s;
          
          &:hover, &.is-focus {
            background: white;
            border-color: var(--primary-color);
            box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.1);
          }
          
          [data-theme="dark"] & {
            background: rgba(0, 0, 0, 0.2);
            &:hover, &.is-focus {
              background: rgba(0, 0, 0, 0.3);
            }
          }
        }
      }
    }
    
    .form-actions {
      margin-top: 32px;
      display: flex;
      gap: 12px;
      
      .save-btn {
        flex: 2;
        border-radius: 12px;
        height: 40px;
        font-weight: 600;
      }
      
      .cancel-btn {
        flex: 1;
        border-radius: 12px;
        height: 40px;
      }
    }
  }
}

// 右侧内容区域
.right-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

// 个性化标签卡片
.tags-card {
  padding: 24px;
  min-height: 200px;
}

// 密码修改卡片
.password-card {
  padding: 24px;
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(0,0,0,0.05);
    
    [data-theme="dark"] & {
      border-bottom-color: rgba(255,255,255,0.05);
    }
    
    .icon-box {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: rgba(245, 158, 11, 0.1);
      @include flex-center;
      color: #f59e0b;
    }
    
    h3 {
      font-size: 16px;
      font-weight: 700;
      color: var(--text-dark);
      margin: 0;
    }
  }
  
  .password-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    
    .full-width {
      grid-column: 1 / -1;
    }
    
    .form-group {
      margin-bottom: 0;
      
      .form-label {
        font-size: 13px;
        font-weight: 600;
        color: var(--text-gray);
        margin-bottom: 8px;
        
        .required { color: var(--danger-color); margin-left: 4px; }
      }
      
      .custom-input {
        // 复用输入框样式
        :deep(.el-input__wrapper) {
          background: var(--bg-light-secondary);
          box-shadow: none;
          border: 1px solid transparent;
          border-radius: 12px;
          padding: 8px 12px;
          
          &:hover, &.is-focus {
            background: white;
            border-color: #f59e0b;
            box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1);
          }
          
          [data-theme="dark"] & {
            background: rgba(0, 0, 0, 0.2);
            &:hover, &.is-focus {
              background: rgba(0, 0, 0, 0.3);
            }
          }
        }
      }
    }
  }
  
  .password-actions {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
    
    .submit-btn {
      border-radius: 12px;
      padding: 10px 24px;
      background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
      border: none;
      
      &:hover {
        box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
        transform: translateY(-1px);
      }
    }
  }
}

// 暗色模式文字适配
[data-theme="dark"] {
  .page-header .header-text .page-title { color: var(--text-light); }
  .basic-info-card .profile-form .form-group .form-label { color: var(--text-light-secondary); }
  .password-card .card-header h3 { color: var(--text-light); }
  .password-card .password-grid .form-group .form-label { color: var(--text-light-secondary); }
}
</style>
