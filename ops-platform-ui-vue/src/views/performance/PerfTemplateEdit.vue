<!--
  M3 - 绩效模板编辑
  依据: FR-M3-001 模板维护
  路径: /perf/template/:id (新增/编辑/查看)
  4 区: 基本信息 / 指标配置(可动态行) / 计算规则预览 / 操作历史
-->
<template>
  <div class="perf-template-edit-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/perf/template' }">绩效模板</el-breadcrumb-item>
      <el-breadcrumb-item>{{ isNew ? '新增模板' : form.name || '编辑' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card shadow="never">
      <div class="header">
        <h2 style="margin: 0">
          {{ isNew ? '新增模板' : form.name }}
          <el-tag v-if="!isNew" :type="form.status === 1 ? 'success' : 'info'" style="margin-left: 8px">
            {{ form.status === 1 ? '启用中' : '已停用' }}
          </el-tag>
        </h2>
        <div>
          <el-button @click="router.back()">返回</el-button>
          <el-button type="primary" @click="save" :loading="saving">保存</el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" style="margin-top: 16px">
      <!-- Tab 1: 基本信息 -->
      <el-tab-pane label="基本信息" name="basic" :lazy="false">
        <ContentWrap>
          <el-form :model="form" label-width="120px" style="max-width: 800px">
            <el-form-item label="模板名称" required>
              <el-input v-model="form.name" placeholder="如：抖音主账号运营模板" />
            </el-form-item>
            <el-form-item label="岗位">
              <DictSelect v-model="form.position" dict-type="dict_position" />
            </el-form-item>
            <el-form-item label="适用平台">
              <el-select v-model="form.platform" multiple style="width: 100%">
                <el-option label="公众号" value="wechat" />
                <el-option label="抖音" value="douyin" />
                <el-option label="小红书" value="xhs" />
                <el-option label="快手" value="kuaishou" />
                <el-option label="B站" value="bilibili" />
              </el-select>
            </el-form-item>
            <el-form-item label="核算周期">
              <el-radio-group v-model="form.cycle">
                <el-radio-button value="monthly">月度</el-radio-button>
                <el-radio-button value="quarterly">季度</el-radio-button>
                <el-radio-button value="yearly">年度</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="基础分">
              <el-input-number v-model="form.baseScore" :min="0" :max="1000" />
            </el-form-item>
            <el-form-item label="满分上限">
              <el-input-number v-model="form.maxScore" :min="0" :max="2000" />
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 2: 指标配置 -->
      <el-tab-pane label="指标配置" name="metrics" :lazy="false">
        <ContentWrap>
          <div class="toolbar">
            <span class="title">共 {{ form.metrics.length }} 项指标,权重合计 {{ totalWeight }}%</span>
            <el-button type="primary" @click="addMetric">+ 新增指标</el-button>
          </div>
          <el-alert
            v-if="totalWeight !== 100"
            type="warning"
            :closable="false"
            show-icon
            :title="`权重合计 ${totalWeight}% ，需等于 100%`"
          />
          <el-table :data="form.metrics" border style="margin-top: 12px">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="指标名" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="如：粉丝净增" />
              </template>
            </el-table-column>
            <el-table-column label="数据源" width="140">
              <template #default="{ row }">
                <el-select v-model="row.source">
                  <el-option label="粉丝净增数" value="follower_inc" />
                  <el-option label="作品发布数" value="work_count" />
                  <el-option label="订单 GMV" value="order_gmv" />
                  <el-option label="互动数" value="engagement" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="权重(%)" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.weight" :min="0" :max="100" />
              </template>
            </el-table-column>
            <el-table-column label="达标值" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.target" :min="0" />
              </template>
            </el-table-column>
            <el-table-column label="计算方式" width="140">
              <template #default="{ row }">
                <el-select v-model="row.calcType">
                  <el-option label="实际/达标" value="ratio" />
                  <el-option label="阶梯计分" value="ladder" />
                  <el-option label="封顶式" value="cap" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeMetric($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 3: 计算预览 -->
      <el-tab-pane label="计算预览" name="preview" :lazy="false">
        <ContentWrap>
          <p style="color: #909399; font-size: 13px">假设某员工某月实际数据如下,自动计算得分：</p>
          <el-form :model="preview" label-width="120px" style="max-width: 800px">
            <el-row :gutter="16" v-for="m in form.metrics" :key="m.name + '_input'">
              <el-col :span="12">
                <el-form-item :label="`${m.name} 实际`">
                  <el-input-number v-model="previewData[m.source!]" :min="0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-divider />
            <el-form-item label="计算明细">
              <pre class="calc-result">{{ calcPreview }}</pre>
            </el-form-item>
            <el-form-item label="最终得分">
              <el-statistic :value="finalScore" :precision="2" style="display: inline-block" />
              <span style="margin-left: 8px; color: #909399">/ {{ form.maxScore }}</span>
            </el-form-item>
          </el-form>
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 4: 操作历史 -->
      <el-tab-pane label="操作历史" name="history" :lazy="false">
        <ContentWrap>
          <el-timeline v-if="!isNew">
            <el-timeline-item
              v-for="(r, i) in history"
              :key="i"
              :timestamp="r.time"
              :type="r.type"
              :hollow="i !== 0"
              placement="top"
            >
              <el-card shadow="never">
                <h4 style="margin: 0 0 4px 0">{{ r.action }}</h4>
                <p style="margin: 0; color: #909399; font-size: 13px">操作人：{{ r.operator }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="新模板尚无历史" />
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  getTemplateDetail,
  createTemplate,
  updateTemplate,
} from '@/api/perfTemplate'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')

const form = reactive<any>({
  id: undefined as number | undefined,
  name: '',
  position: undefined,
  platform: [] as string[],
  cycle: 'monthly',
  baseScore: 0,
  maxScore: 100,
  status: 1,
  remark: '',
  metrics: [] as any[],
})

const preview = reactive<any>({})
const previewData = reactive<Record<string, number>>({})
const history = ref<any[]>([])

const isNew = computed(() => !route.params.id || route.params.id === 'new')
const totalWeight = computed(() => form.metrics.reduce((s: number, m: any) => s + (m.weight || 0), 0))

const calcPreview = computed(() => {
  if (!form.metrics.length) return '请先在 "指标配置" 中添加指标'
  const lines: string[] = []
  let score = form.baseScore
  form.metrics.forEach((m: any) => {
    const actual = previewData[m.source!] || 0
    let part = 0
    if (m.calcType === 'ratio') {
      part = m.target > 0 ? Math.min(1, actual / m.target) * m.weight : 0
    } else if (m.calcType === 'cap') {
      part = m.target > 0 ? Math.min(1, actual / m.target) * m.weight : 0
    } else {
      // ladder
      part = m.target > 0 ? Math.min(1.2, actual / m.target) * m.weight : 0
    }
    lines.push(`  ${m.name} : ${actual} / ${m.target} × ${m.weight}% = ${part.toFixed(2)}`)
    score += part
  })
  lines.unshift(`基础分: ${form.baseScore}`)
  return lines.join('\n') + `\n──────────────────\n  合计: ${score.toFixed(2)}`
})

const finalScore = computed(() => {
  if (!form.metrics.length) return form.baseScore
  let s = form.baseScore
  form.metrics.forEach((m: any) => {
    const actual = previewData[m.source!] || 0
    if (m.target > 0) {
      s += Math.min(1, actual / m.target) * (m.weight || 0)
    }
  })
  return Math.min(s, form.maxScore)
})

const addMetric = () => {
  form.metrics.push({ name: '', source: 'follower_inc', weight: 10, target: 100, calcType: 'ratio' })
}
const removeMetric = (i: number) => form.metrics.splice(i, 1)

const loadDetail = async () => {
  loading.value = true
  try {
    if (!isNew.value) {
      const id = Number(route.params.id)
      const data: any = await getTemplateDetail(id)
      Object.assign(form, {
        id: data.id,
        name: data.templateName || '',
        position: data.position || '',
        platform: data.platform || [],
        cycle: data.cycle || 'monthly',
        baseScore: data.baseScore ?? 0,
        maxScore: data.maxScore ?? 100,
        status: data.isActive ? 1 : 0,
        remark: data.remark || '',
        metrics: (data.items || []).map((it: any) => ({
          name: it.metricName || '',
          source: it.metricCode || '',
          weight: it.weight || 0,
          target: it.target || 0,
          calcType: it.calcRule === 'manual' ? 'ladder' : 'ratio',
        })),
      })
      // 操作历史后端暂未提供，先用静态占位
      history.value = []
    }
  } catch {
    ElMessage.error('加载模板详情失败')
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!form.name) {
    ElMessage.warning('请填写模板名称')
    activeTab.value = 'basic'
    return
  }
  if (totalWeight.value !== 100 && form.metrics.length) {
    ElMessage.warning(`权重合计 ${totalWeight.value}%,需为 100%`)
    activeTab.value = 'metrics'
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      templateName: form.name,
      position: form.position,
      isActive: form.status === 1 ? 1 : 0,
      items: form.metrics.map((m: any, idx: number) => ({
        metricId: m.metricId || idx + 1,
        metricName: m.name,
        metricCode: m.source,
        weight: m.weight,
        calcRule: m.calcType === 'ladder' ? 'manual' : 'auto',
        scoreStandard: { ranges: [] },
      })),
    }
    if (form.id) {
      await updateTemplate(payload)
      ElMessage.success('更新成功')
    } else {
      const newId = await createTemplate(payload)
      form.id = newId
      ElMessage.success('保存成功')
    }
    history.value.unshift({
      time: new Date().toLocaleString('zh-CN'),
      action: form.id ? '更新模板' : '创建模板',
      type: 'info',
      operator: '当前用户',
    })
  } catch {
    // 错误已由拦截器处理
  } finally {
    saving.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.perf-template-edit-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.toolbar .title { font-weight: 600; }
.calc-result {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 6px;
  font-family: 'Cascadia Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  color: #303133;
  margin: 0;
}
</style>
