<template>
  <footer class="app-footer">
    <div class="footer-content">
      <p>&copy; 2024 MusicAI 个性化音乐推荐系统. All rights reserved.</p>
      <p>Version 1.0.0</p>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.app-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--footer-height);
  background: var(--bg-white);
  border-top: 1px solid var(--border-color);
  z-index: 999;
  
  .footer-content {
    max-width: 1920px;
    margin: 0 auto;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    font-size: 12px;
    color: var(--text-gray);
  }
}
</style>

