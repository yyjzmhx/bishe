<template>
  <div class="home-page">
    <!-- 轮播图 -->
    <div class="carousel-section">
      <el-carousel
        :interval="5000"
        height="320px"
        indicator-position="outside"
        arrow="hover"
      >
        <el-carousel-item v-for="(item, index) in carouselItems" :key="index">
          <div class="carousel-item" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="carousel-overlay">
              <div class="carousel-content">
                <h2 class="carousel-title">{{ item.title }}</h2>
                <p class="carousel-desc">{{ item.description }}</p>
                <el-button type="primary" size="large" @click="handleCarouselClick(item)">
                  立即体验
                </el-button>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>
    
    <!-- 欢迎语 -->
    <div class="welcome-section">
      <h2 class="welcome-title">
        欢迎回来，<span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
      </h2>
      <p class="welcome-desc">发现属于你的音乐世界</p>
    </div>
    
    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-wrapper">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索歌曲、歌手、专辑..."
          size="large"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #suffix>
            <el-button
              type="primary"
              :icon="Search"
              :loading="searching"
              @click="handleSearch"
            >
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
    
    <!-- 搜索结果 -->
    <div v-if="isSearchMode" class="search-results">
      <div class="search-header">
        <div class="search-stats">
          <span class="stats-text">
            找到 <strong>{{ searchTotal }}</strong> 首相关音乐
            <span v-if="searchKeyword" class="keyword">"{{ searchKeyword }}"</span>
          </span>
        </div>
        <el-button text type="primary" @click="handleClearSearch">
          <el-icon><Close /></el-icon>
          清除搜索
        </el-button>
      </div>
      
      <div v-if="searchLoading" class="music-grid">
        <div v-for="i in 12" :key="i" class="skeleton-card">
          <div class="skeleton skeleton-cover"></div>
          <div class="skeleton skeleton-title"></div>
          <div class="skeleton skeleton-artist"></div>
        </div>
      </div>
      
      <div v-else-if="searchResults.length > 0" class="music-grid">
        <MusicCard
          v-for="music in searchResults"
          :key="music.id"
          :music="music"
          class="fade-in"
        />
      </div>
      
      <div v-else class="empty-state">
        <el-empty description="未找到相关音乐，试试其他关键词">
          <el-button type="primary" @click="handleClearSearch">返回首页</el-button>
        </el-empty>
      </div>
      
      <!-- 搜索结果分页 -->
      <div v-if="searchTotal > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchPageNum"
          v-model:page-size="searchPageSize"
          :total="searchTotal"
          :page-sizes="[12, 24, 48, 96]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearchSizeChange"
          @current-change="handleSearchPageChange"
        />
      </div>
    </div>
    
    <!-- 热门推荐 -->
    <div class="recommend-section">
      <div class="section-header">
        <h3 class="section-title">
          <el-icon><StarFilled /></el-icon>
          热门推荐
        </h3>
        <el-link type="primary" underline="never" @click="$router.push('/upload')">
          音频分析推荐 <el-icon><ArrowRight /></el-icon>
        </el-link>
      </div>
      
      <div v-if="loading" class="music-grid">
        <div v-for="i in 8" :key="i" class="skeleton-card">
          <div class="skeleton skeleton-cover"></div>
          <div class="skeleton skeleton-title"></div>
          <div class="skeleton skeleton-artist"></div>
        </div>
      </div>
      
      <div v-else class="music-grid">
        <MusicCard
          v-for="music in musicList"
          :key="music.id"
          :music="music"
          class="fade-in"
        />
      </div>
      
      <div v-if="!loading && musicList.length === 0" class="empty-state">
        <el-empty description="暂无推荐音乐">
          <el-button type="primary" @click="$router.push('/upload')">音频分析推荐</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { StarFilled, ArrowRight, Search, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getMusicList } from '@/api/music'
import MusicCard from '@/components/common/MusicCard.vue'
import type { Music } from '@/types/music'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const musicList = ref<Music[]>([])

// 搜索相关
const searchKeyword = ref('')
const isSearchMode = ref(false)
const searching = ref(false)
const searchLoading = ref(false)
const searchResults = ref<Music[]>([])
const searchTotal = ref(0)
const searchPageNum = ref(1)
const searchPageSize = ref(12)
const searchTimer = ref<number | null>(null)

const carouselItems = ref([
  {
    title: 'AI智能推荐',
    description: '上传你的音乐，AI为你找到相似的音乐',
    image: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1920',
    link: '/upload'
  },
  {
    title: '个性化体验',
    description: '基于你的喜好，推荐最适合的音乐',
    image: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1920',
    link: '/upload'
  },
  {
    title: '发现新音乐',
    description: '探索音乐库，发现更多精彩',
    image: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1920',
    link: '/'
  }
])

const handleCarouselClick = (item: any) => {
  router.push(item.link)
}

const loadMusicList = async () => {
  loading.value = true
  try {
    const result = await getMusicList({ pageNum: 1, pageSize: 12 })
    musicList.value = result.list || []
  } catch (error) {
    console.error('加载音乐列表失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索处理（用户主动触发，立即执行）
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  // 清除之前的定时器（如果有）
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
    searchTimer.value = null
  }

  // 立即执行搜索
  await performSearch()
}

// 执行搜索
const performSearch = async () => {
  if (!searchKeyword.value.trim()) {
    return
  }

  isSearchMode.value = true
  searching.value = true
  searchLoading.value = true

  try {
    const result = await getMusicList({
      keyword: searchKeyword.value.trim(),
      pageNum: searchPageNum.value,
      pageSize: searchPageSize.value
    })

    searchResults.value = result.list || []
    searchTotal.value = result.total || 0

    // 滚动到搜索结果区域
    nextTick(() => {
      const searchSection = document.querySelector('.search-results')
      if (searchSection) {
        searchSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    })
  } catch (error: any) {
    console.error('搜索失败', error)
    ElMessage.error('搜索失败，请稍后重试')
    searchResults.value = []
    searchTotal.value = 0
  } finally {
    searching.value = false
    searchLoading.value = false
  }
}

// 清除搜索
const handleClearSearch = () => {
  searchKeyword.value = ''
  isSearchMode.value = false
  searchResults.value = []
  searchTotal.value = 0
  searchPageNum.value = 1
  searchPageSize.value = 12

  // 清除定时器
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
    searchTimer.value = null
  }

  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 搜索分页处理
const handleSearchSizeChange = (size: number) => {
  searchPageSize.value = size
  searchPageNum.value = 1
  performSearch()
}

const handleSearchPageChange = (page: number) => {
  searchPageNum.value = page
  performSearch()
}

// 监听路由查询参数（支持从其他地方跳转过来带搜索关键词）
onMounted(() => {
  loadMusicList()

  // 检查URL中是否有搜索关键词
  const routeKeyword = route.query.keyword as string
  if (routeKeyword) {
    searchKeyword.value = routeKeyword
    performSearch()
  }
})

// 监听路由变化
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword && typeof newKeyword === 'string') {
    searchKeyword.value = newKeyword
    performSearch()
  }
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.home-page {
  max-width: 1600px;
  margin: 0 auto;
}

.carousel-section {
  margin-bottom: 48px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  
  .carousel-item {
    width: 100%;
    height: 100%;
    background-size: cover;
    background-position: center;
    position: relative;
    
    .carousel-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(135deg, rgba(30, 64, 175, 0.8), rgba(59, 130, 246, 0.6));
      @include flex-center;
      
      .carousel-content {
        text-align: center;
        color: white;
        padding: 0 24px;
        
        .carousel-title {
          font-size: 48px;
          font-weight: 700;
          margin-bottom: 16px;
          font-family: 'Roboto', sans-serif;
        }
        
        .carousel-desc {
          font-size: 20px;
          margin-bottom: 32px;
          opacity: 0.9;
        }
      }
    }
  }
}

.welcome-section {
  text-align: center;
  margin-bottom: 32px;
  
  .welcome-title {
    font-size: 32px;
    font-weight: 600;
    margin-bottom: 8px;
    
    .username {
      @include gradient-text;
    }
  }
  
  .welcome-desc {
    font-size: 16px;
    color: var(--text-gray);
  }
}

.search-section {
  margin-bottom: 48px;
  
  .search-wrapper {
    max-width: 600px;
    margin: 0 auto;
    
    :deep(.el-input__wrapper) {
      padding: 12px 16px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border-radius: var(--radius-lg);
      transition: all 0.3s ease;
      
      &:hover {
        box-shadow: 0 6px 16px rgba(59, 130, 246, 0.2);
      }
      
      &.is-focus {
        box-shadow: 0 6px 20px rgba(59, 130, 246, 0.3);
      }
    }
    
    :deep(.el-input__suffix) {
      .el-button {
        margin-left: 8px;
      }
    }
  }
}

.search-results {
  margin-bottom: 48px;
  
  .search-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding: 16px;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(124, 58, 237, 0.05) 100%);
    border-radius: var(--radius-md);
    
    .search-stats {
      .stats-text {
        font-size: 16px;
        color: var(--text-gray);
        
        strong {
          color: var(--primary-color);
          font-size: 20px;
          font-weight: 700;
        }
        
        .keyword {
          color: var(--primary-color);
          font-weight: 500;
        }
      }
    }
  }
  
  .music-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
    margin-bottom: 32px;
  }
  
  .empty-state {
    padding: 80px 0;
    text-align: center;
  }
  
  .pagination-wrapper {
    display: flex;
    justify-content: center;
    padding: 32px 0;
  }
}

.recommend-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 24px;
      font-weight: 600;
      color: var(--text-dark);
      
      .el-icon {
        color: var(--secondary-color);
      }
    }
  }
  
  .music-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
    
    .skeleton-card {
      .skeleton-cover {
        width: 100%;
        aspect-ratio: 1;
        margin-bottom: 12px;
      }
      
      .skeleton-title {
        height: 20px;
        width: 80%;
        margin-bottom: 8px;
      }
      
      .skeleton-artist {
        height: 16px;
        width: 60%;
      }
    }
  }
  
  .empty-state {
    padding: 48px 0;
    text-align: center;
  }
}

:deep(.el-carousel__indicators) {
  .el-carousel__button {
        background: rgba(255, 255, 255, 0.5);
        
        &.is-active {
          background: white;
        }
      }
}
</style>

