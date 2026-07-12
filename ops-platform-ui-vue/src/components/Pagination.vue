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

import { computed } from 'vue'



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

  align-items: center;

  width: 100%;

  flex-wrap: wrap;

  gap: 8px;

  margin-top: 16px;

  padding: 12px 16px;

  background-color: var(--el-bg-color);

  border: 1px solid var(--el-border-color-lighter);

  border-radius: 12px;

  box-shadow: var(--el-box-shadow-light);



  :deep(.el-pagination) {

    display: flex;

    flex-wrap: wrap;

    align-items: center;

    width: auto;

    max-width: 100%;

    white-space: nowrap;

    font-weight: 500;

    color: var(--el-text-color-primary);



    .el-pager {

      display: inline-flex;

    }



    .btn-prev,

    .btn-next {

      border-radius: 6px;

      transition: all 0.3s;

      background-color: var(--el-fill-color);



      &:hover {

        color: var(--el-color-primary);

      }

    }



    .el-pager li {

      border-radius: 6px;

      transition: all 0.3s;

      font-weight: 500;

      background-color: var(--el-fill-color);



      &:hover {

        color: var(--el-color-primary);

      }



      &.is-active {

        background-color: var(--el-color-primary);

        color: var(--el-color-white);



        &:hover {

          background-color: var(--el-color-primary-light-3);

        }

      }

    }



    .el-pagination__total {

      color: var(--el-text-color-secondary);

      font-size: 13px;

    }



    .el-pagination__sizes {

      .el-input__wrapper {

        border-radius: 6px;

      }

    }



    .el-pagination__jump {

      color: var(--el-text-color-secondary);

      font-size: 13px;

    }

  }

}

</style>


