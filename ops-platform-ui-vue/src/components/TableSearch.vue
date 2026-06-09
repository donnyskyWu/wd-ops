<template>
  <div class="table-search">
    <el-form :inline="true" :model="searchModel" class="search-form">
      <slot />
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
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
  // 清空搜索条件
  const keys = Object.keys(searchModel.value)
  keys.forEach((key) => {
    searchModel.value[key] = undefined
  })
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
    :deep(.el-form-item) {
      margin-bottom: 8px;
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
}
</style>
