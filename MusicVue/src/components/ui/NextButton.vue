<template>
  <button
    class="nextui-button"
    :class="[
      `button-${variant}`,
      `button-${size}`,
      { 'button-loading': loading, 'button-disabled': disabled }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="button-spinner"></span>
    <span v-if="$slots.icon" class="button-icon">
      <slot name="icon" />
    </span>
    <span class="button-content">
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'flat'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const handleClick = (event: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/nextui-mixins.scss';

.nextui-button {
  @include nextui-button;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: inherit;
  
  &.button-primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  }
  
  &.button-secondary {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    box-shadow: 0 4px 16px rgba(79, 172, 254, 0.4);
  }
  
  &.button-success {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
  }
  
  &.button-warning {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4);
  }
  
  &.button-danger {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    box-shadow: 0 4px 16px rgba(239, 68, 68, 0.4);
  }
  
  &.button-flat {
    background: rgba(102, 126, 234, 0.1);
    color: #667eea;
    box-shadow: none;
    
    &:hover {
      background: rgba(102, 126, 234, 0.2);
      transform: none;
    }
  }
  
  &.button-sm {
    padding: 8px 16px;
    font-size: 13px;
  }
  
  &.button-md {
    padding: 12px 24px;
    font-size: 14px;
  }
  
  &.button-lg {
    padding: 16px 32px;
    font-size: 16px;
  }
  
  .button-spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top-color: white;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }
  
  .button-icon {
    display: flex;
    align-items: center;
  }
  
  .button-content {
    flex: 1;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

[data-theme="dark"] {
  .nextui-button.button-flat {
    background: rgba(102, 126, 234, 0.2);
    color: #a5b4fc;
  }
}
</style>

