<template>
  <div class="table-search">
    <el-form :inline="true" :model="searchModel" class="search-form" @submit.prevent="handleSearch">
      <div v-if="$slots.top" class="search-top">
        <slot name="top" />
      </div>
      <div class="search-body">
        <div class="search-fields">
          <slot />
        </div>
        <div class="search-actions">
          <div v-if="$slots.extra" class="search-actions-extra">
            <slot name="extra" />
          </div>
          <el-button type="primary" native-type="submit">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
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
    flex-direction: column;
    gap: 8px;
  }

  .search-top {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    width: 100%;
    padding-bottom: 4px;
    border-bottom: 1px solid #f0f2f5;
  }

  .search-body {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    justify-content: space-between;
    gap: 4px 16px;
    width: 100%;
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
    gap: 8px;
    margin-left: auto;
    margin-bottom: 8px;

    &-extra {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    :deep(.el-button) {
      margin: 0;
      vertical-align: middle;
    }
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

  :deep(.el-form-item__label) {
    font-size: 14px;
    color: #606266;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    border-radius: 6px;
    transition: all 0.3s;
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
