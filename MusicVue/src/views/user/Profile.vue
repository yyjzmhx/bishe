<template>
  <div class="profile-page-wrapper">
    <!-- 状态切换 -->
    <transition name="fade" mode="out-in">
      <component 
        :is="currentView" 
        @enterEdit="isEditing = true"
        @back="isEditing = false"
      />
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ProfileGalaxy from './ProfileGalaxy.vue'
import ProfileEdit from './ProfileEdit.vue'

const isEditing = ref(false)
const currentView = computed(() => isEditing.value ? ProfileEdit : ProfileGalaxy)
</script>

<style scoped>
.profile-page-wrapper {
  width: 100%;
  height: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
