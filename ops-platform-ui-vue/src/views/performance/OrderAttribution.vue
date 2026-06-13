<template>
  <div class="order-attribution-page">

    <!-- ROI汇总卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">总成交金额</div>
            <div class="stat-value primary">¥ {{ formatMoney(stats.totalAmount) }}</div>
            <div class="stat-tip">当前筛选条件下</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">总投入成本</div>
            <div class="stat-value">¥ {{ formatMoney(stats.totalCost) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">ROI</div>
            <div class="stat-value success">{{ stats.roi.toFixed(2) }}</div>
            <div class="stat-tip">= 成交 / 成本</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">订单数量</div>
            <div class="stat-value">{{ stats.orderCount }}条</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="IP组">
        <el-select v-model="searchForm.ipGroupId" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="A组" value="1" />
          <el-option label="B组" value="2" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <el-tabs v-model="activeTab" class="analysis-tabs">
      <el-tab-pane label="订单列表" name="list">
        <div class="action-bar">
          <el-button type="primary" @click="handleRoi">
            <el-icon><DataAnalysis /></el-icon>
            ROI 分析
          </el-button>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <span class="total-info">共 {{ total }} 条</span>
        </div>

        <el-table :data="orderList" v-loading="loading" stripe>
          <el-table-column prop="orderNo" label="订单编号" width="180" />
          <el-table-column prop="ipGroupName" label="IP组" width="100" />
          <el-table-column prop="accountName" label="账号名称" width="140" />
          <el-table-column prop="operatorName" label="归因运营" width="100" />
          <el-table-column prop="amount" label="成交金额" width="120" align="right">
            <template #default="{ row }">
              <strong>¥ {{ formatMoney(row.amount) }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="attributionTime" label="归因时间" width="140" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
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
      </el-tab-pane>

      <el-tab-pane label="ROI分析" name="roi">
        <div class="roi-analysis">
          <el-radio-group v-model="roiDimension" @change="loadRoiData" style="margin-bottom: 16px">
            <el-radio-button value="ipGroup">按IP组</el-radio-button>
            <el-radio-button value="account">按账号</el-radio-button>
            <el-radio-button value="operator">按运营人员</el-radio-button>
          </el-radio-group>

          <el-table :data="roiList" stripe>
            <el-table-column :prop="roiDimension === 'ipGroup' ? 'ipGroupName' : roiDimension === 'account' ? 'accountName' : 'operatorName'" :label="roiDimension === 'ipGroup' ? 'IP组' : roiDimension === 'account' ? '账号' : '运营人员'" width="150" />
            <el-table-column prop="amount" label="成交金额" width="140" align="right">
              <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="cost" label="投入成本" width="140" align="right">
              <template #default="{ row }">¥ {{ formatMoney(row.cost) }}</template>
            </el-table-column>
            <el-table-column prop="roi" label="ROI" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.roi >= 3 ? 'success' : row.roi >= 2 ? '' : 'warning'">
                  {{ row.roi.toFixed(2) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="trend" label="趋势" width="120" align="center">
              <template #default="{ row }">
                <span :class="row.trend >= 0 ? 'trend-up' : 'trend-down'">
                  {{ row.trend >= 0 ? '📈 ↑' : '📉 ↓' }}{{ Math.abs(row.trend) }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, DataAnalysis } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import { exportToExcel } from '@/utils'
import {
  getOrderAttributionList,
  getOrderAttributionRoi,
  exportOrderAttribution,
} from '@/api/orderAttribution'

const loading = ref(false)
const activeTab = ref('list')
const router = useRouter()

const stats = reactive({
  totalAmount: 0,
  totalCost: 0,
  roi: 0,
  orderCount: 0,
})

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  dateRange: undefined as string[] | undefined,
  ipGroupId: undefined as string | undefined,
})

const orderList = ref<any[]>([])

const total = ref(0)

const loadList = async () => {
  if (!searchForm.dateRange || searchForm.dateRange.length < 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loading.value = true
  try {
    const res: any = await getOrderAttributionList({
      ipGroupId: searchForm.ipGroupId ? Number(searchForm.ipGroupId) : undefined,
      startDate: searchForm.dateRange[0],
      endDate: searchForm.dateRange[1],
      pageNum: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    orderList.value = (res.list || []).map((row: any) => ({
      id: row.id,
      orderNo: row.orderNo,
      ipGroupName: row.ipGroupName,
      accountName: row.accountName,
      operatorName: row.operatorName,
      amount: row.amount,
      attributionTime: row.attributionTime || row.createTime,
    }))
    total.value = res.total ?? 0
    // 统计
    stats.totalAmount = orderList.value.reduce((s, r) => s + (r.amount || 0), 0)
    stats.orderCount = total.value
  } catch {
    orderList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNo = 1
  loadList()
}

const handleReset = () => {
  searchForm.dateRange = undefined
  searchForm.ipGroupId = undefined
  handleSearch()
}

const formatMoney = (value: number) => {
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}

const handleRoi = () => {
  router.push('/perf/order-attribution/roi')
}

const handleExport = async () => {
  if (!searchForm.dateRange || searchForm.dateRange.length < 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  try {
    const res: any = await exportOrderAttribution(searchForm.dateRange[0], searchForm.dateRange[1])
    ElMessage.success(`导出任务已创建：${res?.jobId || res?.id || '已提交'}`)
  } catch {}
}

const handleDetail = (row: any) => {
  ElMessage.info('订单详情功能开发中')
}

// ROI分析
const roiDimension = ref('ipGroup')
const roiList = ref<any[]>([])

const loadRoiData = async () => {
  if (!searchForm.dateRange || searchForm.dateRange.length < 2) {
    ElMessage.warning('请先选择日期范围')
    return
  }
  try {
    const res: any = await getOrderAttributionRoi({
      ipGroupId: searchForm.ipGroupId ? Number(searchForm.ipGroupId) : undefined,
      startDate: searchForm.dateRange[0],
      endDate: searchForm.dateRange[1],
    })
    // 后端目前返回单一 roi VO；按维度展示同一份数据
    const roiVal = res.roi ?? 0
    const amount = res.totalAmount ?? 0
    const cost = res.totalCost ?? 0
    roiList.value = [
      {
        ipGroupName: roiDimension.value === 'ipGroup' ? '当前IP组' : '-',
        accountName: roiDimension.value === 'account' ? '当前账号' : '-',
        operatorName: roiDimension.value === 'operator' ? '当前运营' : '-',
        amount,
        cost,
        roi: roiVal,
        trend: res.trend ?? 0,
      },
    ]
    stats.totalAmount = amount
    stats.totalCost = cost
    stats.roi = roiVal
  } catch {
    roiList.value = []
  }
}

onMounted(() => {
  // 默认给一个最近30天日期范围
  const today = new Date()
  const start = new Date()
  start.setDate(today.getDate() - 30)
  searchForm.dateRange = [start.toISOString().slice(0, 10), today.toISOString().slice(0, 10)]
  loadList()
})
</script>

<style scoped>
.order-attribution-page {
  padding: 20px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.primary {
  color: #409eff;
}

.stat-value.success {
  color: #67c23a;
}

.stat-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.total-info {
  color: #909399;
  font-size: 14px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.analysis-tabs {
  margin-top: 20px;
}

.trend-up {
  color: #67c23a;
  font-weight: 600;
}

.trend-down {
  color: #f56c6c;
  font-weight: 600;
}

.roi-analysis {
  padding: 16px 0;
}
</style>
