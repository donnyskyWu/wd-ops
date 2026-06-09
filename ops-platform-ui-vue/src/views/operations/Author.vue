<template>
  <div class="author-page">
    <!-- 搜索区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="作者名称">
        <el-input v-model="searchForm.authorName" placeholder="请输入作者名称" clearable />
      </el-form-item>
      <el-form-item label="作者类型">
        <el-select v-model="searchForm.authorType" placeholder="请选择" clearable>
          <el-option label="直播" value="LIVE" />
          <el-option label="短视频" value="SHORT_VIDEO" />
          <el-option label="直播+短视频" value="BOTH" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <!-- 操作按钮 -->
    <ContentWrap title="作者管理">
      <template #extra>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增作者
        </el-button>
      </template>

      <!-- 作者列表表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="authorName" label="作者名称" min-width="120" />
        <el-table-column prop="authorTypeText" label="作者类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getAuthorTypeTag(row?.authorType)">
              {{ row?.authorTypeText || row?.authorType || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipGroupName" label="所属IP组" min-width="120">
          <template #default="{ row }">
            {{ row?.ipGroupName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="primaryAccountName" label="主推号" min-width="150">
          <template #default="{ row }">
            {{ row?.primaryAccountName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="运营人员" min-width="150">
          <template #default="{ row }">
            <template v-if="row?.opsUserNames && row.opsUserNames.length">
              <el-tag v-for="(name, index) in row.opsUserNames" :key="index" size="small" style="margin-right: 4px">
                {{ name }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="followerCount" label="粉丝数" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row && row.followerCount != null">{{ formatNumber(row.followerCount) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="contentCount" label="内容数" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row && row.contentCount != null">{{ row.contentCount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="liveHours" label="直播时长" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row && row.liveHours != null">{{ row.liveHours.toFixed(1) }}h</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row?.status === 1 ? 'success' : 'info'">
              {{ row?.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row?.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleGoDashboard(row)">
              看板
            </el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="primary" size="small" @click="handleViewOps(row)">
              运营
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => pagination.pageNo = val"
        @update:page-size="(val) => { pagination.pageSize = val; handlePageChange() }"
        @change="handlePageChange"
      />
    </ContentWrap>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="作者名称" prop="authorName">
          <el-input v-model="formData.authorName" placeholder="请输入作者名称" />
        </el-form-item>
        <el-form-item label="作者类型" prop="authorType">
          <DictSelect
            v-model="formData.authorType"
            type="dict_author_type"
            placeholder="请选择作者类型"
          />
        </el-form-item>
        <el-form-item label="所属IP组" prop="ipGroupId">
          <el-select
            v-model="formData.ipGroupId"
            placeholder="请选择IP组（小组）"
            filterable
            :loading="ipGroupLoading"
            style="width: 100%"
          >
            <el-option
              v-for="g in smallIpGroupOptions"
              :key="g.id"
              :label="g.label"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主推号" prop="primaryAccountId">
          <el-select
            v-model="formData.primaryAccountId"
            placeholder="请选择主推号（OFFICIAL_ACCOUNT 类型）"
            clearable
            filterable
            :loading="accountLoading"
            style="width: 100%"
          >
            <el-option
              v-for="a in officialAccountOptions"
              :key="a.id"
              :label="a.label"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="组长（系统用户）" prop="userId">
          <el-select
            v-model="formData.userId"
            placeholder="请选择组长/系统用户"
            clearable
            filterable
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="u.label"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 数据看板对话框 -->
    <el-dialog v-model="dashboardDialogVisible" title="作者数据看板" width="900px">
      <el-descriptions v-if="dashboardData" :column="3" border>
        <el-descriptions-item label="作者名称">
          {{ dashboardData.authorName }}
        </el-descriptions-item>
        <el-descriptions-item label="作者类型">
          {{ dashboardData.authorTypeText || dashboardData.authorType }}
        </el-descriptions-item>
        <el-descriptions-item label="所属IP组">
          {{ dashboardData.ipGroupName }}
        </el-descriptions-item>
        <el-descriptions-item label="主推号">
          {{ dashboardData.primaryAccountName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="粉丝数">
          {{ formatNumber(dashboardData.followerCount) }}
        </el-descriptions-item>
        <el-descriptions-item label="内容产出数">
          {{ dashboardData.contentCount }}
        </el-descriptions-item>
        <el-descriptions-item label="直播时长">
          {{ dashboardData.liveHours ? dashboardData.liveHours.toFixed(1) + '小时' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="视频数量">
          {{ dashboardData.videoCount }}
        </el-descriptions-item>
        <el-descriptions-item label="爆款内容数">
          {{ dashboardData.hitContentCount }}
        </el-descriptions-item>
      </el-descriptions>
      
      <el-divider />
      
      <div v-if="dashboardData && dashboardData.followerTrend.length > 0">
        <h4>粉丝趋势（近3日）</h4>
        <el-table :data="dashboardData.followerTrend" border stripe>
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="followerCount" label="粉丝数" width="120" align="right">
            <template #default="{ row }">
              {{ formatNumber(row.followerCount) }}
            </template>
          </el-table-column>
          <el-table-column prop="newFollower" label="新增" width="100" align="right" />
          <el-table-column prop="unfollowCount" label="取关" width="100" align="right" />
        </el-table>
      </div>
    </el-dialog>

    <!-- 运营人员列表对话框 -->
    <el-dialog v-model="opsDialogVisible" title="负责运营人员" width="600px">
      <el-table v-if="opsData" :data="opsData" border stripe>
        <el-table-column prop="userName" label="姓名" width="120" />
        <el-table-column prop="deptName" label="部门" min-width="150" />
        <el-table-column prop="relTime" label="关联时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import {
  getAuthorPage,
  createAuthor,
  updateAuthor,
  deleteAuthor,
  getAuthorDashboard,
  getAuthorOpsList,
} from '@/api/author'
import { getIpGroupTree } from '@/api/ip-group'
import { getAccountList } from '@/api/account'
import { fetchUserList } from '@/api/system-user'
import DictSelect from '@/components/DictSelect.vue'
import type {
  AuthorListVO,
  AuthorSaveReqVO,
  AuthorDashboardVO,
  OpsUserVO,
  AuthorPageReqVO,
} from '@/types/author'

// 搜索表单
const router = useRouter()

const searchForm = reactive({
  authorName: '',
  authorType: undefined as string | undefined,
  status: undefined as number | undefined,
})

// 表格数据 - 初始为空，由 onMounted 拉取真实 API
const loading = ref(false)
const tableData = ref<AuthorListVO[]>([])

// 分页
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

// 表单下拉：IP 组/账号/用户（真实 API）
interface IpGroupOption { id: number; label: string }
interface AccountOption { id: number; label: string }
interface UserOption { id: number; label: string }

const ipGroupLoading = ref(false)
const accountLoading = ref(false)
const userLoading = ref(false)

const smallIpGroupOptions = ref<IpGroupOption[]>([])
const officialAccountOptions = ref<AccountOption[]>([])
const userOptions = ref<UserOption[]>([])

const flattenIpGroups = (
  nodes: Array<{ id: number; groupName?: string; name?: string; groupType?: number; children?: any[] }>,
  parentName = '',
  out: IpGroupOption[] = [],
) => {
  for (const n of nodes) {
    const rawName = n.groupName || n.name || ''
    const label = parentName ? `${parentName} / ${rawName}` : rawName
    // 后端约定：groupType=1 大组，2 小组（AuthorServiceImpl.validateIpGroupSmall 校验 == 2）
    if (Number(n.groupType) === 2) {
      out.push({ id: n.id, label })
    }
    if (n.children && n.children.length) {
      flattenIpGroups(n.children, label, out)
    }
  }
  return out
}

const loadIpGroupOptions = async () => {
  ipGroupLoading.value = true
  try {
    const tree: any = await getIpGroupTree()
    // 1) 兼容后端直接返回 { data: [...] }
    const list: any[] = Array.isArray(tree) ? tree : (tree?.data || [])
    smallIpGroupOptions.value = flattenIpGroups(list)
    if (smallIpGroupOptions.value.length === 0) {
      console.warn('[Author] IP 组树为空，请确认 seed-ops 是否已灌入小组')
    }
  } catch (e: any) {
    console.error('加载 IP 组失败', e?.response?.data || e)
    ElMessage.error(`IP 组加载失败：${e?.response?.data?.msg || e?.message || '未知错误'}`)
  } finally {
    ipGroupLoading.value = false
  }
}

const loadAccountOptions = async () => {
  accountLoading.value = true
  try {
    const res: any = await getAccountList({ pageNo: 1, pageSize: 200 } as any)
    const list = (res as any)?.list || []
    // 主推号限定 accountType=OFFICIAL_ACCOUNT（后端 toResp 不返回 accountType，
    // 但 AuthorServiceImpl.validatePrimaryAccount 仅按 accountType 校验，
    // 因此由后端 create/update 时强校验；前端只展示平台类型供辨认）
    officialAccountOptions.value = list.map((a: any) => ({
      id: a.id,
      label: `${a.accountName}（${a.platformType || '账号'}）`,
    }))
  } catch (e) {
    console.error('加载账号失败', e)
    ElMessage.error('账号列表加载失败')
  } finally {
    accountLoading.value = false
  }
}

const loadUserOptions = async () => {
  userLoading.value = true
  try {
    const res = await fetchUserList({ pageNo: 1, pageSize: 200 })
    const list = (res as any)?.list || []
    userOptions.value = list
      .filter((u: any) => u.status === 'ENABLED')
      .map((u: any) => ({
        id: u.id,
        label: `${u.nickname || u.username}${u.position ? '（' + u.position + '）' : ''}`,
      }))
  } catch (e) {
    console.error('加载用户失败', e)
    ElMessage.error('用户列表加载失败')
  } finally {
    userLoading.value = false
  }
}

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = reactive<AuthorSaveReqVO>({
  authorName: '',
  ipGroupId: 0,
  authorType: '' as any,
  primaryAccountId: null,
  userId: null,
  status: 1,
})

// 表单校验规则
const formRules: FormRules = {
  authorName: [
    { required: true, message: '请输入作者名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在1到50个字符', trigger: 'blur' },
  ],
  ipGroupId: [{ required: true, message: '请选择IP组', trigger: 'change' }],
  authorType: [{ required: true, message: '请选择作者类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

// 数据看板对话框
const dashboardDialogVisible = ref(false)
const dashboardData = ref<AuthorDashboardVO | null>(null)

// 运营人员对话框
const opsDialogVisible = ref(false)
const opsData = ref<OpsUserVO[]>([])

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchForm.authorName || undefined,
      status: searchForm.status,
      page: pagination.pageNo,
      size: pagination.pageSize,
    }
    const result = await getAuthorPage(params as any)
    tableData.value = (result as any)?.list || []
    pagination.total = (result as any)?.total || 0
  } catch (error) {
    console.error('加载作者数据失败:', error)
    ElMessage.error('加载作者列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.authorName = ''
  searchForm.authorType = undefined
  searchForm.status = undefined
  handleSearch()
}

// 分页变化
const handlePageChange = (page: number, size: number) => {
  pagination.pageNo = page
  pagination.pageSize = size
  loadData()
}

// 新增
const handleCreate = () => {
  dialogTitle.value = '新增作者'
  Object.assign(formData, {
    id: undefined,
    authorName: '',
    ipGroupId: 0,
    authorType: '' as any,
    primaryAccountId: null,
    userId: null,
    status: 1,
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: AuthorListVO) => {
  dialogTitle.value = '编辑作者'
  Object.assign(formData, {
    id: row.id,
    authorName: row.authorName,
    ipGroupId: row.ipGroupId,
    authorType: row.authorType,
    primaryAccountId: row.primaryAccountId,
    userId: null, // 需要从详情获取
    status: row.status,
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (formData.id) {
        await updateAuthor(formData)
        ElMessage.success('修改成功')
      } else {
        await createAuthor(formData)
        ElMessage.success('新增成功')
      }

      dialogVisible.value = false
      await loadData()
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 删除
const handleDelete = async (row: AuthorListVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除作者"${row.authorName}"吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await deleteAuthor(row.id)
    ElMessage.success('删除成功')

    await loadData()
  } catch (error) {
    // 用户取消或接口失败
  }
}

// 跳转到作者看板独立页
const handleGoDashboard = (row: AuthorListVO) => {
  router.push(`/author/${row.id}/dashboard`)
}

// 查看数据看板(旧版弹窗,已用独立页替代)
const handleViewDashboard = (row: AuthorListVO) => {
  router.push(`/author/${row.id}/dashboard`)
}

// 查看运营人员
const handleViewOps = async (row: AuthorListVO) => {
  try {
    const data = await getAuthorOpsList(row.id)
    opsData.value = data
    opsDialogVisible.value = true
  } catch (error) {
    console.error('获取运营人员失败:', error)
    ElMessage.error('获取运营人员失败')
  }
}

// 获取作者类型标签颜色
const getAuthorTypeTag = (type: string): string => {
  const colors: Record<string, string> = {
    LIVE: 'danger',
    SHORT_VIDEO: 'warning',
    BOTH: 'success',
  }
  return colors[type] || 'info'
}

// 格式化数字
const formatNumber = (num: any): string => {
  if (num == null || isNaN(Number(num))) return '-'
  const n = Number(num)
  if (n >= 10000) {
    return (n / 10000).toFixed(1) + '万'
  }
  return n.toString()
}

// 初始化
onMounted(() => {
  loadData()
  loadIpGroupOptions()
  loadAccountOptions()
  loadUserOptions()
})
</script>

<style scoped lang="scss">
.author-page {
  padding: 20px;
}
</style>
