<template>
  <div class="nextui-input-wrapper" :class="[`input-${size}`, `input-${variant}`]">
    <label v-if="label" class="input-label">{{ label }}</label>
    <div class="input-container" :class="{ 'input-error': hasError, 'input-disabled': disabled, 'input-focus': isFocused }">
      <span v-if="$slots.prefix || prefixIcon" class="input-prefix">
        <slot name="prefix">
          <el-icon v-if="prefixIcon"><component :is="prefixIcon" /></el-icon>
        </slot>
      </span>
      
      <textarea
        v-if="type === 'textarea'"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :rows="rows"
        class="nextui-input textarea"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
      />
      <input
        v-else
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        class="nextui-input"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
      />
      
      <span v-if="clearable && modelValue && !disabled" class="input-suffix input-clear" @click="handleClear">
        <el-icon><Close /></el-icon>
      </span>
      <span v-else-if="$slots.suffix || suffixIcon" class="input-suffix">
        <slot name="suffix">
          <el-icon v-if="suffixIcon"><component :is="suffixIcon" /></el-icon>
        </slot>
      </span>
    </div>
    
    <div v-if="hasError" class="input-error-message">
      {{ error }}
    </div>
    <div v-if="hint && !hasError" class="input-hint">
      {{ hint }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Close } from '@element-plus/icons-vue'

interface Props {
  modelValue: string | number
  type?: 'text' | 'password' | 'number' | 'email' | 'textarea'
  placeholder?: string
  label?: string
  error?: string | boolean
  hint?: string
  disabled?: boolean
  readonly?: boolean
  clearable?: boolean
  prefixIcon?: string | any
  suffixIcon?: string | any
  size?: 'sm' | 'md' | 'lg'
  variant?: 'default' | 'bordered' | 'underlined'
  rows?: number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  placeholder: '',
  label: '',
  error: false,
  hint: '',
  disabled: false,
  readonly: false,
  clearable: false,
  size: 'md',
  variant: 'default',
  rows: 4
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
  clear: []
}>()

const isFocused = ref(false)

const hasError = computed(() => {
  return typeof props.error === 'string' ? !!props.error : props.error === true
})

const inputType = computed(() => {
  if (props.type === 'password') return 'password'
  if (props.type === 'number') return 'number'
  if (props.type === 'email') return 'email'
  return 'text'
})

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false
  emit('blur', event)
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/nextui-mixins.scss';

.nextui-input-wrapper {
  width: 100%;
  
  .input-label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-dark);
    margin-bottom: 8px;
  }
  
  .input-container {
    position: relative;
    display: flex;
    align-items: center;
    @include nextui-input;
    padding: 0;
    background: transparent;
    border: none;
    
    &.input-focus {
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
    
    &.input-error {
      border-color: #ef4444;
      
      &.input-focus {
        box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
      }
    }
    
    &.input-disabled {
      opacity: 0.6;
      cursor: not-allowed;
      background: rgba(0, 0, 0, 0.02);
    }
    
    .input-prefix,
    .input-suffix {
      display: flex;
      align-items: center;
      padding: 0 12px;
      color: var(--text-gray);
      flex-shrink: 0;
    }
    
    .input-prefix {
      border-right: 1px solid rgba(0, 0, 0, 0.1);
    }
    
    .input-suffix {
      border-left: 1px solid rgba(0, 0, 0, 0.1);
    }
    
    .input-clear {
      cursor: pointer;
      transition: color 0.2s;
      
      &:hover {
        color: var(--text-dark);
      }
    }
    
    .nextui-input {
      flex: 1;
      border: none;
      outline: none;
      background: transparent;
      padding: 12px 16px;
      font-size: 14px;
      color: var(--text-dark);
      width: 100%;
      
      &::placeholder {
        color: rgba(0, 0, 0, 0.4);
      }
      
      &:disabled {
        cursor: not-allowed;
      }
      
      &.textarea {
        resize: vertical;
        min-height: 80px;
        padding: 12px 16px;
        font-family: inherit;
      }
    }
  }
  
  &.input-sm {
    .input-container .nextui-input {
      padding: 8px 12px;
      font-size: 13px;
    }
  }
  
  &.input-lg {
    .input-container .nextui-input {
      padding: 16px 20px;
      font-size: 16px;
    }
  }
  
  &.input-bordered {
    .input-container {
      border-width: 2px;
    }
  }
  
  &.input-underlined {
    .input-container {
      border: none;
      border-bottom: 2px solid rgba(0, 0, 0, 0.1);
      border-radius: 0;
      
      &.input-focus {
        border-bottom-color: #667eea;
        box-shadow: none;
      }
    }
  }
  
  .input-error-message {
    margin-top: 6px;
    font-size: 12px;
    color: #ef4444;
  }
  
  .input-hint {
    margin-top: 6px;
    font-size: 12px;
    color: var(--text-gray);
  }
}

[data-theme="dark"] {
  .nextui-input-wrapper {
    .input-label {
      color: var(--text-light);
    }
    
    .input-container {
      background: rgba(30, 41, 59, 0.9);
      border-color: rgba(255, 255, 255, 0.1);
      
      .nextui-input {
        color: #f1f5f9;
        
        &::placeholder {
          color: rgba(255, 255, 255, 0.4);
        }
      }
      
      .input-prefix,
      .input-suffix {
        border-color: rgba(255, 255, 255, 0.1);
      }
    }
  }
}
</style>

