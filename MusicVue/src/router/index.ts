import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMusicStore } from '@/store/music'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false, isUserLogin: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/auth/AdminLogin.vue'),
    meta: { requiresAuth: false, isAdminLogin: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPassword.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/verify-code',
    name: 'VerifyCode',
    component: () => import('@/views/auth/VerifyCode.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/auth/ResetPassword.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/user/Home.vue')
      },
      {
        path: 'upload',
        name: 'Upload',
        component: () => import('@/views/user/Upload.vue')
      },
      {
        path: 'analysis/:uploadId',
        name: 'Analysis',
        component: () => import('@/views/user/Analysis.vue')
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('@/views/user/History.vue')
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/user/Favorites.vue')
      },
      {
        path: 'community',
        name: 'Community',
        component: () => import('@/views/user/Community.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue')
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/user/Search.vue')
      },
      {
        path: 'feedback',
        name: 'Feedback',
        component: () => import('@/views/user/Feedback.vue')
      },
      {
        path: 'music/:id',
        name: 'MusicDetail',
        component: () => import('@/views/user/MusicDetail.vue')
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/music',
        name: 'AdminMusic',
        component: () => import('@/views/admin/MusicManage.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/uploads',
        name: 'AdminUploads',
        component: () => import('@/views/admin/UploadManage.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/feedback',
        name: 'AdminFeedback',
        component: () => import('@/views/admin/FeedbackManage.vue'),
        meta: { requiresAdmin: true, feedbackMode: 'feedback' }
      },
      {
        path: 'admin/community',
        name: 'AdminCommunity',
        component: () => import('@/views/admin/FeedbackManage.vue'),
        meta: { requiresAdmin: true, feedbackMode: 'community' }
      },
      {
        path: 'admin/site-content',
        name: 'AdminSiteContent',
        component: () => import('@/views/admin/SiteContentManage.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/NoticeManage.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/ai-monitor',
        name: 'AdminAIManage',
        component: () => import('@/views/admin/AIManage.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (userStore.isLoggedIn) {
    if (to.meta.isUserLogin && userStore.isAdmin) {
      next('/admin/login')
      return
    }

    if (to.meta.isAdminLogin && !userStore.isAdmin) {
      next('/login')
      return
    }

    if (to.meta.isUserLogin || to.meta.isAdminLogin) {
      next('/')
      return
    }
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next(to.meta.requiresAdmin ? '/admin/login' : '/login')
    return
  }

  if (to.meta.requiresAdmin) {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }

    if (!userStore.isAdmin) {
      ElMessage.warning('权限不足，请使用管理员账号登录')
      next('/')
      return
    }
  }

  if (to.meta.requiresAuth && userStore.isLoggedIn) {
    const musicStore = useMusicStore()
    if (from.path === '/login' || from.path === '/admin/login' || from.path === '/register' || from.path === '/') {
      musicStore.loadPlaylistFromServer()
    }
  }

  next()
})

export default router
