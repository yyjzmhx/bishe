<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-input-wrapper">
        <el-input
          v-model="keyword"
          placeholder="搜索歌曲、歌手..."
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #suffix>
            <el-button
              type="primary"
              :icon="Search"
              @click="handleSearch"
            >
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="keyword" class="search-content">
      <!-- 搜索统计 -->
      <div class="search-stats">
        <span class="stats-text">
          找到 <strong>{{ total }}</strong> 首相关音乐
        </span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="4" animated />
      </div>

      <!-- 音乐列表 -->
      <div v-else-if="musicList.length > 0" class="music-grid">
        <MusicCard
          v-for="music in musicList"
          :key="music.id"
          :music="music"
          class="fade-in"
        />
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-empty description="未找到相关音乐，试试其他关键词">
          <el-button type="primary" @click="keyword = ''">清空搜索</el-button>
        </el-empty>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 48, 96]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 初始状态（无搜索关键词） -->
    <div v-else class="initial-state">
      <div class="initial-content">
        <el-icon class="search-icon"><Search /></el-icon>
        <h2 class="initial-title">搜索音乐</h2>
        <p class="initial-desc">输入歌曲名、歌手名进行搜索</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getMusicList } from '@/api/music'
import MusicCard from '@/components/common/MusicCard.vue'
import type { Music } from '@/types/music'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const musicList = ref<Music[]>([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 从路由参数获取搜索关键词
onMounted(() => {
  const routeKeyword = route.query.keyword as string
  if (routeKeyword) {
    keyword.value = routeKeyword
    handleSearch()
  }
})

// 监听路由变化
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword && typeof newKeyword === 'string') {
    keyword.value = newKeyword
    handleSearch()
  }
})

const handleSearch = async () => {
  if (!keyword.value.trim()) {
    return
  }

  // 更新URL
  router.replace({
    query: { keyword: keyword.value }
  })

  loading.value = true
  try {
    const result = await getMusicList({
      keyword: keyword.value.trim(),
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    
    musicList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    console.error('搜索失败', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  handleSearch()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  handleSearch()
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.search-page {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.search-header {
  margin-bottom: 32px;
  
  .search-input-wrapper {
    max-width: 600px;
    margin: 0 auto;
    
    :deep(.el-input__wrapper) {
      padding: 12px 16px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border-radius: var(--radius-lg);
      
      &:hover {
        box-shadow: 0 6px 16px rgba(0, 195, 255, 0.2);
      }
      
      &.is-focus {
        box-shadow: 0 6px 20px rgba(0, 195, 255, 0.3);
      }
    }
    
    :deep(.el-input__suffix) {
      .el-button {
        margin-left: 8px;
      }
    }
  }
}

.search-content {
  .search-stats {
    margin-bottom: 24px;
    padding: 16px;
    background: linear-gradient(135deg, rgba(0, 195, 255, 0.05) 0%, rgba(124, 58, 237, 0.05) 100%);
    border-radius: var(--radius-md);
    
    .stats-text {
      font-size: 16px;
      color: var(--text-gray);
      
      strong {
        color: var(--primary-color);
        font-size: 20px;
        font-weight: 700;
      }
    }
  }
  
  .loading-container {
    padding: 48px;
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

.initial-state {
  padding: 120px 0;
  text-align: center;
  
  .initial-content {
    max-width: 400px;
    margin: 0 auto;
    
    .search-icon {
      font-size: 80px;
      color: var(--text-light-gray);
      margin-bottom: 24px;
    }
    
    .initial-title {
      font-size: 28px;
      font-weight: 600;
      color: var(--text-dark);
      margin: 0 0 12px 0;
    }
    
    .initial-desc {
      font-size: 16px;
      color: var(--text-gray);
      margin: 0;
    }
  }
}
</style>

