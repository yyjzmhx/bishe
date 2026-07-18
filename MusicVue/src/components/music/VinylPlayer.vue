<template>
  <div class="vinyl-player-container">
    <div class="turntable">
      <!-- 转盘背景 -->
      <div class="turntable-base"></div>
      
      <!-- 唱片 -->
      <div 
        class="vinyl-record"
        :class="{ 'playing': isPlaying }"
        @click="handleClick"
      >
        <div class="record-surface">
          <!-- 专辑封面 -->
          <div class="record-label">
            <img 
              :src="music.coverUrl || defaultCover" 
              :alt="music.title"
              class="cover-image"
            />
          </div>
        </div>
      </div>

      <!-- 唱臂 -->
      <div class="tonearm" :class="{ 'playing': isPlaying }">
        <div class="tonearm-base"></div>
        <div class="tonearm-arm"></div>
        <div class="tonearm-head">
          <div class="stylus"></div>
        </div>
      </div>

      <!-- 放大镜图标 -->
      <div class="zoom-icon">
        <el-icon><ZoomIn /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ZoomIn } from '@element-plus/icons-vue'
import type { Music } from '@/types/music'

const props = defineProps<{
  music: Music
  isPlaying: boolean
}>()

const emit = defineEmits<{
  play: []
  pause: []
}>()

const defaultCover = 'https://via.placeholder.com/300x300?text=Music'

const handleClick = () => {
  if (props.isPlaying) {
    emit('pause')
  } else {
    emit('play')
  }
}
</script>

<style scoped lang="scss">
.vinyl-player-container {
  width: 100%;
  max-width: 450px;
  aspect-ratio: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.turntable {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.turntable-base {
  width: 85%;
  height: 85%;
  background: radial-gradient(
    circle,
    #4a4a4a 0%,
    #3a3a3a 30%,
    #2d2d2d 60%,
    #1a1a1a 100%
  );
  border-radius: 50%;
  box-shadow: 
    inset 0 0 60px rgba(0, 0, 0, 0.9),
    inset 0 0 30px rgba(64, 158, 255, 0.1),
    0 15px 50px rgba(0, 0, 0, 0.6),
    0 0 80px rgba(64, 158, 255, 0.1);
  position: relative;
  border: 2px solid rgba(255, 255, 255, 0.05);

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 24px;
    height: 24px;
    background: radial-gradient(circle, #2d2d2d 0%, #1a1a1a 100%);
    border-radius: 50%;
    box-shadow: 
      inset 0 0 15px rgba(0, 0, 0, 0.9),
      0 0 20px rgba(64, 158, 255, 0.2);
    z-index: 2;
  }

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 60%;
    height: 60%;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.03);
    pointer-events: none;
  }
}

.vinyl-record {
  position: absolute;
  width: 75%;
  height: 75%;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: rotate 10s linear infinite;
  animation-play-state: paused;
  filter: drop-shadow(0 10px 30px rgba(0, 0, 0, 0.5));

  &.playing {
    animation-play-state: running;
    filter: drop-shadow(0 15px 40px rgba(64, 158, 255, 0.3));
  }

  &:hover {
    transform: scale(1.03);
    filter: drop-shadow(0 20px 50px rgba(64, 158, 255, 0.4));
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.record-surface {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: 
    radial-gradient(
      circle at 30% 30%,
      rgba(255, 255, 255, 0.4) 0%,
      transparent 50%
    ),
    linear-gradient(
      135deg,
      rgba(200, 200, 200, 0.35) 0%,
      rgba(150, 150, 150, 0.25) 20%,
      rgba(100, 100, 100, 0.15) 40%,
      rgba(80, 80, 80, 0.1) 50%,
      rgba(100, 100, 100, 0.15) 60%,
      rgba(150, 150, 150, 0.25) 80%,
      rgba(200, 200, 200, 0.35) 100%
    );
  position: relative;
  box-shadow: 
    inset 0 0 40px rgba(0, 0, 0, 0.6),
    inset 0 0 20px rgba(64, 158, 255, 0.1),
    0 8px 25px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 30%;
    height: 30%;
    border-radius: 50%;
    background: radial-gradient(
      circle,
      rgba(0, 0, 0, 0.4) 0%,
      rgba(0, 0, 0, 0.7) 100%
    );
    box-shadow: inset 0 0 15px rgba(0, 0, 0, 0.8);
  }
}

.record-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 35%;
  height: 35%;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 
    0 4px 15px rgba(0, 0, 0, 0.6),
    0 0 30px rgba(64, 158, 255, 0.2),
    inset 0 0 20px rgba(0, 0, 0, 0.3);
  border: 2px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s ease;
  z-index: 3;

  .vinyl-record.playing & {
    box-shadow: 
      0 4px 15px rgba(0, 0, 0, 0.6),
      0 0 40px rgba(64, 158, 255, 0.4),
      inset 0 0 20px rgba(0, 0, 0, 0.3);
  }

  .cover-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s ease;
  }

  .vinyl-record:hover & .cover-image {
    transform: scale(1.05);
  }
}

.tonearm {
  position: absolute;
  top: 20%;
  right: 15%;
  width: 40%;
  height: 40%;
  transform-origin: 20% 30%;
  transform: rotate(-25deg);
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
  z-index: 10;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.4));

  &.playing {
    transform: rotate(-15deg);
    filter: drop-shadow(0 4px 12px rgba(64, 158, 255, 0.3));
  }
}

.tonearm-base {
  position: absolute;
  top: 0;
  left: 0;
  width: 15%;
  height: 15%;
  background: linear-gradient(135deg, #3a3a3a 0%, #2d2d2d 100%);
  border-radius: 50%;
  box-shadow: 
    0 3px 8px rgba(0, 0, 0, 0.6),
    inset 0 1px 2px rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.tonearm-arm {
  position: absolute;
  top: 7%;
  left: 7%;
  width: 3px;
  height: 80%;
  background: linear-gradient(
    to bottom,
    #5a5a5a 0%,
    #4a4a4a 30%,
    #3a3a3a 60%,
    #2d2d2d 100%
  );
  box-shadow: 
    2px 0 4px rgba(0, 0, 0, 0.4),
    inset -1px 0 2px rgba(255, 255, 255, 0.05);
  transform-origin: top center;
  border-radius: 2px;
}

.tonearm-head {
  position: absolute;
  bottom: 0;
  left: -8px;
  width: 18px;
  height: 18px;
  background: #3a3a3a;
  border-radius: 50%;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}

.stylus {
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 8px;
  background: #1a1a1a;
  border-radius: 0 0 2px 2px;
}

.zoom-icon {
  position: absolute;
  bottom: 10%;
  right: 10%;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.9) 0%, rgba(64, 158, 255, 0.7) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
  box-shadow: 
    0 4px 15px rgba(64, 158, 255, 0.4),
    0 0 20px rgba(64, 158, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.2);

  &:hover {
    background: linear-gradient(135deg, rgba(64, 158, 255, 1) 0%, rgba(64, 158, 255, 0.9) 100%);
    transform: scale(1.15) rotate(90deg);
    box-shadow: 
      0 6px 20px rgba(64, 158, 255, 0.6),
      0 0 30px rgba(64, 158, 255, 0.4);
  }

  .el-icon {
    color: white;
    font-size: 18px;
    transition: transform 0.3s ease;
  }
}
</style>

