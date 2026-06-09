<!--
  M4 - 实名人详情
  依据: UX-M4 § 4.1 / S-03 中介人
-->
<template>
  <div class="realname-detail-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/realname' }">实名人管理</el-breadcrumb-item>
      <el-breadcrumb-item>详情</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="detail">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              {{ detail.realName }}
              <el-tag :type="detail.status === 'ENABLED' ? 'success' : 'info'" style="margin-left: 8px">
                {{ detail.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </h2>
            <p class="meta">
              <span>证件：{{ detail.idType }} {{ detail.idCardMasked }}</span>
              <el-divider direction="vertical" />
              <span>手机：{{ detail.phoneMasked }}</span>
              <el-divider direction="vertical" />
              <span>微信：{{ detail.wechat || '-' }}</span>
            </p>
          </div>
          <div>
            <el-button @click="router.back()">返回</el-button>
          </div>
        </div>
      </el-card>

      <el-tabs v-model="activeTab" style="margin-top: 16px">
        <el-tab-pane label="基本信息" name="basic">
          <ContentWrap>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">{{ detail.realName }}</el-descriptions-item>
              <el-descriptions-item label="所属公司">{{ detail.companyName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="证件号">{{ detail.idCardMasked }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ detail.phoneMasked }}</el-descriptions-item>
              <el-descriptions-item label="微信号">{{ detail.wechat || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="detail.status === 'ENABLED' ? 'success' : 'info'" size="small">
                  {{ detail.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </ContentWrap>
        </el-tab-pane>

        <el-tab-pane :label="`中介人（${intermediaries.length}）`" name="intermediaries">
          <ContentWrap>
            <div class="toolbar">
              <span class="title">该实名人的中介人联系人</span>
              <el-button type="primary" @click="openIntermediaryDialog()">+ 新增中介人</el-button>
            </div>
            <el-table v-if="intermediaries.length" :data="intermediaries" border stripe>
              <el-table-column prop="intermediaryName" label="姓名" width="120" />
              <el-table-column prop="intermediaryPhoneMasked" label="手机" width="140" />
              <el-table-column prop="intermediaryWechat" label="微信" width="140" />
              <el-table-column prop="relationType" label="关系" width="120">
                <template #default="{ row }">
                  <el-tag size="small">{{ relationLabel(row.relationType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="commissionRateDisplay" label="佣金比例" width="120" align="center" />
              <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" link type="primary" @click="openIntermediaryDialog(row)">编辑</el-button>
                  <el-button size="small" link type="danger" @click="handleDeleteIntermediary(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无中介人，点击上方按钮添加" />
          </ContentWrap>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="intermediaryDialogVisible" :title="intermediaryDialogTitle" width="520px">
      <el-form :model="intermediaryForm" :rules="intermediaryRules" ref="intermediaryFormRef" label-width="100px">
        <el-form-item label="中介人姓名" prop="intermediaryName">
          <el-input v-model="intermediaryForm.intermediaryName" placeholder="请输入姓名" maxlength="64" />
        </el-form-item>
        <el-form-item label="手机号" prop="intermediaryPhone">
          <el-input v-model="intermediaryForm.intermediaryPhone" placeholder="11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="微信号">
          <el-input v-model="intermediaryForm.intermediaryWechat" placeholder="可选" />
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <DictSelect v-model="intermediaryForm.relationType" dict-type="dict_intermediary_relation" />
        </el-form-item>
        <el-form-item label="佣金比例" prop="commissionRate">
          <el-input-number v-model="intermediaryForm.commissionRate" :min="0" :max="100" :precision="2" />
          <span class="form-tip">%</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="intermediaryForm.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="intermediaryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIntermediary">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import { getRealname, type RealnameVO } from '@/api/realname'
import {
  createIntermediary,
  deleteIntermediary,
  listIntermediaries,
  updateIntermediary,
  type IntermediaryVO,
} from '@/api/intermediary'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('basic')
const detail = ref<RealnameVO | null>(null)
const intermediaries = ref<IntermediaryVO[]>([])

const relationLabelMap: Record<string, string> = {
  DIRECT: '直签',
  INTERMEDIARY: '中介代理',
  AGENCY: '机构合作',
}
const relationLabel = (v: string) => relationLabelMap[v] || v

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    detail.value = await getRealname(id)
    intermediaries.value = await listIntermediaries(id)
  } catch {
    detail.value = null
    intermediaries.value = []
  } finally {
    loading.value = false
  }
}

const intermediaryDialogVisible = ref(false)
const intermediaryDialogTitle = ref('新增中介人')
const intermediaryFormRef = ref()
const editingIntermediaryId = ref<number | undefined>()

const intermediaryForm = reactive({
  intermediaryName: '',
  intermediaryPhone: '',
  intermediaryWechat: '',
  relationType: 'DIRECT',
  commissionRate: 10 as number | undefined,
  remark: '',
})

const intermediaryRules = {
  intermediaryName: [{ required: true, message: '请输入中介人姓名', trigger: 'blur' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
  commissionRate: [{ required: true, message: '请填写佣金比例', trigger: 'blur' }],
}

const resetIntermediaryForm = () => {
  Object.assign(intermediaryForm, {
    intermediaryName: '',
    intermediaryPhone: '',
    intermediaryWechat: '',
    relationType: 'DIRECT',
    commissionRate: 10,
    remark: '',
  })
  editingIntermediaryId.value = undefined
}

const openIntermediaryDialog = (row?: IntermediaryVO) => {
  resetIntermediaryForm()
  if (row) {
    intermediaryDialogTitle.value = '编辑中介人'
    editingIntermediaryId.value = row.id
    Object.assign(intermediaryForm, {
      intermediaryName: row.intermediaryName,
      intermediaryPhone: '',
      intermediaryWechat: row.intermediaryWechat || '',
      relationType: row.relationType,
      commissionRate: row.commissionRate ?? undefined,
      remark: row.remark || '',
    })
  } else {
    intermediaryDialogTitle.value = '新增中介人'
  }
  intermediaryDialogVisible.value = true
}

const submitIntermediary = async () => {
  if (!intermediaryFormRef.value || !detail.value) return
  await intermediaryFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const payload = {
        intermediaryName: intermediaryForm.intermediaryName,
        intermediaryPhone: intermediaryForm.intermediaryPhone || undefined,
        intermediaryWechat: intermediaryForm.intermediaryWechat || undefined,
        relationType: intermediaryForm.relationType,
        commissionRate: intermediaryForm.commissionRate!,
        remark: intermediaryForm.remark || undefined,
      }
      if (editingIntermediaryId.value) {
        await updateIntermediary({ id: editingIntermediaryId.value, ...payload })
      } else {
        await createIntermediary(detail.value!.id, payload)
      }
      ElMessage.success('保存成功')
      intermediaryDialogVisible.value = false
      intermediaries.value = await listIntermediaries(detail.value!.id)
    } catch {
      /* 错误已由拦截器提示 */
    }
  })
}

const handleDeleteIntermediary = async (row: IntermediaryVO) => {
  try {
    await ElMessageBox.confirm(`确认删除中介人「${row.intermediaryName}」？`, '提示', { type: 'warning' })
    await deleteIntermediary(row.id)
    ElMessage.success('删除成功')
    if (detail.value) {
      intermediaries.value = await listIntermediaries(detail.value.id)
    }
  } catch {
    /* 取消或错误 */
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.realname-detail-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.toolbar .title { font-weight: 600; }
.form-tip { margin-left: 8px; color: #909399; }
</style>
