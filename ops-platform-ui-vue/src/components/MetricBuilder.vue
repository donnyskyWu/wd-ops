<template>
  <div class="metric-builder">
    <div class="builder-layout">
      <div class="builder-main">
        <el-form label-width="96px" size="default">
          <el-form-item label="数据源">
            <el-select
              :model-value="config.dataSource"
              placeholder="选择预定义数据表"
              filterable
              style="width: 100%"
              @update:model-value="onDataSourceChange"
            >
              <el-option
                v-for="item in dataSources"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="计算方式">
            <el-select v-model="config.calcMethod" style="width: 100%" @change="emitFormula">
              <el-option
                v-for="m in METRIC_CALC_METHODS"
                :key="m.value"
                :label="m.label"
                :value="m.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="calcNeedsField" label="计算字段">
            <el-select
              v-model="config.calcField"
              placeholder="选择要聚合的字段"
              filterable
              clearable
              style="width: 100%"
              @change="emitFormula"
            >
              <el-option
                v-for="f in numericFields"
                :key="f.name"
                :label="`${f.label} (${f.name})`"
                :value="f.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="汇总字段">
            <el-select
              v-model="config.groupByFields"
              placeholder="可选，按字段分组"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              style="width: 100%"
              @change="emitFormula"
            >
              <el-option
                v-for="f in schemaFields"
                :key="f.name"
                :label="`${f.label} (${f.name})`"
                :value="f.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="关联表">
            <el-select
              v-model="config.joinTables"
              placeholder="可选，选择关联表"
              multiple
              filterable
              collapse-tags
              style="width: 100%"
              @change="emitFormula"
            >
              <el-option
                v-for="j in availableJoins"
                :key="j.table"
                :label="`${j.label} (${j.table})`"
                :value="j.table"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="查询条件">
            <div class="conditions-wrap">
              <div v-for="(cond, idx) in config.conditions" :key="idx" class="condition-row">
                <el-select v-model="cond.field" placeholder="字段" filterable style="width: 140px" @change="emitFormula">
                  <el-option
                    v-for="f in schemaFields"
                    :key="f.name"
                    :label="f.label"
                    :value="f.name"
                  />
                </el-select>
                <el-select v-model="cond.operator" placeholder="运算符" style="width: 110px" @change="emitFormula">
                  <el-option v-for="op in METRIC_FILTER_OPERATORS" :key="op.value" :label="op.label" :value="op.value" />
                </el-select>
                <el-input
                  v-if="cond.operator !== 'IS NULL' && cond.operator !== 'IS NOT NULL'"
                  v-model="cond.value"
                  placeholder="值"
                  style="flex: 1"
                  @input="emitFormula"
                />
                <el-button link type="danger" @click="removeCondition(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button size="small" @click="addCondition">+ 添加条件</el-button>
            </div>
          </el-form-item>
        </el-form>

        <div class="builder-actions">
          <el-button type="primary" @click="generateFormula">生成公式</el-button>
          <el-button :loading="previewing" @click="handlePreview">预览数据</el-button>
        </div>

        <el-form-item label="计算公式" label-width="96px" class="formula-block">
          <el-input
            :model-value="formula"
            type="textarea"
            :rows="4"
            spellcheck="false"
            placeholder="点击「生成公式」或手动编辑 SQL"
            @update:model-value="onFormulaInput"
          />
          <div class="form-tip">自动生成 SELECT 语句，含 tenant_id 占位符 :tenantId</div>
        </el-form-item>
      </div>

      <div class="builder-side">
        <div class="side-title">字段列表</div>
        <div v-if="!config.dataSource" class="side-empty">请先选择数据源</div>
        <ul v-else class="field-list">
          <li
            v-for="f in schemaFields"
            :key="f.name"
            class="field-item"
            @click="pickField(f)"
          >
            <span class="field-name">{{ f.name }}</span>
            <span class="field-label">{{ f.label }}</span>
            <el-tag size="small" type="info">{{ f.type }}</el-tag>
          </li>
        </ul>
        <div class="side-tip">点击字段可填入「计算字段」或「查询条件」</div>
      </div>
    </div>

    <div v-if="previewRows.length > 0 || previewError" class="preview-panel">
      <div class="preview-title">预览结果（最多 20 行）</div>
      <el-alert v-if="previewError" type="error" :title="previewError" show-icon :closable="false" />
      <el-table v-else :data="previewRows" stripe size="small" max-height="220">
        <el-table-column
          v-for="col in previewColumns"
          :key="col"
          :prop="col"
          :label="col"
          min-width="100"
          show-overflow-tooltip
        />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import {
  METRIC_CALC_METHODS,
  METRIC_FILTER_OPERATORS,
  METRIC_TABLE_SCHEMAS,
  buildMetricSqlFromConfig,
  type MetricBuilderConfig,
  type MetricFieldMeta,
} from '@/constants/metricSchema'
import { previewMetric, METRIC_DATA_SOURCES } from '@/api/metric'

const props = defineProps<{
  dataSource: string
  metricFormula: string
}>()

const emit = defineEmits<{
  (e: 'update:dataSource', v: string): void
  (e: 'update:metricFormula', v: string): void
}>()

const dataSources = METRIC_DATA_SOURCES
const previewing = ref(false)
const previewRows = ref<Record<string, unknown>[]>([])
const previewColumns = ref<string[]>([])
const previewError = ref('')

const config = reactive<MetricBuilderConfig>({
  dataSource: props.dataSource || '',
  calcMethod: 'COUNT',
  calcField: '',
  groupByFields: [],
  joinTables: [],
  conditions: [],
})

const formula = computed({
  get: () => props.metricFormula,
  set: (v: string) => emit('update:metricFormula', v),
})

const schema = computed(() => METRIC_TABLE_SCHEMAS[config.dataSource])
const schemaFields = computed(() => schema.value?.fields ?? [])
const availableJoins = computed(() => schema.value?.joins ?? [])
const calcNeedsField = computed(() =>
  METRIC_CALC_METHODS.find((m) => m.value === config.calcMethod)?.needsField ?? false,
)
const numericFields = computed(() =>
  schemaFields.value.filter((f) => f.type === 'number'),
)

watch(
  () => props.dataSource,
  (v) => {
    if (v && v !== config.dataSource) {
      config.dataSource = v
    }
  },
)

function resetBuilderState(dataSource: string) {
  config.dataSource = dataSource
  config.calcMethod = 'COUNT'
  config.calcField = ''
  config.groupByFields = []
  config.joinTables = []
  config.conditions = []
  previewRows.value = []
  previewColumns.value = []
  previewError.value = ''
}

function onDataSourceChange(v: string) {
  resetBuilderState(v)
  emit('update:dataSource', v)
  emitFormula()
}

function addCondition() {
  config.conditions.push({ field: '', operator: '=', value: '' })
}

function removeCondition(idx: number) {
  config.conditions.splice(idx, 1)
  emitFormula()
}

function pickField(f: MetricFieldMeta) {
  if (f.type === 'number') {
    config.calcField = f.name
    if (config.calcMethod === 'COUNT') {
      config.calcMethod = 'SUM'
    }
  } else {
    const empty = config.conditions.find((c) => !c.field)
    if (empty) {
      empty.field = f.name
    } else {
      config.conditions.push({ field: f.name, operator: '=', value: '' })
    }
  }
  emitFormula()
}

function generateFormula() {
  if (!config.dataSource) {
    ElMessage.warning('请先选择数据源')
    return
  }
  if (calcNeedsField.value && !config.calcField) {
    ElMessage.warning('请选择计算字段')
    return
  }
  const sql = buildMetricSqlFromConfig(config)
  emit('update:metricFormula', sql)
  ElMessage.success('公式已生成')
}

function emitFormula() {
  if (!config.dataSource) return
  const sql = buildMetricSqlFromConfig(config)
  if (sql) emit('update:metricFormula', sql)
}

function onFormulaInput(v: string) {
  emit('update:metricFormula', v)
}

async function handlePreview() {
  const sql = props.metricFormula?.trim()
  if (!sql) {
    ElMessage.warning('请先生成或填写计算公式')
    return
  }
  previewing.value = true
  previewError.value = ''
  previewRows.value = []
  previewColumns.value = []
  try {
    const res: any = await previewMetric({ metricFormula: sql })
    const data = res?.data ?? res
    const rows = data?.rows ?? []
    previewRows.value = Array.isArray(rows) ? rows : [rows]
    previewColumns.value = previewRows.value.length > 0 ? Object.keys(previewRows.value[0]) : []
    if (previewRows.value.length === 0) {
      ElMessage.info('查询成功，暂无数据')
    }
  } catch (e: any) {
    previewError.value = e?.message || '预览失败'
  } finally {
    previewing.value = false
  }
}
</script>

<style scoped>
.metric-builder { width: 100%; }
.builder-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.builder-main { flex: 1; min-width: 0; }
.builder-side {
  width: 220px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  background: var(--el-fill-color-blank);
  max-height: 420px;
  overflow-y: auto;
}
.side-title { font-weight: 600; font-size: 13px; margin-bottom: 8px; }
.side-empty { color: #909399; font-size: 12px; }
.field-list { list-style: none; margin: 0; padding: 0; }
.field-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 6px 4px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  cursor: pointer;
  font-size: 12px;
}
.field-item:hover { background: var(--el-fill-color-light); }
.field-name { font-family: monospace; color: var(--el-color-primary); }
.field-label { color: #606266; flex: 1; }
.side-tip { margin-top: 8px; font-size: 11px; color: #909399; }
.conditions-wrap { width: 100%; }
.condition-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.builder-actions {
  display: flex;
  gap: 8px;
  margin: 0 0 12px 96px;
}
.formula-block { margin-top: 4px; }
.form-tip { margin-top: 4px; color: #909399; font-size: 12px; }
.preview-panel {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color);
}
.preview-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
</style>
