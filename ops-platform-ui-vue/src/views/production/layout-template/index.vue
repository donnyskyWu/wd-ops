<template>

  <div class="layout-template-page">

    <TableSearch v-model="searchForm" @search="loadData" @reset="resetSearch">

      <el-form-item label="模板名称">

        <el-input v-model="searchForm.templateName" clearable placeholder="模糊搜索" />

      </el-form-item>

      <el-form-item label="状态">

        <DictSelect v-model="searchForm.status" dict-type="dict_layout_template_status" clearable />

      </el-form-item>

      <el-form-item label="来源">

        <DictSelect v-model="searchForm.sourceType" dict-type="dict_layout_template_source" clearable />

      </el-form-item>

    </TableSearch>



    <div class="action-bar">

      <el-button v-hasPermi="'oa:layout-template:create'" type="primary" @click="router.push('/layout-template/create')">

        <el-icon><Plus /></el-icon>

        新建模板

      </el-button>

      <el-button v-hasPermi="'oa:layout-template:import'" @click="router.push('/layout-template/import')">导入向导</el-button>

    </div>



    <el-table v-loading="loading" :data="list" stripe>

      <el-table-column prop="templateName" label="模板名称" min-width="180" />

      <el-table-column prop="documentType" label="文档类型" width="140">

        <template #default="{ row }">

          <DictLabel

            v-if="row.documentType"

            dict-type="dict_document_type"

            :value="row.documentType"

          />

          <span v-else>通用</span>

        </template>

      </el-table-column>

      <el-table-column prop="sourceType" label="来源" width="120">

        <template #default="{ row }">

          <DictLabel dict-type="dict_layout_template_source" :value="row.sourceType" />

        </template>

      </el-table-column>

      <el-table-column prop="status" label="状态" width="100">

        <template #default="{ row }">

          <DictLabel dict-type="dict_layout_template_status" :value="row.status" />

        </template>

      </el-table-column>

      <el-table-column prop="creatorName" label="创建人" width="100" />

      <el-table-column prop="updateTime" label="更新时间" width="170" />

      <el-table-column label="操作" width="300" fixed="right">

        <template #default="{ row }">

          <el-button link type="primary" @click="router.push(`/layout-template/${row.id}`)">预览</el-button>

          <el-button v-hasPermi="'oa:layout-template:update'" link type="primary" @click="router.push(`/layout-template/${row.id}/edit`)">
            {{ row.sourceType === 'PRESET' ? '查看' : '编辑' }}
          </el-button>
          <el-button
            v-if="row.sourceType === 'PRESET'"
            v-hasPermi="'oa:layout-template:create'"
            link
            type="primary"
            @click="handleCopy(row.id)"
          >
            复制
          </el-button>

          <el-button

            v-if="row.status === 'DRAFT'"

            v-hasPermi="'oa:layout-template:update'"

            link

            type="success"

            @click="handlePublish(row)"

          >

            发布

          </el-button>

          <el-button

            v-if="row.status === 'ENABLED'"

            v-hasPermi="'oa:layout-template:update'"

            link

            type="warning"

            @click="handleDisable(row)"

          >

            停用

          </el-button>

          <el-button

            v-if="row.status === 'DISABLED'"

            v-hasPermi="'oa:layout-template:update'"

            link

            type="success"

            @click="handleEnable(row)"

          >

            重新启用

          </el-button>

          <el-button
            v-if="row.sourceType !== 'PRESET'"
            v-hasPermi="'oa:layout-template:delete'"
            link
            type="danger"
            @click="handleDelete(row.id)"
          >
            删除
          </el-button>

        </template>

      </el-table-column>

    </el-table>



    <el-pagination

      v-model:current-page="searchForm.pageNum"

      v-model:page-size="searchForm.pageSize"

      :total="total"

      layout="total, prev, pager, next"

      @current-change="loadData"

    />

  </div>

</template>



<script setup lang="ts">

import { onMounted, reactive, ref } from 'vue'

import { useRouter } from 'vue-router'

import { ElMessage, ElMessageBox } from 'element-plus'

import { Plus } from '@element-plus/icons-vue'

import TableSearch from '@/components/TableSearch.vue'

import DictSelect from '@/components/DictSelect.vue'

import DictLabel from '@/components/DictLabel.vue'

import {

  copyLayoutTemplate,
  deleteLayoutTemplate,

  disableLayoutTemplate,

  enableLayoutTemplate,

  listLayoutTemplates,

  publishLayoutTemplate

} from '@/api/layoutTemplate'

import type { LayoutTemplateVO } from '@/types/layoutTemplate'



const router = useRouter()



const loading = ref(false)

const list = ref<LayoutTemplateVO[]>([])

const total = ref(0)

const searchForm = reactive({

  templateName: '',

  status: '',

  sourceType: '',

  pageNum: 1,

  pageSize: 20

})



async function loadData() {

  loading.value = true

  try {

    const res = await listLayoutTemplates({ ...searchForm })

    list.value = res.list || []

    total.value = res.total || 0

  } finally {

    loading.value = false

  }

}



function resetSearch() {

  searchForm.templateName = ''

  searchForm.status = ''

  searchForm.sourceType = ''

  searchForm.pageNum = 1

  loadData()

}



async function handlePublish(row: LayoutTemplateVO) {

  await ElMessageBox.confirm(`确定发布模板「${row.templateName}」？发布后可被内容创作选用。`, '发布模板', { type: 'info' })

  await publishLayoutTemplate(row.id)

  ElMessage.success('已发布')

  loadData()

}



async function handleDisable(row: LayoutTemplateVO) {

  await ElMessageBox.confirm(`确定停用模板「${row.templateName}」？停用后不可在内容创作中选择。`, '停用模板', { type: 'warning' })

  await disableLayoutTemplate(row.id)

  ElMessage.success('已停用')

  loadData()

}



async function handleEnable(row: LayoutTemplateVO) {

  await ElMessageBox.confirm(`确定重新启用模板「${row.templateName}」？`, '重新启用', { type: 'info' })

  await enableLayoutTemplate(row.id)

  ElMessage.success('已重新启用')

  loadData()

}



async function handleCopy(id: number) {
  const newId = await copyLayoutTemplate(id)
  ElMessage.success('已复制预置模板')
  router.push(`/layout-template/${newId}/edit`)
}

async function handleDelete(id: number) {

  await ElMessageBox.confirm('确定删除该模板？已应用的内容不受影响。', '提示', { type: 'warning' })

  await deleteLayoutTemplate(id)

  ElMessage.success('已删除')

  loadData()

}



onMounted(loadData)

</script>



<style scoped>

.layout-template-page {

  padding: 16px;

}



.action-bar {

  display: flex;

  gap: 12px;

  margin-bottom: 16px;

}

</style>

