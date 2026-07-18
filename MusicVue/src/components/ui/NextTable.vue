<template>
  <div class="nextui-table-wrapper">
    <!-- 加载状态 -->
    <div v-if="loading" class="table-loading">
      <el-skeleton :rows="5" animated />
    </div>
    
    <!-- 表格 -->
    <div v-else class="nextui-table-container">
      <table class="nextui-table" :class="{ 'table-stripe': stripe, 'table-border': border, 'table-hover': hover }">
        <!-- 表头 -->
        <thead>
          <tr>
            <th v-if="selection" class="table-checkbox-col">
              <el-checkbox
                :model-value="isAllSelected"
                :indeterminate="isIndeterminate"
                @change="handleSelectAll"
              />
            </th>
            <th
              v-for="column in columns"
              :key="column.prop"
              :style="{ width: column.width }"
              :class="{ 'table-sortable': column.sortable, 'table-filterable': column.filterable }"
            >
              <div class="table-header-cell">
                <span>{{ column.label }}</span>
                <span v-if="column.sortable" class="table-sort-icons">
                  <el-icon
                    :class="{ active: sortProp === column.prop && sortOrder === 'asc' }"
                    @click="handleSort(column.prop, 'asc')"
                  >
                    <ArrowUp />
                  </el-icon>
                  <el-icon
                    :class="{ active: sortProp === column.prop && sortOrder === 'desc' }"
                    @click="handleSort(column.prop, 'desc')"
                  >
                    <ArrowDown />
                  </el-icon>
                </span>
              </div>
            </th>
          </tr>
        </thead>
        
        <!-- 表体 -->
        <tbody>
          <tr
            v-for="(row, rowIndex) in paginatedData"
            :key="getRowKey(row, rowIndex)"
            :class="{ 'table-row-selected': isRowSelected(row) }"
            @click="handleRowClick(row)"
          >
            <td v-if="selection" class="table-checkbox-col">
              <el-checkbox
                :model-value="isRowSelected(row)"
                @change="handleSelectRow(row, $event)"
              />
            </td>
            <td
              v-for="column in columns"
              :key="column.prop"
            >
              <slot
                :name="`column-${column.prop}`"
                :row="row"
                :column="column"
                :value="getCellValue(row, column)"
              >
                <span v-if="column.formatter">
                  {{ column.formatter(row) }}
                </span>
                <component
                  v-else-if="column.render"
                  :is="column.render"
                  :row="row"
                  :column="column"
                />
                <span v-else>
                  {{ getCellValue(row, column) }}
                </span>
              </slot>
            </td>
          </tr>
          
          <!-- 空数据 -->
          <tr v-if="paginatedData.length === 0">
            <td :colspan="columns.length + (selection ? 1 : 0)" class="table-empty">
              <el-empty description="暂无数据" :image-size="100" />
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 分页 -->
      <div v-if="pagination" class="table-pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="typeof pagination === 'object' && pagination.total ? pagination.total : filteredData.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

export interface TableColumn {
  prop: string
  label: string
  width?: string | number
  sortable?: boolean
  filterable?: boolean
  formatter?: (row: any) => string
  render?: (props: { row: any; column: TableColumn }) => any
}

interface Props {
  data: any[]
  columns: TableColumn[]
  loading?: boolean
  stripe?: boolean
  border?: boolean
  hover?: boolean
  pagination?: boolean | {
    pageSize?: number
    currentPage?: number
    total?: number
  }
  selection?: boolean
  rowKey?: string | ((row: any) => string)
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  stripe: true,
  border: false,
  hover: true,
  pagination: false,
  selection: false,
  rowKey: 'id'
})

const emit = defineEmits<{
  'row-click': [row: any]
  'selection-change': [selection: any[]]
  'sort-change': [sortProp: string, sortOrder: 'asc' | 'desc' | null]
  'size-change': [size: number]
  'current-change': [page: number]
}>()

const selectedRows = ref<any[]>([])
const sortProp = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc' | null>(null)

// 分页状态：如果pagination是对象，使用外部控制，否则使用内部状态
const currentPage = computed({
  get: () => {
    if (typeof props.pagination === 'object' && props.pagination?.currentPage !== undefined) {
      return props.pagination.currentPage
    }
    return internalCurrentPage.value
  },
  set: (val) => {
    if (typeof props.pagination === 'object' && props.pagination?.currentPage !== undefined) {
      emit('current-change', val)
    } else {
      internalCurrentPage.value = val
    }
  }
})

const pageSize = computed({
  get: () => {
    if (typeof props.pagination === 'object' && props.pagination?.pageSize !== undefined) {
      return props.pagination.pageSize
    }
    return internalPageSize.value
  },
  set: (val) => {
    if (typeof props.pagination === 'object' && props.pagination?.pageSize !== undefined) {
      emit('size-change', val)
    } else {
      internalPageSize.value = val
    }
  }
})

const internalCurrentPage = ref(1)
const internalPageSize = ref(10)

// 获取行键
const getRowKey = (row: any, index: number): string => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey(row)
  }
  return row[props.rowKey] || `row-${index}`
}

// 获取单元格值
const getCellValue = (row: any, column: TableColumn) => {
  const keys = column.prop.split('.')
  let value = row
  for (const key of keys) {
    value = value?.[key]
  }
  return value ?? ''
}

// 排序
const handleSort = (prop: string, order: 'asc' | 'desc') => {
  if (sortProp.value === prop && sortOrder.value === order) {
    // 取消排序
    sortProp.value = null
    sortOrder.value = null
    emit('sort-change', prop, null)
  } else {
    sortProp.value = prop
    sortOrder.value = order
    emit('sort-change', prop, order)
  }
}

// 排序后的数据
const sortedData = computed(() => {
  if (!sortProp.value || !sortOrder.value) {
    return props.data
  }
  
  const sorted = [...props.data].sort((a, b) => {
    const aVal = getCellValue(a, { prop: sortProp.value! } as TableColumn)
    const bVal = getCellValue(b, { prop: sortProp.value! } as TableColumn)
    
    if (sortOrder.value === 'asc') {
      return aVal > bVal ? 1 : aVal < bVal ? -1 : 0
    } else {
      return aVal < bVal ? 1 : aVal > bVal ? -1 : 0
    }
  })
  
  return sorted
})

// 分页数据
const paginatedData = computed(() => {
  if (!props.pagination) {
    return sortedData.value
  }
  
  // 如果使用外部分页控制，直接返回所有数据（由外部处理分页）
  if (typeof props.pagination === 'object' && props.pagination.currentPage !== undefined) {
    return sortedData.value
  }
  
  // 内部分页处理
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return sortedData.value.slice(start, end)
})

// 过滤后的数据（用于分页总数）
const filteredData = computed(() => {
  // 如果使用外部分页，使用外部传入的total
  if (typeof props.pagination === 'object' && props.pagination.total !== undefined) {
    return []
  }
  return sortedData.value
})

// 选择相关
const isRowSelected = (row: any) => {
  return selectedRows.value.some(r => getRowKey(r, 0) === getRowKey(row, 0))
}

const isAllSelected = computed(() => {
  return paginatedData.value.length > 0 && 
         paginatedData.value.every(row => isRowSelected(row))
})

const isIndeterminate = computed(() => {
  const selectedCount = paginatedData.value.filter(row => isRowSelected(row)).length
  return selectedCount > 0 && selectedCount < paginatedData.value.length
})

const handleSelectRow = (row: any, checked: boolean) => {
  if (checked) {
    if (!isRowSelected(row)) {
      selectedRows.value.push(row)
    }
  } else {
    const index = selectedRows.value.findIndex(r => getRowKey(r, 0) === getRowKey(row, 0))
    if (index > -1) {
      selectedRows.value.splice(index, 1)
    }
  }
  emit('selection-change', selectedRows.value)
}

const handleSelectAll = (checked: boolean) => {
  if (checked) {
    paginatedData.value.forEach(row => {
      if (!isRowSelected(row)) {
        selectedRows.value.push(row)
      }
    })
  } else {
    paginatedData.value.forEach(row => {
      const index = selectedRows.value.findIndex(r => getRowKey(r, 0) === getRowKey(row, 0))
      if (index > -1) {
        selectedRows.value.splice(index, 1)
      }
    })
  }
  emit('selection-change', selectedRows.value)
}

// 行点击
const handleRowClick = (row: any) => {
  emit('row-click', row)
}

// 分页
const handleSizeChange = (size: number) => {
  if (typeof props.pagination === 'object' && props.pagination?.pageSize !== undefined) {
    emit('size-change', size)
  } else {
    internalPageSize.value = size
    internalCurrentPage.value = 1
  }
}

const handlePageChange = (page: number) => {
  if (typeof props.pagination === 'object' && props.pagination?.currentPage !== undefined) {
    emit('current-change', page)
  } else {
    internalCurrentPage.value = page
  }
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/nextui-mixins.scss';

.nextui-table-wrapper {
  width: 100%;
}

.nextui-table-container {
  @include nextui-card;
  padding: 0;
  overflow: hidden;
}

.nextui-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  
  thead {
    background: rgba(0, 0, 0, 0.02);
    
    th {
      padding: 16px;
      text-align: left;
      font-weight: 600;
      color: var(--text-dark);
      border-bottom: 2px solid rgba(0, 0, 0, 0.1);
      
      .table-header-cell {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
      }
      
      .table-sort-icons {
        display: flex;
        flex-direction: column;
        gap: 2px;
        cursor: pointer;
        color: var(--text-gray);
        
        .el-icon {
          font-size: 12px;
          transition: color 0.2s;
          
          &:hover {
            color: var(--text-dark);
          }
          
          &.active {
            color: #667eea;
          }
        }
      }
    }
  }
  
  tbody {
    tr {
      transition: all 0.2s;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      
      &:hover {
        background: rgba(102, 126, 234, 0.05);
      }
      
      &.table-row-selected {
        background: rgba(102, 126, 234, 0.1);
      }
      
      td {
        padding: 16px;
        color: var(--text-dark);
      }
      
      .table-empty {
        text-align: center;
        padding: 48px;
      }
    }
  }
  
  &.table-stripe {
    tbody tr:nth-child(even) {
      background: rgba(0, 0, 0, 0.02);
    }
  }
  
  &.table-border {
    border: 1px solid rgba(0, 0, 0, 0.1);
    
    th,
    td {
      border-right: 1px solid rgba(0, 0, 0, 0.1);
      
      &:last-child {
        border-right: none;
      }
    }
  }
  
  .table-checkbox-col {
    width: 50px;
    text-align: center;
  }
}

.table-loading {
  padding: 24px;
}

.table-pagination {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: flex-end;
}

[data-theme="dark"] {
  .nextui-table {
    thead {
      background: rgba(255, 255, 255, 0.05);
      
      th {
        border-bottom-color: rgba(255, 255, 255, 0.1);
        color: var(--text-light);
      }
    }
    
    tbody {
      tr {
        border-bottom-color: rgba(255, 255, 255, 0.05);
        
        &:hover {
          background: rgba(102, 126, 234, 0.1);
        }
        
        td {
          color: var(--text-light);
        }
      }
    }
    
    &.table-stripe {
      tbody tr:nth-child(even) {
        background: rgba(255, 255, 255, 0.03);
      }
    }
  }
  
  .table-pagination {
    border-top-color: rgba(255, 255, 255, 0.1);
  }
}
</style>

