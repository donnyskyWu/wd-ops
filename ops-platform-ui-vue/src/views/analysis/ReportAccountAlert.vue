<!--
  M6-8 异常预警
  依据: FR-M6-008
  路径: /analysis/report/account-alert
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>异常预警</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="预警级别">
          <el-select v-model="filter.level" multiple clearable style="width: 220px">
            <el-option label="紧急" value="urgent" />
            <el-option label="重要" value="important" />
            <el-option label="提示" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card><div class="kpi-label">紧急</div><div class="kpi-value danger">{{ kpi.urgent }}</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="kpi-label">重要</div><div class="kpi-value warning">{{ kpi.important }}</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="kpi-label">提示</div><div class="kpi-value primary">{{ kpi.info }}</div></el-card>
      </el-col>
    </el-row>

    <ContentWrap title="预警列表" style="margin-top: 16px">
      <el-table :data="list" border stripe>
        <el-table-column prop="level" label="级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === '紧急' ? 'danger' : row.level === '重要' ? 'warning' : 'info'" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="account" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="metric" label="指标" width="120" />
        <el-table-column prop="actual" label="实际" width="100" align="right" />
        <el-table-column prop="threshold" label="阈值" width="100" align="right" />
        <el-table-column prop="desc" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="time" label="触发时间" width="160" align="center" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default>
            <el-button link type="primary" size="small">查看</el-button>
            <el-button link type="primary" size="small">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import ContentWrap from '@/components/ContentWrap.vue'

const loading = ref(false)
const filter = reactive({ level: [] as string[] })
const kpi = reactive({ urgent: 0, important: 0, info: 0 })
const list = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 300))
  kpi.urgent = 2
  kpi.important = 5
  kpi.info = 12
  list.value = [
    { level: '紧急', type: '封禁风险', account: '抖音-知识变现研究院', platform: '抖音', metric: '违规分', actual: '85/100', threshold: '80', desc: '已接近封禁阈值', time: '2026-06-07 14:30' },
    { level: '紧急', type: '掉粉异常', account: '公众号-种草官', platform: '公众号', metric: '7 日掉粉', actual: '-5200', threshold: '-3000', desc: '单日掉粉超阈值', time: '2026-06-07 11:00' },
    { level: '重要', type: '互动下滑', account: '小红书-AI 技术前沿', platform: '小红书', metric: '互动率', actual: '2.1%', threshold: '3%', desc: '互动率连续 3 天低于阈值', time: '2026-06-06 18:00' },
    { level: '重要', type: '成本异常', account: '抖音-健身达人', platform: '抖音', metric: '单粉成本', actual: '¥3.2', threshold: '¥2.0', desc: '单粉成本超阈值 60%', time: '2026-06-06 15:20' },
    { level: '重要', type: '产出下降', account: '快手-美食家', platform: '快手', metric: '日均产出', actual: '0.5', threshold: '1', desc: '日均产出连续 5 天低于阈值', time: '2026-06-05 09:00' },
    { level: '提示', type: '素材过期', account: '公众号-财经早班车', platform: '公众号', metric: '最新素材', actual: '12 天前', threshold: '7 天', desc: '素材已 12 天未更新', time: '2026-06-05 14:00' },
    { level: '提示', type: '违规词', account: '小红书-种草官 02', platform: '小红书', metric: '最近笔记', actual: '1 条', threshold: '0', desc: '出现 1 次违规词命中', time: '2026-06-04 16:30' },
  ]
  loading.value = false
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; text-align: center; padding: 8px 0; }
.kpi-value.danger { color: #f56c6c; }
.kpi-value.warning { color: #e6a23c; }
.kpi-value.primary { color: #409eff; }
</style>
