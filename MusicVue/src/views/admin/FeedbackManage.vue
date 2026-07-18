<template>
  <div class="feedback-manage-page">
    <h2 class="page-title text-gradient">{{ pageTitle }}</h2>

    <NextCard variant="shadow" hover class="search-card">
      <div class="search-actions">
        <NextInput
          v-model="searchKeyword"
          :placeholder="searchPlaceholder"
          :prefix-icon="Search"
          clearable
          style="width: 320px"
          @keyup.enter="handleSearch"
        />

        <el-select
          v-if="!isCommunityMode"
          v-model="filterType"
          placeholder="类型"
          clearable
          style="width: 160px"
          @change="handleSearch"
        >
          <el-option label="点赞" value="LIKE" />
          <el-option label="差评" value="DISLIKE" />
          <el-option label="评论" value="COMMENT" />
          <el-option label="收藏" value="FAVORITE" />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 300px"
          @change="handleSearch"
        />

        <NextButton variant="primary" :icon="Download" @click="handleExport">
          导出数据
        </NextButton>
      </div>
    </NextCard>

    <NextCard variant="shadow" hover class="table-container">
      <el-table :data="feedbackList" v-loading="loading" class="custom-table">
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column label="用户" width="170">
          <template #default="{ row }">
            <div class="user-info">
              <span class="username">{{ row.username || '未知用户' }}</span>
              <span class="nickname" v-if="row.nickname">({{ row.nickname }})</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="关联歌曲" min-width="220">
          <template #default="{ row }">
            <div v-if="row.musicTitle" class="music-info">
              <div class="music-title">{{ row.musicTitle }}</div>
              <div class="music-artist" v-if="row.musicArtist">{{ row.musicArtist }}</div>
            </div>
            <span v-else class="text-gray">无</span>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />

        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="right" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="查看详情" placement="top">
                <el-button circle :icon="View" @click="handleView(row)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button circle :icon="Delete" type="danger" @click="handleDelete(row.id)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadFeedback"
          @current-change="loadFeedback"
        />
      </div>
    </NextCard>

    <NextModal
      v-model="detailDialogVisible"
      :title="isCommunityMode ? '社区内容详情' : '反馈详情'"
      size="lg"
      class="detail-dialog"
    >
      <div v-if="currentFeedback" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentFeedback.id }}</el-descriptions-item>
          <el-descriptions-item label="用户">
            {{ currentFeedback.username || '未知用户' }}
            <span v-if="currentFeedback.nickname">({{ currentFeedback.nickname }})</span>
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getTypeTagType(currentFeedback.type)">
              {{ getTypeText(currentFeedback.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDate(currentFeedback.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="关联歌曲" :span="2" v-if="currentFeedback.musicTitle">
            {{ currentFeedback.musicTitle }}<span v-if="currentFeedback.musicArtist"> - {{ currentFeedback.musicArtist }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="内容" :span="2" v-if="currentFeedback.content">
            <div class="content-text">{{ currentFeedback.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </NextModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Download, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NextButton from '@/components/ui/NextButton.vue'
import NextCard from '@/components/ui/NextCard.vue'
import NextInput from '@/components/ui/NextInput.vue'
import NextModal from '@/components/ui/NextModal.vue'
import request from '@/api/request'
import { formatDate } from '@/utils/format'

const route = useRoute()

const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const feedbackList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailDialogVisible = ref(false)
const currentFeedback = ref<any>(null)

const isCommunityMode = computed(() => route.meta.feedbackMode === 'community')
const pageTitle = computed(() => isCommunityMode.value ? '社区管理' : '反馈管理')
const searchPlaceholder = computed(() => {
  return isCommunityMode.value
    ? '搜索社区评论、用户或歌曲'
    : '搜索反馈内容、用户或歌曲'
})

const buildParams = () => {
  const params: Record<string, any> = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }

  if (searchKeyword.value) params.keyword = searchKeyword.value

  const type = isCommunityMode.value ? 'COMMENT' : filterType.value
  if (type) params.type = type

  if (dateRange.value?.length === 2) {
    params.startDate = formatDate(dateRange.value[0], 'YYYY-MM-DD')
    params.endDate = formatDate(dateRange.value[1], 'YYYY-MM-DD')
  }

  return params
}

const loadFeedback = async () => {
  loading.value = true
  try {
    const result: any = await request.get('/admin/feedback', { params: buildParams() })
    feedbackList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadFeedback()
}

const handleView = (row: any) => {
  currentFeedback.value = row
  detailDialogVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除这条记录吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/admin/feedback/${id}`)
    ElMessage.success('删除成功')
    loadFeedback()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleExport = () => {
  ElMessage.info('导出功能待补充')
}

const getTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    LIKE: 'success',
    DISLIKE: 'danger',
    COMMENT: 'info',
    FAVORITE: 'warning'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    LIKE: '点赞',
    DISLIKE: '差评',
    COMMENT: '评论',
    FAVORITE: '收藏'
  }
  return typeMap[type] || type
}

watch(() => route.fullPath, () => {
  filterType.value = ''
  searchKeyword.value = ''
  dateRange.value = null
  pageNum.value = 1
  loadFeedback()
})

onMounted(() => {
  loadFeedback()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.feedback-manage-page {
  max-width: 1600px;
  margin: 0 auto;

  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 24px;
  }
}

.search-card {
  margin-bottom: 24px;
}

.search-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.table-container {
  padding: 20px;
}

.user-info,
.music-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username,
.music-title {
  font-weight: 600;
  color: var(--text-dark);
}

.nickname,
.music-artist,
.text-gray {
  font-size: 12px;
  color: var(--text-gray);
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
}

.content-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}
</style>
