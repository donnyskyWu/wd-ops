<template>
  <table class="screen-list-table">
    <thead>
      <tr>
        <th
          v-for="col in columns"
          :key="col.key"
          :class="thClass(col.key)"
        >{{ col.label }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(row, idx) in rows" :key="idx">
        <td v-for="col in columns" :key="col.key" :class="tdClass(col.key)">
          <span
            v-if="col.key === 'rank'"
            class="rank-badge"
            :class="rankClass(Number(row.rank ?? idx + 1))"
          >{{ row.rank ?? idx + 1 }}</span>
          <span v-else-if="col.key === 'platform_type'" :class="['platform-tag', platformClass(row[col.key])]">
            {{ row[col.key] }}
          </span>
          <span v-else-if="col.key === 'trend_pct'" :class="trendClass(row[col.key])">
            {{ formatTrend(row[col.key]) }}
          </span>
          <span v-else-if="isHighlightNum(col.key)" class="num-highlight">
            {{ formatCell(row[col.key]) }}
          </span>
          <span v-else>{{ formatCell(row[col.key]) }}</span>
        </td>
      </tr>
      <tr v-if="!rows.length">
        <td :colspan="columns.length || 1" class="empty-cell">暂无数据</td>
      </tr>
    </tbody>
  </table>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatNumber, type DashboardWidgetResult, type ListColumnDef } from '@/types/dataScreen'

const props = defineProps<{
  widget: DashboardWidgetResult
}>()

const columns = computed(() => props.widget.payload.columns || [])
const rows = computed(() => props.widget.payload.rows || [])

const HIGHLIGHT_NUM_KEYS = new Set(['read_count', 'play_count', 'follower_count', 'like_count'])

const isHighlightNum = (key: string) => HIGHLIGHT_NUM_KEYS.has(key)

const thClass = (key: string) => {
  if (key === 'rank') return 'col-rank'
  if (key === 'trend_pct') return 'col-trend'
  if (isHighlightNum(key)) return 'col-num'
  if (key === 'platform_type') return 'col-platform'
  return 'col-text'
}

const tdClass = (key: string) => thClass(key)

const rankClass = (rank: number) => {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return 'rank-other'
}

const formatCell = (val: unknown) => {
  if (typeof val === 'number') return formatNumber(val)
  return val ?? '-'
}

const formatTrend = (val: unknown) => {
  if (val == null || val === '') return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  const sign = n > 0 ? '+' : ''
  return `${sign}${n.toFixed(1)}%`
}

const trendClass = (val: unknown) => {
  const n = Number(val)
  if (Number.isNaN(n) || n === 0) return 'trend-flat'
  return n > 0 ? 'trend-up' : 'trend-down'
}

const platformClass = (val: unknown) => {
  const text = String(val ?? '')
  if (text.includes('抖音') || text.toUpperCase().includes('DOUYIN')) return 'platform-douyin'
  return 'platform-wechat'
}
</script>

<style scoped>
.screen-list-table {
  width: 100%;
  border-collapse: collapse;
}
.screen-list-table th {
  text-align: left;
  padding: 8px 12px;
  font-size: 12px;
  color: #78909c;
  border-bottom: 1px solid #1e4976;
}
.screen-list-table td {
  padding: 8px 12px;
  font-size: 13px;
  border-bottom: 1px solid #0d2137;
}
.screen-list-table tr:hover {
  background: rgba(79, 195, 247, 0.05);
}
.col-rank {
  width: 40px;
  text-align: center;
}
.col-trend {
  width: 72px;
  text-align: right;
}
.col-num {
  width: 88px;
  text-align: right;
}
.col-platform {
  width: 72px;
}
.col-text {
  /* 作品标题等自适应列 */
}
th.col-num,
th.col-trend {
  text-align: right;
}
.num-highlight {
  font-weight: 600;
  color: #4fc3f7;
}
.trend-up { color: #66bb6a; }
.trend-down { color: #ef5350; }
.trend-flat { color: #90a4ae; }
.rank-badge {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  text-align: center;
  line-height: 20px;
  font-size: 11px;
  font-weight: 700;
}
.rank-1 { background: #ff6b6b; color: #fff; }
.rank-2 { background: #ffa726; color: #fff; }
.rank-3 { background: #ffee58; color: #333; }
.rank-other { background: #37474f; color: #90a4ae; }
.platform-tag { font-size: 12px; }
.platform-wechat { color: #07c160; }
.platform-douyin { color: #fa9d3b; }
.empty-cell {
  text-align: center;
  color: #78909c;
  padding: 24px;
}
</style>
