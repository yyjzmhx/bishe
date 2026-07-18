<template>
  <div class="personal-tag-display">
    <el-tag
      v-for="tag in tags"
      :key="tag.id"
      :style="{
        backgroundColor: tag.color + '20',
        borderColor: tag.color,
        color: tag.color
      }"
      effect="plain"
      size="small"
      class="personal-tag"
    >
      {{ tag.name }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import type { UserPersonalTag } from '@/api/userTag'

defineProps<{
  tags: UserPersonalTag[]
}>()
</script>

<style scoped lang="scss">
.personal-tag-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  
  .personal-tag {
    font-size: 12px;
    padding: 4px 12px;
    border-radius: 12px;
  }
}
</style>

