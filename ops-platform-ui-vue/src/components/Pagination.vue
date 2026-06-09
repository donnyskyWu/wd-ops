<template>
  <div class="pagination-wrap">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      :background="background"
      layout="total, sizes, prev, pager, next, jumper"
      @update:current-page="(val) => emit('update:currentPage', val)"
      @update:page-size="(val) => emit('update:pageSize', val)"
      @change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 总记录数 */
  total: number
  /** 当前页码 */
  currentPage?: number
  /** 每页条数 */
  pageSize?: number
  /** 是否带背景色 */
  background?: boolean
}

interface Emits {
  (e: 'update:currentPage', value: number): void
  (e: 'update:pageSize', value: number): void
  (e: 'change', page: number, size: number): void
}

const props = withDefaults(defineProps<Props>(), {
  currentPage: 1,
  pageSize: 10,
  background: true,
})

const emit = defineEmits<Emits>()

const currentPage = computed({
  get: () => props.currentPage,
  set: (val) => emit('update:currentPage', val),
})

const pageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val),
})

const handleChange = (page: number, size: number) => {
  emit('change', page, size)
}
</script>

<style scoped lang="scss">
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 12px 16px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  :deep(.el-pagination) {
    font-weight: 500;

    .btn-prev, .btn-next {
      border-radius: 6px;
      transition: all 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .el-pager li {
      border-radius: 6px;
      transition: all 0.3s;
      font-weight: 500;

      &:hover {
        color: #1890ff;
      }

      &.is-active {
        background-color: #1890ff;
        color: #ffffff;

        &:hover {
          background-color: #40a9ff;
        }
      }
    }

    .el-pagination__total {
      color: #909399;
      font-size: 13px;
    }

    .el-pagination__sizes {
      .el-input__wrapper {
        border-radius: 6px;
      }
    }

    .el-pagination__jump {
      color: #909399;
      font-size: 13px;
    }
  }
}
</style>