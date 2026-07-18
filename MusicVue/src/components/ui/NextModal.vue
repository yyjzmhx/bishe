<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div
        v-if="modelValue"
        class="nextui-modal-overlay"
        :style="{ zIndex: zIndex }"
        @click="handleMaskClick"
      >
        <Transition name="modal-scale">
          <div
            v-if="modelValue"
            class="nextui-modal"
            :class="[`modal-${size}`, { 'modal-centered': centered }]"
            :style="modalStyle"
            @click.stop
          >
            <!-- 头部 -->
            <div v-if="title || $slots.header || showClose" class="modal-header">
              <slot name="header">
                <h3 v-if="title" class="modal-title">{{ title }}</h3>
              </slot>
              <el-button
                v-if="showClose"
                text
                circle
                class="modal-close"
                @click="handleClose"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            
            <!-- 内容 -->
            <div class="modal-body">
              <slot />
            </div>
            
            <!-- 底部 -->
            <div v-if="$slots.footer" class="modal-footer">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'

interface Props {
  modelValue: boolean
  title?: string
  width?: string | number
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  closable?: boolean
  maskClosable?: boolean
  showClose?: boolean
  centered?: boolean
  zIndex?: number
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  width: undefined,
  size: 'md',
  closable: true,
  maskClosable: true,
  showClose: true,
  centered: true,
  zIndex: 2000
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

const modalStyle = computed(() => {
  const style: Record<string, string> = {}
  
  if (props.width) {
    style.width = typeof props.width === 'number' ? `${props.width}px` : props.width
  }
  
  return style
})

const handleClose = () => {
  if (props.closable) {
    emit('update:modelValue', false)
    emit('close')
  }
}

const handleMaskClick = () => {
  if (props.maskClosable) {
    handleClose()
  }
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/nextui-mixins.scss';

.nextui-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow-y: auto;
}

.nextui-modal {
  @include nextui-card;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px) saturate(180%);
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  @include nextui-shadow-xl;
  
  &.modal-sm {
    width: 400px;
  }
  
  &.modal-md {
    width: 600px;
  }
  
  &.modal-lg {
    width: 800px;
  }
  
  &.modal-xl {
    width: 1000px;
  }
  
  &.modal-full {
    width: 95vw;
    height: 95vh;
  }
  
  &.modal-centered {
    margin: auto;
  }
  
  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24px 24px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    
    .modal-title {
      font-size: 20px;
      font-weight: 700;
      color: var(--text-dark);
      margin: 0;
    }
    
    .modal-close {
      color: var(--text-gray);
      transition: all 0.2s;
      
      &:hover {
        color: var(--text-dark);
        background: rgba(0, 0, 0, 0.05);
      }
    }
  }
  
  .modal-body {
    flex: 1;
    padding: 24px;
    overflow-y: auto;
    min-height: 0;
  }
  
  .modal-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    padding: 16px 24px;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
  }
}

// 动画
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-scale-enter-active,
.modal-scale-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-scale-enter-from {
  opacity: 0;
  transform: scale(0.9) translateY(-20px);
}

.modal-scale-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(-20px);
}

[data-theme="dark"] {
  .nextui-modal {
    background: rgba(30, 41, 59, 0.95);
    
    .modal-header,
    .modal-footer {
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    .modal-title {
      color: var(--text-light);
    }
  }
  
  .nextui-modal-overlay {
    background: rgba(0, 0, 0, 0.7);
  }
}
</style>

