<template>
  <div class="community-page">
    <div class="hero">
      <div>
        <h2 class="page-title">音乐社区</h2>
        <p class="page-subtitle">查看大家正在讨论的歌曲内容，也可以直接跳到歌曲详情继续参与</p>
      </div>

      <el-radio-group v-model="sortType" size="large" @change="handleSortChange">
        <el-radio-button label="latest">最新</el-radio-button>
        <el-radio-button label="hot">最热</el-radio-button>
      </el-radio-group>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索社区帖子、用户或歌曲"
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

    <div v-loading="loading" class="feed">
      <template v-if="postList.length">
        <article
          v-for="post in postList"
          :key="post.id"
          class="post-card"
        >
          <div class="post-cover" @click="openMusic(post.musicId)">
            <img :src="post.musicCoverUrl || defaultCover" :alt="post.musicTitle || 'music'" />
          </div>

          <div class="post-body">
            <div class="post-meta">
              <div class="author">
                <el-avatar :src="post.avatar">{{ (post.nickname || post.username || 'U').slice(0, 1) }}</el-avatar>
                <div>
                  <div class="author-name">{{ post.nickname || post.username || '匿名用户' }}</div>
                  <div class="post-time">{{ formatDate(post.createTime) }}</div>
                </div>
              </div>
              <el-tag type="info" effect="plain">{{ post.musicTitle || '未关联歌曲' }}</el-tag>
            </div>

            <p class="post-content">{{ post.content }}</p>

            <div class="music-line">
              <span>{{ post.musicTitle || '未知歌曲' }}</span>
              <span v-if="post.musicArtist">· {{ post.musicArtist }}</span>
            </div>

            <div class="post-actions">
              <el-button text @click="handlePostLike(post)">
                {{ post.isLiked ? '已点赞' : '点赞' }} {{ post.likeCount || 0 }}
              </el-button>
              <el-button text @click="openMusic(post.musicId)">
                查看歌曲 {{ post.replyCount || 0 }} 条回复
              </el-button>
            </div>
          </div>
        </article>
      </template>

      <el-empty v-else description="社区里还没有可展示的动态" />
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 30, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadCommunity"
        @current-change="loadCommunity"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/format'
import { getCommunityFeed, toggleCommentLike, type CommunityPostVO } from '@/api/feedback'

const router = useRouter()

const defaultCover = 'https://via.placeholder.com/160x160?text=Music'
const loading = ref(false)
const keyword = ref('')
const sortType = ref<'latest' | 'hot'>('latest')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const postList = ref<CommunityPostVO[]>([])

const loadCommunity = async () => {
  loading.value = true
  try {
    const result = await getCommunityFeed({
      keyword: keyword.value || undefined,
      sortType: sortType.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    postList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载社区内容失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadCommunity()
}

const handleSortChange = () => {
  pageNum.value = 1
  loadCommunity()
}

const handlePostLike = async (post: CommunityPostVO) => {
  try {
    const result = await toggleCommentLike(post.id)
    post.isLiked = result.isLiked
    post.likeCount = Math.max((post.likeCount || 0) + (result.isLiked ? 1 : -1), 0)
  } catch (error) {
    console.error('社区点赞失败', error)
  }
}

const openMusic = (musicId?: number) => {
  if (!musicId) {
    ElMessage.warning('该动态暂未关联歌曲')
    return
  }
  router.push(`/music/${musicId}`)
}

onMounted(() => {
  loadCommunity()
})
</script>

<style scoped lang="scss">
.community-page {
  max-width: 1600px;
  margin: 0 auto;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 24px;
  padding: 32px;
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at top left, rgba(0, 195, 255, 0.16), transparent 45%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 250, 252, 0.92) 100%);
}

.page-title {
  font-size: 34px;
  font-weight: 700;
  margin-bottom: 10px;
}

.page-subtitle {
  color: var(--text-gray);
  margin: 0;
  max-width: 720px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.feed {
  display: grid;
  gap: 20px;
  min-height: 320px;
}

.post-card {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 20px;
  padding: 20px;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: var(--shadow-sm);

  .post-cover {
    cursor: pointer;

    img {
      width: 100%;
      aspect-ratio: 1;
      border-radius: var(--radius-lg);
      object-fit: cover;
    }
  }

  .post-meta {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .author {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .author-name {
    font-weight: 600;
    color: var(--text-dark);
  }

  .post-time,
  .music-line {
    color: var(--text-gray);
    font-size: 13px;
  }

  .post-content {
    margin: 0 0 16px;
    font-size: 15px;
    line-height: 1.8;
    color: var(--text-dark);
    white-space: pre-wrap;
    word-break: break-word;
  }

  .music-line {
    margin-bottom: 12px;
  }

  .post-actions {
    display: flex;
    gap: 12px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .post-card {
    grid-template-columns: 1fr;
  }
}
</style>
