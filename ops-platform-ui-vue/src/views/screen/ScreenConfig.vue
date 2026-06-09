<template>
  <div class="screen-config-page">
    <el-card shadow="never">
      <template #header><div class="card-header"><span>大屏布局配置</span><el-button type="primary" @click="handleSave">保存配置</el-button></div></template>
      <el-form :model="configForm" label-width="120px">
        <el-form-item label="大屏名称"><el-input v-model="configForm.name" style="width: 300px" /></el-form-item>
        <el-form-item label="分辨率"><el-select v-model="configForm.resolution" style="width: 200px"><el-option label="1920x1080" value="1920x1080" /><el-option label="2560x1440" value="2560x1440" /></el-select></el-form-item>
        <el-form-item label="主题色"><el-color-picker v-model="configForm.themeColor" /></el-form-item>
        <el-divider content-position="left">KPI卡片配置</el-divider>
        <el-form-item label="显示项">
          <el-checkbox-group v-model="configForm.kpiItems">
            <el-checkbox label="totalFollowers">总粉丝</el-checkbox>
            <el-checkbox label="totalRevenue">总营收</el-checkbox>
            <el-checkbox label="totalCost">总成本</el-checkbox>
            <el-checkbox label="overallRoi">综合ROI</el-checkbox>
            <el-checkbox label="activeAccount">活跃号</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-divider content-position="left">图表配置</el-divider>
        <el-form-item label="左侧图表"><el-checkbox-group v-model="configForm.leftCharts"><el-checkbox label="contentTrend">内容发布趋势</el-checkbox><el-checkbox label="followerTrend">粉丝增长趋势</el-checkbox></el-checkbox-group></el-form-item>
        <el-form-item label="中间图表"><el-checkbox-group v-model="configForm.centerCharts"><el-checkbox label="platformDist">平台分布</el-checkbox><el-checkbox label="roiTrend">ROI趋势</el-checkbox></el-checkbox-group></el-form-item>
        <el-form-item label="右侧面板"><el-checkbox-group v-model="configForm.rightPanels"><el-checkbox label="alerts">预警信息</el-checkbox><el-checkbox label="rankings">排行榜</el-checkbox></el-checkbox-group></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const configForm = reactive({
  name: '运营数据大屏',
  resolution: '1920x1080',
  themeColor: '#00d4ff',
  kpiItems: ['totalFollowers', 'totalRevenue', 'totalCost', 'overallRoi', 'activeAccount'],
  leftCharts: ['contentTrend', 'followerTrend'],
  centerCharts: ['platformDist', 'roiTrend'],
  rightPanels: ['alerts', 'rankings'],
})

const handleSave = () => ElMessage.success('配置已保存')
</script>

<style scoped>
.screen-config-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
