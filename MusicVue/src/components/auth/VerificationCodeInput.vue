<template>
  <div class="verification-code-input">
    <div
      v-for="(item, index) in codeArray"
      :key="index"
      class="code-input-item"
      :class="{ active: currentIndex === index, filled: item !== '' }"
    >
      <input
        :ref="el => inputRefs[index] = el"
        v-model="codeArray[index]"
        type="text"
        maxlength="1"
        class="code-input"
        @input="handleInput(index, $event)"
        @keydown="handleKeydown(index, $event)"
        @focus="currentIndex = index"
      />
      <div class="input-border"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'

const props = defineProps<{
  modelValue: string
  length?: number
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  complete: [code: string]
}>()

const codeLength = props.length || 6
const codeArray = ref<string[]>(Array(codeLength).fill(''))
const inputRefs = ref<(HTMLInputElement | null)[]>([])
const currentIndex = ref(0)

watch(() => props.modelValue, (newVal) => {
  if (newVal.length <= codeLength) {
    const arr = newVal.split('').concat(Array(codeLength - newVal.length).fill(''))
    codeArray.value = arr.slice(0, codeLength)
  }
}, { immediate: true })

watch(codeArray, (newVal) => {
  const code = newVal.join('')
  emit('update:modelValue', code)
  
  if (code.length === codeLength) {
    emit('complete', code)
  }
}, { deep: true })

const handleInput = (index: number, event: Event) => {
  const target = event.target as HTMLInputElement
  const value = target.value.replace(/[^0-9]/g, '')
  
  codeArray.value[index] = value
  
  if (value && index < codeLength - 1) {
    nextTick(() => {
      inputRefs.value[index + 1]?.focus()
    })
  }
}

const handleKeydown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Backspace' && !codeArray.value[index] && index > 0) {
    inputRefs.value[index - 1]?.focus()
  }
}

onMounted(() => {
  // 自动聚焦第一个输入框
  nextTick(() => {
    inputRefs.value[0]?.focus()
  })
})
</script>

<style scoped lang="scss">
.verification-code-input {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin: 32px 0;
  
  .code-input-item {
    position: relative;
    width: 56px;
    height: 72px;
    
    .code-input {
      width: 100%;
      height: 100%;
      border: 2px solid #e2e8f0;
      border-radius: 12px;
      text-align: center;
      font-size: 28px;
      font-weight: 600;
      color: #1a202c;
      background: white;
      transition: all 0.3s ease;
      
      &:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
      }
    }
    
    &.filled {
      .code-input {
        border-color: #667eea;
        background: rgba(102, 126, 234, 0.05);
      }
    }
    
    &.active {
      .code-input {
        border-color: #667eea;
        box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.2);
      }
    }
  }
}

[data-theme="dark"] {
  .verification-code-input {
    .code-input-item {
      .code-input {
        background: rgba(45, 55, 72, 0.8);
        border-color: rgba(255, 255, 255, 0.1);
        color: #f7fafc;
      }
      
      &.filled .code-input {
        background: rgba(102, 126, 234, 0.2);
      }
    }
  }
}
</style>

