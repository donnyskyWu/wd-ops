<template>
  <div class="system-manage-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户管理" name="user">
        <div class="action-bar"><el-button type="primary" @click="handleAdd">新增用户</el-button></div>
        <el-table :data="userList" stripe><el-table-column prop="username" label="用户名" width="120" /><el-table-column prop="nickname" label="昵称" width="120" /><el-table-column prop="dept" label="部门" width="120" /><el-table-column prop="role" label="角色" width="120" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="操作" width="200" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="warning">重置密码</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="角色管理" name="role">
        <div class="action-bar"><el-button type="primary" @click="handleAdd">新增角色</el-button></div>
        <el-table :data="roleList" stripe><el-table-column prop="roleName" label="角色名称" min-width="150" /><el-table-column prop="roleKey" label="角色标识" width="150" /><el-table-column prop="sort" label="排序" width="80" /><el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="操作" width="200" fixed="right"><template #default><el-button link type="primary">编辑</el-button><el-button link type="primary">授权</el-button><el-button link type="danger">删除</el-button></template></el-table-column></el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('user')
const userList = ref([
  { username: 'admin', nickname: '管理员', dept: '技术部', role: '超级管理员', status: '正常' },
  { username: 'ops01', nickname: '运营专员', dept: '运营部', role: '运营人员', status: '正常' },
])
const roleList = ref([
  { roleName: '超级管理员', roleKey: 'admin', sort: 1, status: '正常' },
  { roleName: '运营人员', roleKey: 'ops', sort: 2, status: '正常' },
])

const handleAdd = () => ElMessage.success('新增功能')
</script>

<style scoped>
.system-manage-page { padding: 20px; }
.action-bar { margin-bottom: 16px; }
</style>
