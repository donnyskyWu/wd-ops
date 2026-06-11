<template>
  <div class="query-builder">
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

          <el-form-item label="展示字段">
            <el-select
              v-model="config.displayFields"
              placeholder="选择结果中展示的字段"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              style="width: 100%"
              @change="emitSql"
            >
              <el-option
                v-for="f in schemaFields"
                :key="f.name"
                :label="`${f.label} (${f.name})`"
                :value="f.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="计算方式">
            <el-select
              v-model="config.calcMethod"
              placeholder="可选，不选则返回明细"
              clearable
              style="width: 100%"
              @change="onCalcMethodChange"
            >
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
              @change="emitSql"
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
              @change="emitSql"
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
              @change="emitSql"
            >
              <el-option
                v-for="j in availableJoins"
                :key="j.table"
                :label="`${j.label} (${j.table})`"
                :value="j.table"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            class="conditions-item"
            :class="{ 'is-collapsible': collapsibleConditions }"
          >
            <template #label>
              <div
                class="conditions-label"
                :class="{ 'collapsible-title': collapsibleConditions }"
                @click.stop.prevent="toggleConditions"
              >
                <el-icon
                  v-if="collapsibleConditions"
                  class="collapse-icon"
                  :class="{ 'is-collapsed': !conditionsExpanded }"
                >
                  <ArrowDown />
                </el-icon>
                <span>查询条件</span>
              </div>
            </template>
            <div v-show="!collapsibleConditions || conditionsExpanded" class="conditions-wrap">
              <div v-for="(cond, idx) in config.conditions" :key="idx" class="condition-row">
                <el-select v-model="cond.field" placeholder="字段" filterable style="width: 140px" @change="emitSql">
                  <el-option
                    v-for="f in schemaFields"
                    :key="f.name"
                    :label="f.label"
                    :value="f.name"
                  />
                </el-select>
                <el-select v-model="cond.operator" placeholder="运算符" style="width: 110px" @change="emitSql">
                  <el-option v-for="op in METRIC_FILTER_OPERATORS" :key="op.value" :label="op.label" :value="op.value" />
                </el-select>
                <el-input
                  v-if="cond.operator !== 'IS NULL' && cond.operator !== 'IS NOT NULL'"
                  v-model="cond.value"
                  placeholder="值"
                  style="flex: 1"
                  @input="emitSql"
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
          <el-button type="primary" @click="generateSql">生成 SQL</el-button>
        </div>

        <el-form-item label="SQL 预览" label-width="96px" class="formula-block">
          <el-input
            :model-value="props.sqlText"
            type="textarea"
            :rows="4"
            spellcheck="false"
            placeholder="配置字段后自动生成，也可手动编辑"
            @update:model-value="onSqlInput"
          />
          <div class="form-tip">仅允许 SELECT；含 tenant_id 占位符 :tenantId</div>
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
        <div class="side-tip">点击字段可加入「展示字段」或「查询条件」</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Delete } from '@element-plus/icons-vue'
import {
  METRIC_CALC_METHODS,
  METRIC_FILTER_OPERATORS,
  METRIC_TABLE_SCHEMAS,
  buildQuerySqlFromConfig,
  createEmptyQueryConfig,
  type MetricFieldMeta,
  type QueryBuilderConfig,
} from '@/constants/metricSchema'
import { METRIC_DATA_SOURCES } from '@/api/metric'

const props = withDefaults(defineProps<{
  modelValue: QueryBuilderConfig
  sqlText: string
  collapsibleConditions?: boolean
}>(), {
  collapsibleConditions: false,
})

const conditionsExpanded = ref(true)

function toggleConditions() {
  if (!props.collapsibleConditions) return
  conditionsExpanded.value = !conditionsExpanded.value
}

const emit = defineEmits<{
  (e: 'update:modelValue', v: QueryBuilderConfig): void
  (e: 'update:sqlText', v: string): void
}>()

const dataSources = METRIC_DATA_SOURCES

const config = reactive<QueryBuilderConfig>({ ...createEmptyQueryConfig(), ...props.modelValue })

const schema = computed(() => METRIC_TABLE_SCHEMAS[config.dataSource])
const schemaFields = computed(() => schema.value?.fields ?? [])
const availableJoins = computed(() => schema.value?.joins ?? [])
const calcNeedsField = computed(() =>
  config.calcMethod
    ? (METRIC_CALC_METHODS.find((m) => m.value === config.calcMethod)?.needsField ?? false)
    : false,
)
const numericFields = computed(() => schemaFields.value.filter((f) => f.type === 'number'))

watch(
  () => props.modelValue,
  (v) => Object.assign(config, v),
  { deep: true },
)

function syncConfig() {
  emit('update:modelValue', { ...config })
}

function resetBuilderState(dataSource: string) {
  Object.assign(config, createEmptyQueryConfig(dataSource))
  syncConfig()
}

function onDataSourceChange(v: string) {
  resetBuilderState(v)
  emitSql()
}

function onCalcMethodChange() {
  if (!config.calcMethod) config.calcField = ''
  emitSql()
}

function addCondition() {
  config.conditions.push({ field: '', operator: '=', value: '' })
  syncConfig()
}

function removeCondition(idx: number) {
  config.conditions.splice(idx, 1)
  emitSql()
}

function pickField(f: MetricFieldMeta) {
  if (!config.displayFields.includes(f.name)) {
    config.displayFields.push(f.name)
  }
  if (f.type === 'number' && config.calcMethod) {
    config.calcField = f.name
  } else if (f.type !== 'number') {
    const empty = config.conditions.find((c) => !c.field)
    if (empty) {
      empty.field = f.name
    } else {
      config.conditions.push({ field: f.name, operator: '=', value: '' })
    }
  }
  emitSql()
}

function validateConfig(): boolean {
  if (!config.dataSource) {
    ElMessage.warning('请先选择数据源')
    return false
  }
  if (config.displayFields.length === 0 && !config.calcMethod) {
    ElMessage.warning('请至少选择展示字段或计算方式')
    return false
  }
  if (calcNeedsField.value && !config.calcField) {
    ElMessage.warning('请选择计算字段')
    return false
  }
  return true
}

function generateSql() {
  if (!validateConfig()) return
  const sql = buildQuerySqlFromConfig(config)
  emit('update:sqlText', sql)
  syncConfig()
  ElMessage.success('SQL 已生成')
}

function emitSql() {
  syncConfig()
  if (!config.dataSource) return
  if (config.displayFields.length === 0 && !config.calcMethod) return
  if (calcNeedsField.value && !config.calcField) return
  const sql = buildQuerySqlFromConfig(config)
  if (sql) emit('update:sqlText', sql)
}

function onSqlInput(v: string) {
  emit('update:sqlText', v)
}
</script>

<style scoped>
.query-builder { width: 100%; }
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
  max-height: 480px;
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
.conditions-label { display: inline-flex; align-items: center; max-width: 100%; }
.conditions-item.is-collapsible :deep(.el-form-item__label) {
  overflow: visible;
  pointer-events: auto;
}
.collapsible-title { cursor: pointer; user-select: none; }
.collapse-icon {
  margin-right: 6px;
  transition: transform 0.2s ease;
}
.collapse-icon.is-collapsed { transform: rotate(-90deg); }
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
</style>
