<template>
  <div class="table-search">
    <el-form :inline="true" :model="searchModel" class="search-form" @submit.prevent="handleSearch">
      <div class="search-fields">
        <slot />
      </div>
      <div class="search-actions">
        <slot name="extra" />
        <el-form-item class="search-btns">
          <el-button type="primary" native-type="submit">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

interface Props {
  /** 搜索模型 */
  modelValue?: Record<string, any>
}

interface Emits {
  (e: 'update:modelValue', value: Record<string, any>): void
  (e: 'search'): void
  (e: 'reset'): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => ({}),
})

const emit = defineEmits<Emits>()

const searchModel = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  emit('reset')
}
</script>

<style scoped lang="scss">
.table-search {
  margin-bottom: 16px;
  background-color: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .search-form {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    justify-content: space-between;
    gap: 4px 16px;
  }

  .search-fields {
    display: flex;
    flex-wrap: wrap;
    flex: 1 1 auto;
    align-items: flex-end;
    gap: 0 4px;
    min-width: 0;
  }

  .search-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    flex-shrink: 0;
    gap: 0 8px;
    margin-left: auto;
  }

  :deep(.el-form-item) {
    margin-bottom: 8px;
    margin-right: 12px;
  }

  :deep(.search-fields .el-form-item) {
    min-width: 200px;
  }

  :deep(.search-fields .el-date-editor--daterange) {
    width: 260px !important;
  }

  :deep(.search-fields .el-input),
  :deep(.search-fields .el-select) {
    width: 180px;
  }

  :deep(.search-btns) {
    margin-right: 0;
    margin-bottom: 8px;

    .el-form-item__content {
      display: flex;
      gap: 8px;
      flex-wrap: nowrap;
    }
  }

  :deep(.el-form-item__label) {
    font-size: 14px;
    color: #606266;
  }

  :deep(.el-input__wrapper) {
    border-radius: 6px;
    transition: all 0.3s;

    &:hover {
      box-shadow: none;
    }

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
    }
  }

  :deep(.el-button--primary) {
    background-color: #1890ff;
    border-color: #1890ff;
    border-radius: 6px;

    &:hover {
      background-color: #40a9ff;
      border-color: #40a9ff;
    }
  }

  :deep(.el-button:not(.is-text-button)) {
    border-radius: 6px;
    font-weight: 500;
  }
}
</style>
