<template>
  <div class="ai-manage-page">
    <h2 class="page-title text-gradient">AI管理</h2>
    
    <el-tabs v-model="activeTab" type="border-card" class="ai-tabs">
      <!-- Tab 1: AI接入配置 -->
      <el-tab-pane label="AI接入配置" name="config">
        <AIConfigTab />
      </el-tab-pane>
      
      <!-- Tab 2: AI数据监控 -->
      <el-tab-pane label="AI数据监控" name="monitor">
        <AIMonitorTab />
      </el-tab-pane>
      
      <!-- Tab 3: 报告模板管理 -->
      <el-tab-pane label="报告模板管理" name="template">
        <ReportTemplateTab />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AIConfigTab from '@/components/admin/ai/AIConfigTab.vue'
import AIMonitorTab from '@/components/admin/ai/AIMonitorTab.vue'
import ReportTemplateTab from '@/components/admin/ai/ReportTemplateTab.vue'

const activeTab = ref('config')
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.ai-manage-page {
  max-width: 1600px;
  margin: 0 auto;
  
  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 24px;
    font-family: 'Roboto', sans-serif;
  }
  
  .ai-tabs {
    @include glass-card;
    padding: 0;
    
    :deep(.el-tabs__header) {
      margin: 0;
      background: rgba(255, 255, 255, 0.5);
      backdrop-filter: blur(10px);
    }
    
    :deep(.el-tabs__content) {
      padding: 24px;
    }
  }
}
</style>

