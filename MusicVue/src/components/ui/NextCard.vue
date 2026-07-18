<template>
  <div 
    class="nextui-card" 
    :class="[`card-${variant}`, { 'card-hover': hover, 'card-3d': threeD }]"
    :style="cardStyle"
  >
    <div v-if="$slots.header" class="card-header">
      <slot name="header" />
    </div>
    
    <div class="card-body">
      <slot />
    </div>
    
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'default' | 'bordered' | 'flat' | 'shadow'
  hover?: boolean
  threeD?: boolean
  padding?: string
  radius?: string
  background?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  hover: true,
  threeD: false,
  padding: '24px',
  radius: '20px',
  background: undefined
})

const cardStyle = computed(() => {
  const style: Record<string, string> = {
    padding: props.padding,
    borderRadius: props.radius
  }
  
  if (props.background) {
    style.background = props.background
  }
  
  return style
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/nextui-mixins.scss';

.nextui-card {
  @include nextui-card;
  position: relative;
  overflow: hidden;
  
  &.card-bordered {
    border-width: 2px;
    border-color: rgba(102, 126, 234, 0.2);
  }
  
  &.card-flat {
    box-shadow: none;
    border: 1px solid rgba(0, 0, 0, 0.1);
  }
  
  &.card-shadow {
    @include nextui-shadow-xl;
  }
  
  &.card-3d {
    @include nextui-3d-hover;
  }
  
  &.card-hover {
    cursor: pointer;
  }
  
  .card-header {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  }
  
  .card-body {
    flex: 1;
  }
  
  .card-footer {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
  }
}

[data-theme="dark"] {
  .nextui-card {
    background: rgba(30, 41, 59, 0.8);
    border-color: rgba(255, 255, 255, 0.1);
    
    .card-header,
    .card-footer {
      border-color: rgba(255, 255, 255, 0.1);
    }
  }
}
</style>

