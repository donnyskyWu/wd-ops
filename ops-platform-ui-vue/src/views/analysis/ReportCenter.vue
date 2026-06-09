<!--
  M6 数据报表中心入口
  路径: /data-report
  8 张报表卡 + 跳转
-->
<template>
  <div class="report-center">
    <h2 style="margin-top: 0">数据报表中心</h2>
    <p style="color: #909399; margin-bottom: 24px">点击任一卡片查看对应报表详细数据</p>
    <el-row :gutter="16">
      <el-col :span="6" v-for="r in reports" :key="r.path">
        <el-card shadow="hover" class="report-card" @click="goReport(r.path)">
          <div class="report-icon" :style="{ background: r.color }">
            <el-icon :size="24"><component :is="r.icon" /></el-icon>
          </div>
          <h3 class="report-title">{{ r.title }}</h3>
          <p class="report-desc">{{ r.desc }}</p>
          <el-tag size="small" :type="r.tagType">{{ r.tag }}</el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  DataLine,
  Warning,
  VideoPlay,
  Headset,
  Money,
  TrendCharts,
  User,
  Bell,
} from '@element-plus/icons-vue'

const router = useRouter()

const reports = [
  { path: '/analysis/report/unified-account', title: '统一视图', desc: '账号/粉丝/GMV 全局概览', tag: '概览', tagType: 'primary', color: '#ecf5ff', icon: DataLine },
  { path: '/analysis/report/account-status', title: '状态监控', desc: '账号健康状态实时监控', tag: '运营', tagType: 'success', color: '#f0f9eb', icon: Warning },
  { path: '/analysis/report/video-output', title: '短视频产出', desc: '各平台短视频产出趋势', tag: '内容', tagType: 'warning', color: '#fdf6ec', icon: VideoPlay },
  { path: '/analysis/report/live-duration', title: '直播时长', desc: '直播场次/时长/在线', tag: '直播', tagType: 'danger', color: '#fef0f0', icon: Headset },
  { path: '/analysis/report/cost-allocation', title: '成本分摊', desc: '投流/人力/设备成本占比', tag: '财务', tagType: 'info', color: '#f4f4f5', icon: Money },
  { path: '/analysis/report/roi', title: 'ROI 分析', desc: '整体 ROI 与平台 ROI 拆解', tag: '财务', tagType: 'success', color: '#f0f9eb', icon: TrendCharts },
  { path: '/analysis/report/team-config', title: '团队配置', desc: '人员分布/账号人均/人效', tag: '人力', tagType: 'primary', color: '#ecf5ff', icon: User },
  { path: '/analysis/report/account-alert', title: '异常预警', desc: '紧急/重要/提示 三级预警', tag: '风控', tagType: 'danger', color: '#fef0f0', icon: Bell },
] as any[]

const goReport = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.report-center { padding: 20px; }
.report-card { cursor: pointer; transition: all 0.2s; margin-bottom: 16px; }
.report-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }
.report-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; }
.report-title { margin: 0 0 4px 0; font-size: 16px; font-weight: 600; }
.report-desc { color: #909399; font-size: 13px; margin: 0 0 12px 0; min-height: 38px; }
</style>
