<!--
  M5 - 成本录入独立页
  依据: FR-M5-001 成本录入
  路径: /finance/cost/edit
  4 区: 基本信息 / 成本项分摊 / 凭证附件 / 提交审批
-->
<template>
  <div class="cost-entry-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/finance/cost' }">成本管理</el-breadcrumb-item>
      <el-breadcrumb-item>成本录入</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card shadow="never">
      <div class="header">
        <h2 style="margin: 0">
          成本录入单
          <el-tag style="margin-left: 8px">{{ statusLabel }}</el-tag>
        </h2>
        <div>
          <el-button @click="router.back()">返回</el-button>
          <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
          <el-button type="primary" @click="submit" :loading="submitting">提交审批</el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" style="margin-top: 16px">
      <!-- Tab 1: 基本信息 -->
      <el-tab-pane label="基本信息" name="basic" :lazy="false">
        <ContentWrap>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" style="max-width: 900px">
            <el-form-item label="成本单号">
              <el-input :model-value="form.costNo" disabled />
            </el-form-item>
            <el-form-item label="成本类型" prop="costType">
              <DictSelect v-model="form.costType" dict-type="dict_cost_type" />
            </el-form-item>
            <el-form-item label="所属 IP 组" prop="ipGroupId">
              <IpGroupTreeSelect v-model="form.ipGroupId" />
            </el-form-item>
            <el-form-item label="关联账号" prop="accountId">
              <AccountSelect v-model="form.accountId" />
            </el-form-item>
            <el-form-item label="所属期间" prop="period">
              <el-date-picker v-model="form.period" type="month" value-format="YYYY-MM" placeholder="选择月份" />
            </el-form-item>
            <el-form-item label="总金额" prop="totalAmount">
              <el-input-number v-model="form.totalAmount" :min="0" :precision="2" :step="100" style="width: 200px" />
              <span style="margin-left: 8px; color: #909399">元</span>
            </el-form-item>
            <el-form-item label="支付方式" prop="payMethod">
              <el-radio-group v-model="form.payMethod">
                <el-radio-button value="alipay">支付宝</el-radio-button>
                <el-radio-button value="wechat">微信</el-radio-button>
                <el-radio-button value="bank">对公转账</el-radio-button>
                <el-radio-button value="cash">现金</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="供应商">
              <CompanySelect v-model="form.vendorId" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 2: 成本分项 -->
      <el-tab-pane label="成本分项" name="breakdown" :lazy="false">
        <ContentWrap>
          <div class="toolbar">
            <span class="title">共 {{ form.items.length }} 项,合计 ¥ {{ itemsTotal.toLocaleString() }}</span>
            <el-button type="primary" @click="addItem">+ 新增分项</el-button>
          </div>
          <el-table :data="form.items" border>
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="费用名" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="如：投流费" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="140">
              <template #default="{ row }">
                <el-select v-model="row.category">
                  <el-option label="投流费" value="ads" />
                  <el-option label="人力" value="labor" />
                  <el-option label="设备" value="device" />
                  <el-option label="差旅" value="travel" />
                  <el-option label="其他" value="other" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.amount" :min="0" :precision="2" />
              </template>
            </el-table-column>
            <el-table-column label="可分摊" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.sharable" />
              </template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }">
                <el-input v-model="row.remark" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-alert
            v-if="Math.abs(itemsTotal - form.totalAmount) > 0.01 && form.items.length"
            type="warning"
            :closable="false"
            show-icon
            style="margin-top: 12px"
            :title="`分项合计 ¥${itemsTotal.toLocaleString()} ≠ 总金额 ¥${form.totalAmount.toLocaleString()}`"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 3: 凭证附件 -->
      <el-tab-pane label="凭证附件" name="files" :lazy="false">
        <ContentWrap>
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="form.files"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <p style="color: #909399; font-size: 13px; margin-top: 8px">
            支持上传发票、合同、付款凭证等,单个文件不超过 10MB
          </p>
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 4: 审批流 -->
      <el-tab-pane label="审批流" name="approval" :lazy="false">
        <ContentWrap>
          <el-steps :active="approvalStep" finish-status="success">
            <el-step title="提交" description="运营人员" />
            <el-step title="财务初审" description="财务-小李" />
            <el-step title="财务总监" description="财务总监" />
            <el-step title="归档" description="凭证入账" />
          </el-steps>
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import CompanySelect from '@/components/selectors/CompanySelect.vue'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const activeTab = ref('basic')
const formRef = ref<FormInstance>()
const approvalStep = ref(0)

const form = reactive<any>({
  costNo: 'C' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '-' + Math.floor(Math.random() * 1000),
  costType: undefined,
  ipGroupId: undefined,
  accountId: undefined,
  period: new Date().toISOString().slice(0, 7),
  totalAmount: 0,
  payMethod: 'alipay',
  vendorId: undefined,
  remark: '',
  items: [] as any[],
  files: [] as any[],
})

const rules: FormRules = {
  costType: [{ required: true, message: '请选择成本类型', trigger: 'change' }],
  ipGroupId: [{ required: true, message: '请选择 IP 组', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择关联账号', trigger: 'change' }],
  period: [{ required: true, message: '请选择所属期间', trigger: 'change' }],
  totalAmount: [{ required: true, type: 'number', min: 0.01, message: '请输入大于 0 的金额', trigger: 'blur' }],
}

const statusLabel = computed(() => ['草稿', '待初审', '待总监审', '已入账'][approvalStep.value] || '草稿')
const itemsTotal = computed(() => form.items.reduce((s: number, i: any) => s + (i.amount || 0), 0))

const addItem = () => {
  form.items.push({ name: '', category: 'ads', amount: 0, sharable: true, remark: '' })
}
const removeItem = (i: number) => form.items.splice(i, 1)

const handleFileChange = (file: any) => {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  form.files.push(file)
}
const handleFileRemove = (file: any) => {
  form.files = form.files.filter((f: any) => f.uid !== file.uid)
}

const saveDraft = async () => {
  saving.value = true
  await new Promise((r) => setTimeout(r, 300))
  saving.value = false
  ElMessage.success('草稿已保存')
}

const submit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    activeTab.value = 'basic'
    ElMessage.warning('请完整填写基本信息')
    return
  }
  if (form.items.length === 0) {
    activeTab.value = 'breakdown'
    ElMessage.warning('请至少添加一项成本分项')
    return
  }
  if (form.files.length === 0) {
    try {
      await ElMessageBox.confirm('尚未上传凭证附件,确定要提交吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  submitting.value = true
  await new Promise((r) => setTimeout(r, 500))
  submitting.value = false
  approvalStep.value = 1
  ElMessage.success('已提交财务初审')
}

onMounted(() => {
  // 初始化示例数据
  form.items = [
    { name: '抖音投流费', category: 'ads', amount: 5000, sharable: true, remark: '618 期间' },
    { name: '设备采购', category: 'device', amount: 3200, sharable: true, remark: '麦克风+灯光' },
  ]
})
</script>

<style scoped>
.cost-entry-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.toolbar .title { font-weight: 600; }
</style>
