<template>
  <div class="config-manage-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="字典管理" name="dict">
        <el-table :data="dictList" stripe><el-table-column prop="dictType" label="字典类型" width="150" /><el-table-column prop="dictName" label="字典名称" min-width="150" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '启用' ? 'success' : 'info'">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="参数配置" name="param">
        <el-table :data="paramList" stripe><el-table-column prop="paramName" label="参数名称" min-width="150" /><el-table-column prop="paramKey" label="参数键" width="150" /><el-table-column prop="paramValue" label="参数值" min-width="150" /><el-table-column label="操作" width="100" fixed="right"><template #default><el-button link type="primary">编辑</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="公告管理" name="notice">
        <el-table :data="noticeList" stripe><el-table-column prop="title" label="公告标题" min-width="200" /><el-table-column prop="type" label="类型" width="100" /><el-table-column prop="publishTime" label="发布时间" width="150" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '已发布' ? 'success' : 'info'">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="菜单管理" name="menu">
        <el-table :data="menuList" stripe><el-table-column prop="menuName" label="菜单名称" min-width="150" /><el-table-column prop="icon" label="图标" width="80" /><el-table-column prop="sort" label="排序" width="80" /><el-table-column prop="path" label="路由" min-width="150" /><el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="部门管理" name="dept">
        <el-table :data="deptList" stripe><el-table-column prop="deptName" label="部门名称" min-width="150" /><el-table-column prop="leader" label="负责人" width="120" /><el-table-column prop="phone" label="联系电话" width="150" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="岗位管理" name="post">
        <el-table :data="postList" stripe><el-table-column prop="postCode" label="岗位编码" width="120" /><el-table-column prop="postName" label="岗位名称" min-width="150" /><el-table-column prop="sort" label="排序" width="80" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="日志管理" name="log">
        <el-table :data="logList" stripe><el-table-column prop="module" label="模块" width="120" /><el-table-column prop="action" label="操作" width="150" /><el-table-column prop="operator" label="操作人" width="120" /><el-table-column prop="time" label="时间" width="180" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '成功' ? 'success' : 'danger'">{{ row.status }}</el-tag></template></el-table-column></el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const activeTab = ref('dict')
const dictList = ref([{ dictType: 'platform', dictName: '平台类型', status: '启用' }])
const paramList = ref([{ paramName: '系统名称', paramKey: 'sys.name', paramValue: '运营数据平台' }])
const noticeList = ref([{ title: '系统维护通知', type: '通知', publishTime: '2026-05-27', status: '已发布' }])
const menuList = ref([{ menuName: '首页', icon: '🏠', sort: 1, path: '/dashboard' }])
const deptList = ref([{ deptName: '运营部', leader: '张三', phone: '13800138000', status: '正常' }])
const postList = ref([{ postCode: 'OPS001', postName: '运营专员', sort: 1, status: '正常' }])
const logList = ref([{ module: '用户管理', action: '新增用户', operator: 'admin', time: '2026-05-27 10:30:00', status: '成功' }])
</script>

<style scoped>
.config-manage-page { padding: 20px; }
</style>
