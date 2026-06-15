<template>
  <div class="phone-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="手机号/编号/机型/设备编号" clearable />
      </el-form-item>
      <el-form-item label="手机类型">
        <DictSelect v-model="searchForm.phoneType" dict-type="dict_phone_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_phone_status" placeholder="全部" clearable />
      </el-form-item>
      <template #extra>
        <el-button type="success" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增手机信息
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="phoneList" v-loading="loading" stripe border>
      <el-table-column label="序号" width="70" align="center">
        <template #default="{ $index }">
          {{ rowIndex($index) }}
        </template>
      </el-table-column>
      <el-table-column prop="phoneCode" label="手机编号" width="110" />
      <el-table-column prop="phoneModel" label="型号信息" min-width="120" show-overflow-tooltip />
      <el-table-column prop="deviceNumber" label="设备编号" width="110" />
      <el-table-column prop="phoneType" label="手机类型" width="100" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_phone_type" :value="row.phoneType" />
        </template>
      </el-table-column>
      <el-table-column prop="isAochuang" label="奥创手机" width="90" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_yes_no" :value="row.isAochuang" />
        </template>
      </el-table-column>
      <el-table-column prop="keeperName" label="保管人" width="100" />
      <el-table-column prop="handlerName" label="经手人" width="100" />
      <el-table-column prop="purchaseBatch" label="购买批次" width="100" />
      <el-table-column label="购买日期" width="110">
        <template #default="{ row }">{{ row.purchaseDate || '--' }}</template>
      </el-table-column>
      <el-table-column label="购买时间" width="90">
        <template #default="{ row }">{{ formatTime(row.purchaseTime) }}</template>
      </el-table-column>
      <el-table-column label="设置截图" width="90" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.settingsScreenshotUrl"
            :src="imagePreview(row.settingsScreenshotUrl)"
            :preview-src-list="[imagePreview(row.settingsScreenshotUrl)]"
            fit="cover"
            class="thumb"
          />
          <span v-else class="text-muted">--</span>
        </template>
      </el-table-column>
      <el-table-column label="正面图" width="80" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.frontImageUrl"
            :src="imagePreview(row.frontImageUrl)"
            :preview-src-list="[imagePreview(row.frontImageUrl)]"
            fit="cover"
            class="thumb"
          />
          <span v-else class="text-muted">--</span>
        </template>
      </el-table-column>
      <el-table-column label="反面图" width="80" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.backImageUrl"
            :src="imagePreview(row.backImageUrl)"
            :preview-src-list="[imagePreview(row.backImageUrl)]"
            fit="cover"
            class="thumb"
          />
          <span v-else class="text-muted">--</span>
        </template>
      </el-table-column>
      <el-table-column prop="phoneNumberMasked" label="手机号" width="130">
        <template #default="{ row }">
          <span class="masked-text">{{ row.phoneNumberMasked || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_phone_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="searchForm.pageNo"
      :page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @update:current-page="(val) => searchForm.pageNo = val"
      @update:page-size="(val) => { searchForm.pageSize = val; handleSearch() }"
      @current-change="handleSearch"
      @size-change="handleSearch"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" destroy-on-close>
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item v-if="formData.id" label="手机号">
              <el-input :model-value="editingPhoneMasked" disabled placeholder="已登记号码" />
            </el-form-item>
            <el-form-item v-else label="手机号" prop="phoneNumber">
              <el-input v-model="formData.phoneNumber" placeholder="11位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机编号">
              <el-input v-model="formData.phoneCode" placeholder="内部编号" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号信息">
              <el-input v-model="formData.phoneModel" placeholder="如 iPhone 15 Pro" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备编号">
              <el-input v-model="formData.deviceNumber" placeholder="设备编号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机类型">
              <DictSelect v-model="formData.phoneType" dict-type="dict_phone_type" placeholder="请选择" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="奥创手机">
              <DictSelect v-model="formData.isAochuang" dict-type="dict_yes_no" placeholder="请选择" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买批次">
              <el-input v-model="formData.purchaseBatch" placeholder="购买批次" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经手人">
              <el-input v-model="formData.handlerName" placeholder="经手人姓名" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买日期">
              <el-date-picker
                v-model="formData.purchaseDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买时间">
              <el-time-picker
                v-model="formData.purchaseTime"
                value-format="HH:mm:ss"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保管人" prop="keeperId">
              <UserSelect v-model="formData.keeperId" placeholder="请选择保管人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <DictSelect v-model="formData.status" dict-type="dict_phone_status" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定微信">
              <el-input v-model="formData.wechatBound" placeholder="绑定微信号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">设备图片</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="设置截图">
            <ImageUploadField
              v-model="formData.settingsScreenshotKey"
              v-model:preview-url="formData.settingsScreenshotUrl"
            />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="正面图">
              <ImageUploadField
                v-model="formData.frontImageKey"
                v-model:preview-url="formData.frontImageUrl"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="反面图">
              <ImageUploadField
                v-model="formData.backImageKey"
                v-model:preview-url="formData.backImageUrl"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, defineComponent, h } from 'vue'
import { ElMessage, ElMessageBox, ElButton, ElImage, ElUpload } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import { exportToExcel } from '@/utils'
import { appendFileAuth } from '@/utils/fileUrl'
import { IMAGE_ACCEPT, uploadContentImage, validateImageFile } from '@/api/file'
import {
  createPhone,
  deletePhone,
  getPhonePage,
  updatePhone,
  type PhoneVO,
} from '@/api/phone'

const ImageUploadField = defineComponent({
  name: 'ImageUploadField',
  props: {
    modelValue: { type: String, default: '' },
    previewUrl: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'update:previewUrl'],
  setup(props, { emit }) {
    const uploading = ref(false)
    const localPreview = ref('')

    const displayUrl = () => {
      const url = localPreview.value || props.previewUrl
      return url ? appendFileAuth(url) : ''
    }

    const handleUpload = async (options: { file: File }) => {
      const err = validateImageFile(options.file)
      if (err) {
        ElMessage.error(err)
        return
      }
      uploading.value = true
      try {
        const uploaded = await uploadContentImage(options.file)
        emit('update:modelValue', uploaded.key)
        emit('update:previewUrl', uploaded.url)
        localPreview.value = uploaded.url
        ElMessage.success('上传成功')
      } catch (e: any) {
        ElMessage.error(e?.message || '上传失败')
      } finally {
        uploading.value = false
      }
    }

    const handleClear = () => {
      emit('update:modelValue', '')
      emit('update:previewUrl', '')
      localPreview.value = ''
    }

    return () => h('div', { class: 'image-upload-field' }, [
      displayUrl()
        ? h(ElImage, {
            src: displayUrl(),
            fit: 'cover',
            class: 'upload-preview',
            previewSrcList: [displayUrl()],
          })
        : null,
      h(ElUpload, {
        accept: IMAGE_ACCEPT,
        showFileList: false,
        httpRequest: handleUpload,
      }, {
        default: () => h(ElButton, { loading: uploading.value, size: 'small' }, () => props.modelValue ? '重新上传' : '上传图片'),
      }),
      props.modelValue
        ? h(ElButton, { link: true, type: 'danger', size: 'small', onClick: handleClear }, () => '清除')
        : null,
    ])
  },
})

const loading = ref(false)
const exportLoading = ref(false)
const submitLoading = ref(false)
const phoneList = ref<PhoneVO[]>([])
const total = ref(0)

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  phoneType: undefined as string | undefined,
  status: undefined as string | undefined,
})

const rowIndex = (index: number) => (searchForm.pageNo - 1) * searchForm.pageSize + index + 1
const imagePreview = (url?: string) => (url ? appendFileAuth(url) : '')
const formatTime = (time?: string) => (time ? time.slice(0, 5) : '--')

const loadList = async () => {
  loading.value = true
  try {
    const res = await getPhonePage({
      phoneNumber: searchForm.keyword,
      phoneType: searchForm.phoneType,
      status: searchForm.status,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    phoneList.value = res.list || []
    total.value = res.total ?? 0
  } catch {
    phoneList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadList()

const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.phoneType = undefined
  searchForm.status = undefined
  searchForm.pageNo = 1
  loadList()
}

const buildListParams = (pageNo: number, pageSize: number) => ({
  phoneNumber: searchForm.keyword,
  phoneType: searchForm.phoneType,
  status: searchForm.status,
  pageNo,
  pageSize,
})

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getPhonePage(buildListParams(1, exportPageSize))
  let rows = first.list || []
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getPhonePage(buildListParams(page, exportPageSize))
      rows = rows.concat(res.list || [])
    }
  }
  return rows
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    const exportData = rows.map((row, idx) => ({
      seq: idx + 1,
      phoneCode: row.phoneCode || '',
      phoneModel: row.phoneModel || '',
      deviceNumber: row.deviceNumber || '',
      phoneType: row.phoneType || '',
      isAochuang: row.isAochuang || '',
      keeperName: row.keeperName || '',
      handlerName: row.handlerName || '',
      purchaseBatch: row.purchaseBatch || '',
      purchaseDate: row.purchaseDate || '',
      purchaseTime: formatTime(row.purchaseTime),
      phoneNumberMasked: row.phoneNumberMasked || '',
      status: row.status || '',
    }))
    const columns = [
      { key: 'seq', label: '序号' },
      { key: 'phoneCode', label: '手机编号' },
      { key: 'phoneModel', label: '型号信息' },
      { key: 'deviceNumber', label: '设备编号' },
      { key: 'phoneType', label: '手机类型' },
      { key: 'isAochuang', label: '奥创手机' },
      { key: 'keeperName', label: '保管人' },
      { key: 'handlerName', label: '经手人' },
      { key: 'purchaseBatch', label: '购买批次' },
      { key: 'purchaseDate', label: '购买日期' },
      { key: 'purchaseTime', label: '购买时间' },
      { key: 'phoneNumberMasked', label: '手机号' },
      { key: 'status', label: '状态' },
    ]
    exportToExcel(exportData, columns, '手机管理')
  } catch {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增手机信息')
const formRef = ref()
const editingPhoneMasked = ref('')

const formData = reactive({
  id: undefined as number | undefined,
  phoneNumber: '',
  phoneCode: '',
  phoneModel: '',
  settingsScreenshotKey: '',
  settingsScreenshotUrl: '',
  frontImageKey: '',
  frontImageUrl: '',
  backImageKey: '',
  backImageUrl: '',
  purchaseBatch: '',
  purchaseDate: '',
  purchaseTime: '',
  handlerName: '',
  deviceNumber: '',
  isAochuang: '',
  phoneType: '',
  keeperId: undefined as number | undefined,
  wechatBound: '',
  status: 'ENABLED',
})

const formRules = {
  phoneNumber: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (formData.id) { cb(); return }
      if (!val || !/^1[3-9]\d{9}$/.test(val)) cb(new Error('请输入正确的11位手机号'))
      else cb()
    },
    trigger: 'blur',
  }],
  keeperId: [{ required: true, message: '请选择保管人', trigger: 'change' }],
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    phoneNumber: '',
    phoneCode: '',
    phoneModel: '',
    settingsScreenshotKey: '',
    settingsScreenshotUrl: '',
    frontImageKey: '',
    frontImageUrl: '',
    backImageKey: '',
    backImageUrl: '',
    purchaseBatch: '',
    purchaseDate: '',
    purchaseTime: '',
    handlerName: '',
    deviceNumber: '',
    isAochuang: '',
    phoneType: '',
    keeperId: undefined,
    wechatBound: '',
    status: 'ENABLED',
  })
  editingPhoneMasked.value = ''
}

const handleAdd = () => {
  dialogTitle.value = '新增手机信息'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: PhoneVO) => {
  dialogTitle.value = '编辑手机信息'
  editingPhoneMasked.value = row.phoneNumberMasked || '--'
  Object.assign(formData, {
    id: row.id,
    phoneNumber: '',
    phoneCode: row.phoneCode || '',
    phoneModel: row.phoneModel || '',
    settingsScreenshotKey: row.settingsScreenshotKey || '',
    settingsScreenshotUrl: row.settingsScreenshotUrl || '',
    frontImageKey: row.frontImageKey || '',
    frontImageUrl: row.frontImageUrl || '',
    backImageKey: row.backImageKey || '',
    backImageUrl: row.backImageUrl || '',
    purchaseBatch: row.purchaseBatch || '',
    purchaseDate: row.purchaseDate || '',
    purchaseTime: row.purchaseTime || '',
    handlerName: row.handlerName || '',
    deviceNumber: row.deviceNumber || '',
    isAochuang: row.isAochuang || '',
    phoneType: row.phoneType || '',
    keeperId: row.keeperId,
    wechatBound: row.wechatBound || '',
    status: row.status || 'ENABLED',
  })
  dialogVisible.value = true
}

const buildPayload = () => ({
  phoneCode: formData.phoneCode || undefined,
  phoneModel: formData.phoneModel || undefined,
  settingsScreenshotKey: formData.settingsScreenshotKey || undefined,
  frontImageKey: formData.frontImageKey || undefined,
  backImageKey: formData.backImageKey || undefined,
  purchaseBatch: formData.purchaseBatch || undefined,
  purchaseDate: formData.purchaseDate || undefined,
  purchaseTime: formData.purchaseTime || undefined,
  handlerName: formData.handlerName || undefined,
  deviceNumber: formData.deviceNumber || undefined,
  isAochuang: formData.isAochuang || undefined,
  phoneType: formData.phoneType || undefined,
  keeperId: formData.keeperId!,
  wechatBound: formData.wechatBound || undefined,
  status: formData.status,
})

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = buildPayload()
      if (formData.id) {
        await updatePhone({ id: formData.id, ...payload })
      } else {
        await createPhone({ ...payload, phoneNumber: formData.phoneNumber })
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch {
      /* 错误已由拦截器提示 */
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = async (row: PhoneVO) => {
  try {
    await ElMessageBox.confirm('确认删除该手机记录？', '提示', { type: 'warning' })
    await deletePhone(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.phone-page { padding: 20px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.masked-text { font-family: monospace; }
.text-muted { color: #c0c4cc; }
.thumb { width: 48px; height: 48px; border-radius: 4px; }
:deep(.image-upload-field) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
:deep(.upload-preview) {
  width: 120px;
  height: 120px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
</style>
