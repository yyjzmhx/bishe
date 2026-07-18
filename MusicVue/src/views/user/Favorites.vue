<template>
  <div class="favorites-page">
    <div class="page-header">
      <div>
        <h2 class="page-title text-gradient">我的收藏夹</h2>
        <p class="page-subtitle">每个账号独立保存自己的收藏歌曲</p>
      </div>
      <div class="page-stats">
        <span class="stats-value">{{ total }}</span>
        <span class="stats-label">首已收藏</span>
      </div>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索收藏的歌曲、歌手或专辑"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div v-loading="loading" class="content">
      <div v-if="favoriteList.length" class="music-grid">
        <MusicCard
          v-for="music in favoriteList"
          :key="music.favoriteId"
          :music="music"
          :initial-favorite="true"
          @favorite-change="handleFavoriteChange"
        />
      </div>

      <el-empty v-else description="收藏夹还是空的，先去挑几首歌吧" />
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadFavorites"
        @current-change="loadFavorites"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MusicCard from '@/components/common/MusicCard.vue'
import { getFavoriteList, type FavoriteMusicVO } from '@/api/feedback'

const loading = ref(false)
const keyword = ref('')
const favoriteList = ref<FavoriteMusicVO[]>([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

const loadFavorites = async () => {
  loading.value = true
  try {
    const result = await getFavoriteList({
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    favoriteList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载收藏夹失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadFavorites()
}

const handleFavoriteChange = ({ musicId, isFavorite }: { musicId: number; isFavorite: boolean }) => {
  if (isFavorite) return

  favoriteList.value = favoriteList.value.filter(item => item.id !== musicId)
  total.value = Math.max(total.value - 1, 0)

  if (!favoriteList.value.length && pageNum.value > 1) {
    pageNum.value -= 1
    loadFavorites()
  }
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped lang="scss">
.favorites-page {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 24px;

  .page-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 8px;
  }

  .page-subtitle {
    color: var(--text-gray);
    margin: 0;
  }

  .page-stats {
    min-width: 140px;
    padding: 20px 24px;
    border-radius: var(--radius-lg);
    background: linear-gradient(135deg, rgba(0, 195, 255, 0.12) 0%, rgba(0, 136, 255, 0.08) 100%);
    text-align: center;

    .stats-value {
      display: block;
      font-size: 28px;
      font-weight: 700;
      color: var(--primary-color);
    }

    .stats-label {
      color: var(--text-gray);
      font-size: 13px;
    }
  }
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.content {
  min-height: 320px;
}

.music-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
